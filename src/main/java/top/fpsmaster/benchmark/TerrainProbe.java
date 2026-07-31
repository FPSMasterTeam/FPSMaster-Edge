package top.fpsmaster.benchmark;

import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Splits {@code setupTerrain} by whether it rebuilt the visible-chunk list that frame.
 *
 * <p>The section is 14.9% of the frame and has never been looked at, but that number covers three
 * different jobs: updating the camera and the render container, walking the world to decide which
 * chunks are visible, and collecting the ones that need rebuilding. Only the middle one is a
 * candidate for reuse, and only if it is where the time goes.
 *
 * <p>Rather than bracket inside the method — the walk, the container class and the dirty assignment
 * all live in one long body, and an inner anchor there is a maintenance liability — this times the
 * whole call and files it under whether the walk ran. Forge already skips the walk when nothing
 * moved, so both populations occur naturally and the difference between their means is what the
 * walk costs. It also answers the question that decides whether any of this is worth doing: how
 * often it runs at all.
 *
 * <p>The lesson that produced this shape is one bracket away — see the HUD text cache, where an
 * emit bracket turned out to be four fifths vertex submission and the caching aimed at the other
 * fifth. A section timer says where a name is, not where the work is.
 *
 * <pre>
 *   -Dedge.exp.terrainProbe=true
 * </pre>
 */
public final class TerrainProbe {

    private static long walkedFrames;
    private static long walkedNanos;
    private static long reusedFrames;
    private static long reusedNanos;
    private static long started;
    private static boolean walkedThisFrame;

    /** Rebuilds attributed to the camera having moved, and to everything else. */
    private static long walkedMoved;
    private static long walkedStill;
    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static float lastYaw;
    private static float lastPitch;

    private TerrainProbe() {
    }

    public static boolean enabled() {
        return Experiments.active(Experiments.TERRAIN_PROBE);
    }

    public static void begin() {
        walkedThisFrame = false;
        started = System.nanoTime();
    }

    /**
     * Called from inside the branch that rebuilds the list, so it marks the frames that did — and
     * says whether the camera had moved, which is the only cause a relaxed threshold could remove.
     *
     * <p>Forge sets the flag on any of: a pending chunk update, an exact inequality on the view
     * entity's position, or an exact inequality on its rotation. Reuse can only ever address the
     * last two: a chunk that has been rebuilt has to be walked again, there is no version of that
     * which is optional. So the split between the two is the whole decision.
     */
    public static void walked(double x, double y, double z, float yaw, float pitch) {
        walkedThisFrame = true;
        if (x != lastX || y != lastY || z != lastZ || yaw != lastYaw || pitch != lastPitch) {
            walkedMoved++;
        } else {
            walkedStill++;
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        lastYaw = yaw;
        lastPitch = pitch;
    }

    public static void end() {
        long elapsed = System.nanoTime() - started;
        if (walkedThisFrame) {
            walkedFrames++;
            walkedNanos += elapsed;
        } else {
            reusedFrames++;
            reusedNanos += elapsed;
        }
        long total = walkedFrames + reusedFrames;
        if (total % 600L != 0L) {
            return;
        }
        double walkedMean = walkedFrames == 0L ? 0.0d : walkedNanos / 1000.0d / walkedFrames;
        double reusedMean = reusedFrames == 0L ? 0.0d : reusedNanos / 1000.0d / reusedFrames;
        ClientLogger.info("terrain", String.format(
                "setupTerrain over %d frames: rebuilt the visible list on %.1f%% of them,"
                        + " %.1fus when it did, %.1fus when it did not, difference %.1fus"
                        + " (%.1fus/frame amortised); of the rebuilds %.1f%% followed camera"
                        + " movement and %.1f%% did not",
                total, 100.0d * walkedFrames / total, walkedMean, reusedMean,
                walkedMean - reusedMean,
                (walkedMean - reusedMean) * walkedFrames / total,
                walkedFrames == 0L ? 0.0d : 100.0d * walkedMoved / walkedFrames,
                walkedFrames == 0L ? 0.0d : 100.0d * walkedStill / walkedFrames));
        walkedFrames = 0L;
        walkedNanos = 0L;
        reusedFrames = 0L;
        reusedNanos = 0L;
        walkedMoved = 0L;
        walkedStill = 0L;
    }
}
