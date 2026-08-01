package top.fpsmaster.modules.perf;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Frame, memory and pause statistics for the performance overlay.
 *
 * <h3>Why these numbers</h3>
 *
 * <p>A frame rate on its own describes the good frames. What a player notices is the bad ones, and
 * the two move independently — this client has a setting measured at better median frame time and
 * a third worse p99 at the same time. So the overlay leads with the frame rate because that is what
 * people look for, and puts the distribution next to it because that is where the experience is.
 *
 * <ul>
 *   <li><b>p50</b> — the typical frame. The most stable number here by a distance, and the one to
 *       judge a change on when the machine is noisy.</li>
 *   <li><b>1% low</b> — the mean of the worst one per cent, in frames per second. Standard, and it
 *       answers "how bad does it get" without being decided by a single outlier the way max is.</li>
 *   <li><b>hitches</b> — frames that took more than twice the median. A count rather than a time,
 *       because one 40ms frame is felt and forty 4ms frames are not, and an average hides that.</li>
 *   <li><b>alloc</b> — allocation rate. Nothing else predicts garbage collection, and collection is
 *       most of what unexplained stutter turns out to be. The terrain visibility walk allocates
 *       nearly two million objects a second on its own; that is the sort of thing this shows.</li>
 *   <li><b>GC</b> — collections and the milliseconds they took, per second. Time actually spent
 *       stopped, rather than inferred.</li>
 * </ul>
 *
 * <h3>What it costs</h3>
 *
 * <p>One {@code nanoTime} and one array write per frame. Everything else is recomputed on a timer,
 * a few times a second, from a fixed ring — sorting four thousand longs twice a second is nothing,
 * and doing it per frame would put the monitor into the measurement it is taking.
 */
public final class PerformanceMonitor {

    /** Frames kept for the distribution. At 500fps this is about eight seconds of history. */
    private static final int WINDOW = 4096;

    /** How often the aggregates are recomputed. Fast enough to feel live, rare enough to be free. */
    private static final long REFRESH_MILLIS = 250L;

    /** A frame this many times the median is a hitch rather than a slow frame. */
    private static final double HITCH_FACTOR = 2.0d;

    /**
     * Columns in the trace, and how much time each one covers.
     *
     * <p>A column holds the <em>worst</em> frame in its slice, not the average. A trace of averages
     * is a smooth line that hides exactly the events it exists to show — one 40ms frame among two
     * hundred good ones disappears into an average and is the only thing in that slice a player
     * felt. Six seconds of history at fifty milliseconds a column.
     */
    private static final int COLUMNS = 120;
    private static final long COLUMN_MILLIS = 50L;

    private static final int BYTES_PER_MB = 1024 * 1024;

    private static final long[] FRAMES = new long[WINDOW];
    private static int written;
    private static int count;
    private static long lastFrameNanos;

    /** Worst frame time per slice, in nanoseconds, oldest to newest once {@link #traceInto} rotates it. */
    private static final long[] TRACE = new long[COLUMNS];
    private static int tracePosition;
    private static long traceColumnStartedMillis;

    private static long lastRefreshMillis;
    private static long lastAllocatedBytes = -1L;
    private static long lastGcCount;
    private static long lastGcMillis;

    private static double fps;
    private static double averageFps;
    private static double medianMs;
    private static double onePercentLowFps;
    private static double worstMs;
    private static int hitches;
    private static long heapUsedBytes;
    private static long heapMaxBytes;
    private static double allocatedMbPerSecond;
    private static double gcPerSecond;
    private static double gcMillisPerSecond;

    /** Reads bytes allocated by this thread. Absent outside HotSpot, in which case allocation is unreported. */
    private static final Method THREAD_ALLOCATED_BYTES = findThreadAllocatedBytes();
    private static final Object THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    private PerformanceMonitor() {
    }

    private static Method findThreadAllocatedBytes() {
        try {
            Class<?> sunBean = Class.forName("com.sun.management.ThreadMXBean");
            if (!sunBean.isInstance(ManagementFactory.getThreadMXBean())) {
                return null;
            }
            return sunBean.getMethod("getCurrentThreadAllocatedBytes");
        } catch (Throwable unavailable) {
            // Reported as zero rather than guessed at from heap deltas, which miss everything
            // allocated and collected between two samples — which at these rates is most of it.
            return null;
        }
    }

