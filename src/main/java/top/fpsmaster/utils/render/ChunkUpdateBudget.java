package top.fpsmaster.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * How many chunk rebuilds the builder thread is allowed to start, decided by whether the camera is
 * moving.
 *
 * <p>The existing throttle is one number for every situation, and the two situations want opposite
 * things. Moving, a rebuild competes with the frame that is trying to draw the chunks already built,
 * and the backlog is refilled as fast as it is drained — that is when a limit earns its keep. Standing
 * still, the backlog is finite and every rebuild still waiting is a hole in the world; there is
 * nothing to protect the frame from, so the limit is only delaying the world settling down.
 *
 * <p>So: the configured budget while moving, twice it while still. Deliberately two steps and not a
 * controller — the shape of the problem is a threshold, and a first version that guesses at a curve
 * would be measuring its own guess.
 *
 * <p>The decision is made on the client thread, which is the only one that can see the camera, and
 * read by the builder thread. The builder waits on this class's monitor rather than sleeping a fixed
 * interval, so a budget that goes up when the player stops takes effect at once instead of after
 * however much of the sleep was left.
 */
public final class ChunkUpdateBudget {

    /** Squared, so the check needs no square root. About a tenth of a block per tick. */
    private static final double MOVING_THRESHOLD_SQUARED = 0.01d;

    /** Degrees of yaw or pitch in one tick that count as turning rather than looking around. */
    private static final float TURNING_THRESHOLD = 2.0f;

    /** How much more the builder may do once nothing is moving. */
    private static final int STILL_MULTIPLIER = 2;

    /** Ceiling on the wait, so a missed notification cannot park the builder for good. */
    private static final long WAIT_MILLIS = 50L;

    private static final Object MONITOR = new Object();

    private static volatile int allowance = Integer.MAX_VALUE;

    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static float lastYaw;
    private static float lastPitch;
    private static boolean tracking;

    private ChunkUpdateBudget() {
    }

    /** Called once per client tick, from the thread that owns the camera. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = mc.getRenderViewEntity();
        if (camera == null || !Performance.using || !Performance.limitChunks.getValue()) {
            tracking = false;
            set(Integer.MAX_VALUE);
            return;
        }

        int base = Performance.chunkUpdateLimit.getValue().intValue();
        if (!Performance.adaptiveChunkBudget.getValue()) {
            tracking = false;
            set(base);
            return;
        }
        if (!tracking) {
            // First tick after joining or after the setting came on: no previous sample to compare
            // against, and guessing "still" would let the builder run flat out during the load.
            remember(camera);
            tracking = true;
            set(base);
            return;
        }

        double dx = camera.posX - lastX;
        double dy = camera.posY - lastY;
        double dz = camera.posZ - lastZ;
        boolean moving = dx * dx + dy * dy + dz * dz > MOVING_THRESHOLD_SQUARED
                || Math.abs(camera.rotationYaw - lastYaw) > TURNING_THRESHOLD
                || Math.abs(camera.rotationPitch - lastPitch) > TURNING_THRESHOLD;
        remember(camera);

        if (BenchmarkMode.ACTIVE) {
            if (moving) {
                BenchCounters.chunkBudgetMovingTicks++;
            } else {
                BenchCounters.chunkBudgetStillTicks++;
            }
        }
        set(moving ? base : base * STILL_MULTIPLIER);
    }

    /** Read from the chunk builder thread. */
    public static int allowance() {
        return allowance;
    }

    /**
     * Parks the builder until the budget might have changed.
     *
     * <p>Capped rather than indefinite: the counter this budget is compared against is reset by the
     * client on its own schedule, and waiting on a notification that only fires when the camera's
     * state changes would sleep through that reset.
     */
    public static void await() throws InterruptedException {
        synchronized (MONITOR) {
            MONITOR.wait(WAIT_MILLIS);
        }
    }

    private static void remember(Entity camera) {
        lastX = camera.posX;
        lastY = camera.posY;
        lastZ = camera.posZ;
        lastYaw = camera.rotationYaw;
        lastPitch = camera.rotationPitch;
    }

    private static void set(int value) {
        if (value == allowance) {
            return;
        }
        allowance = value;
        synchronized (MONITOR) {
            MONITOR.notifyAll();
        }
    }
}
