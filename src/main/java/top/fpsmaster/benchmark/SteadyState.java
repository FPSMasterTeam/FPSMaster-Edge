package top.fpsmaster.benchmark;

import java.util.Arrays;

/**
 * Decides when a run has stopped getting faster, so measurement starts on the flat part.
 *
 * <p>The discard phase used to be a fixed number of milliseconds, and it was too short. Median
 * frame time by tenth of a measured window showed the openings running at three to seven
 * milliseconds against a two-millisecond steady state, and the runs that had the longest openings
 * were exactly the runs that measured slowest — a nine-run series covered 285 to 417 fps for one
 * configuration, most of it decided before the workload had settled.
 *
 * <p>A fixed wait cannot fix that, because how long the opening lasts is itself variable: the same
 * scenario settled in a tenth of one run and three tenths of another. So the wait is now on the
 * thing that matters. Two consecutive windows of frames are compared, and the run is called steady
 * when the newer one has stopped being meaningfully faster than the older.
 *
 * <p>Compared on the median rather than the mean, because the openings contain the largest spikes
 * and a mean would keep chasing them long after the typical frame had settled.
 */
public final class SteadyState {

    /** Frames per comparison window. At a few hundred fps this is a fraction of a second. */
    private static final int WINDOW = 240;

    /** Improvement below this counts as no longer improving. */
    private static final double TOLERANCE = 0.03d;

    /** Consecutive flat comparisons before believing it. One can happen by accident. */
    private static final int CONFIRMATIONS = 2;

    private final long[] current = new long[WINDOW];
    private int filled;
    private double previousMedian = Double.MAX_VALUE;
    private int flatWindows;

    /** Whether the run has been flat for {@link #CONFIRMATIONS} consecutive windows. */
    public boolean isSteady() {
        return flatWindows >= CONFIRMATIONS;
    }

    /** Feeds one frame. Returns true once the run has been flat for {@link #CONFIRMATIONS} windows. */
    public boolean record(long frameNanos) {
        if (frameNanos <= 0L) {
            return false;
        }
        current[filled++] = frameNanos;
        if (filled < WINDOW) {
            return false;
        }
        filled = 0;

        long[] sorted = Arrays.copyOf(current, WINDOW);
        Arrays.sort(sorted);
        double median = sorted[WINDOW / 2];

        // Only improvement counts. A window that came out slower than the last is noise or a spike,
        // not evidence that the run is still warming up, and treating it as a reset would let one
        // bad window hold the measurement off indefinitely.
        boolean improving = median < previousMedian * (1.0d - TOLERANCE);
        previousMedian = Math.min(previousMedian, median);
        flatWindows = improving ? 0 : flatWindows + 1;
        return flatWindows >= CONFIRMATIONS;
    }

    /** How many frames the decision has seen, for the log line that reports it. */
    public int windowFrames() {
        return WINDOW;
    }

    public void reset() {
        filled = 0;
        previousMedian = Double.MAX_VALUE;
        flatWindows = 0;
    }
}
