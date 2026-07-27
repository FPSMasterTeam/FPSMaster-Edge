package top.fpsmaster.benchmark;

import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Times what the sky pass spends its time on, by call type.
 *
 * <p>Removing a part and watching the frame is how the rest of this was measured, and for the sky it
 * failed: every part removed on its own accounted for at most 6% of the pass, while removing the
 * whole method accounted for all of it. Cost that moves elsewhere when you delete its cause is
 * invisible to subtraction. Timing each call in place cannot move, so it says where the time
 * actually goes even when removing it would not have helped.
 *
 * <p>The interesting number is the remainder: the section total minus everything counted here. If it
 * dominates, the cost is not in any call the method makes.
 */
public final class SkyBreakdown {

    public static final int STATE = 0;
    public static final int MATRIX = 1;
    public static final int COLOR = 2;
    public static final int LIST = 3;
    public static final int DRAW = 4;
    public static final int BIND = 5;
    public static final int QUERY = 6;
    private static final int COUNT = 7;

    private static final String[] NAMES = {
            "state toggles", "matrix ops", "color", "call list", "tessellator draw",
            "bind texture", "world queries",
    };

    private static final long[] NANOS = new long[COUNT];
    private static final int[] CALLS = new int[COUNT];
    private static long frames;

    private SkyBreakdown() {
    }

    public static boolean enabled() {
        return Experiments.active(Experiments.SKY_BREAKDOWN);
    }

    public static void record(int bucket, long nanos) {
        NANOS[bucket] += nanos;
        CALLS[bucket]++;
    }

    /** Called once per sky pass; reports periodically so a run produces a few readings. */
    public static void endPass() {
        if (++frames % 2000L != 0L) {
            return;
        }
        StringBuilder line = new StringBuilder("sky breakdown over " + frames + " passes:");
        long total = 0L;
        for (int i = 0; i < COUNT; i++) {
            total += NANOS[i];
            line.append(String.format(" | %s %.1fus/frame in %.1f calls",
                    NAMES[i], NANOS[i] / 1000.0d / frames, CALLS[i] / (double) frames));
        }
        line.append(String.format(" || counted %.1fus/frame", total / 1000.0d / frames));
        ClientLogger.info("skybreak", line.toString());
    }
}