    /**
     * Called once per presented frame, from the game loop.
     *
     * <p>Placed at the same point as the benchmark's frame boundary, after {@code Display.update},
     * so one sample is a full present-to-present interval rather than part of one.
     */
    public static void onFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            FRAMES[written % WINDOW] = now - lastFrameNanos;
            written++;
            if (count < WINDOW) {
                count++;
            }
        }
        lastFrameNanos = now;

        long millis = System.currentTimeMillis();
        if (count > 0) {
            // One comparison in the common case. The column only rolls twenty times a second.
            long frame = FRAMES[(written - 1 + WINDOW) % WINDOW];
            if (millis - traceColumnStartedMillis >= COLUMN_MILLIS) {
                traceColumnStartedMillis = millis;
                tracePosition = (tracePosition + 1) % COLUMNS;
                TRACE[tracePosition] = frame;
            } else if (frame > TRACE[tracePosition]) {
                TRACE[tracePosition] = frame;
            }
        }
        if (millis - lastRefreshMillis >= REFRESH_MILLIS) {
            refresh(millis);
        }
    }

    private static void refresh(long nowMillis) {
        long elapsed = lastRefreshMillis == 0L ? REFRESH_MILLIS : nowMillis - lastRefreshMillis;
        lastRefreshMillis = nowMillis;
        refreshFrames();
        refreshMemory(elapsed);
        refreshGc(elapsed);
    }

    private static void refreshFrames() {
        if (count == 0) {
            return;
        }
        long[] sorted = Arrays.copyOf(FRAMES, count);
        long total = 0L;
        for (long frame : sorted) {
            total += frame;
        }
        Arrays.sort(sorted);

        averageFps = total == 0L ? 0.0d : count * 1.0e9d / total;
        medianMs = sorted[count / 2] / 1.0e6d;
        worstMs = sorted[count - 1] / 1.0e6d;
        // The most recent frame, not a smoothed one: a reading that lags is a reading that
        // disagrees with what is on screen when someone is looking for the moment it dropped.
        long latest = FRAMES[(written - 1 + WINDOW) % WINDOW];
        fps = latest <= 0L ? 0.0d : 1.0e9d / latest;

        // Mean of the worst hundredth, expressed as a rate. The mean rather than the single
        // percentile point, so one freak frame cannot define it.
        int worstCount = Math.max(1, count / 100);
        long worstTotal = 0L;
        for (int i = count - worstCount; i < count; i++) {
            worstTotal += sorted[i];
        }
        onePercentLowFps = worstTotal == 0L ? 0.0d : worstCount * 1.0e9d / worstTotal;

        long hitchThreshold = (long) (sorted[count / 2] * HITCH_FACTOR);
        int over = 0;
        for (int i = count - 1; i >= 0 && sorted[i] > hitchThreshold; i--) {
            over++;
        }
        hitches = over;
    }

    private static void refreshMemory(long elapsedMillis) {
        Runtime runtime = Runtime.getRuntime();
        heapUsedBytes = runtime.totalMemory() - runtime.freeMemory();
        heapMaxBytes = runtime.maxMemory();

        if (THREAD_ALLOCATED_BYTES == null) {
            return;
        }
        try {
            long allocated = ((Number) THREAD_ALLOCATED_BYTES.invoke(THREAD_MX_BEAN)).longValue();
            if (lastAllocatedBytes >= 0L && elapsedMillis > 0L) {
                double bytesPerSecond = (allocated - lastAllocatedBytes) * 1000.0d / elapsedMillis;
                allocatedMbPerSecond = bytesPerSecond / BYTES_PER_MB;
            }
            lastAllocatedBytes = allocated;
        } catch (Throwable failure) {
            // One failure means the bean stopped answering; stop asking rather than log per frame.
            lastAllocatedBytes = -1L;
        }
    }

    private static void refreshGc(long elapsedMillis) {
        long collections = 0L;
        long collectionMillis = 0L;
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (int i = 0; i < beans.size(); i++) {
            GarbageCollectorMXBean bean = beans.get(i);
            long beanCount = bean.getCollectionCount();
            long beanMillis = bean.getCollectionTime();
            if (beanCount > 0L) {
                collections += beanCount;
            }
            if (beanMillis > 0L) {
                collectionMillis += beanMillis;
            }
        }
        if (lastGcCount > 0L && elapsedMillis > 0L) {
            gcPerSecond = (collections - lastGcCount) * 1000.0d / elapsedMillis;
            gcMillisPerSecond = (collectionMillis - lastGcMillis) * 1000.0d / elapsedMillis;
        }
        lastGcCount = collections;
        lastGcMillis = collectionMillis;
    }

    public static double fps() {
        return fps;
    }

    public static double averageFps() {
        return averageFps;
    }

    public static double medianFrameMs() {
        return medianMs;
    }

    public static double onePercentLowFps() {
        return onePercentLowFps;
    }

    public static double worstFrameMs() {
        return worstMs;
    }

    public static int hitches() {
        return hitches;
    }

    public static long heapUsedMb() {
        return heapUsedBytes / BYTES_PER_MB;
    }

    public static long heapMaxMb() {
        return heapMaxBytes / BYTES_PER_MB;
    }

    public static double heapFraction() {
        return heapMaxBytes <= 0L ? 0.0d : heapUsedBytes / (double) heapMaxBytes;
    }

    /** Zero when the running JVM does not expose per-thread allocation. */
    public static double allocatedMbPerSecond() {
        return allocatedMbPerSecond;
    }

    public static double gcPerSecond() {
        return gcPerSecond;
    }

    public static double gcMillisPerSecond() {
        return gcMillisPerSecond;
    }

    /** Frames the distribution is currently computed over. */
    public static int sampleCount() {
        return count;
    }

    /**
     * Copies the trace into {@code out}, oldest first, in milliseconds.
     *
     * <p>Copied rather than exposed so the drawing cannot see the ring rotate under it mid-frame,
     * and reordered here rather than in the component because where the ring's head is is this
     * class's business. {@code out} must be {@link #columns()} long.
     */
    public static void traceInto(float[] out) {
        for (int i = 0; i < COLUMNS; i++) {
            out[i] = TRACE[(tracePosition + 1 + i) % COLUMNS] / 1.0e6f;
        }
    }

    public static int columns() {
        return COLUMNS;
    }
}
