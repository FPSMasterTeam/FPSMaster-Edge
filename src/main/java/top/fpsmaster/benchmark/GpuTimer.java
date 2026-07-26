package top.fpsmaster.benchmark;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

/**
 * Asynchronous GPU timing for named render sections.
 *
 * <p>Uses {@code glQueryCounter(GL_TIMESTAMP)} pairs rather than {@code GL_TIME_ELAPSED}: elapsed
 * queries cannot nest or overlap — a second {@code glBeginQuery} on the same target is an
 * {@code GL_INVALID_OPERATION} — which rules out timing a section that contains another. Timestamps
 * have no such restriction.
 *
 * <p>Results are harvested several frames later and only when {@code GL_QUERY_RESULT_AVAILABLE}
 * says so. Reading {@code GL_QUERY_RESULT} directly blocks until the GPU catches up, which would
 * stall the very frame being measured. A sample that is not ready in time is dropped rather than
 * waited for.
 *
 * <p>Values are 64-bit. The 32-bit query accessors wrap after about 4.3 seconds of GPU time, and
 * the ARB_timer_query specification explicitly warns against using them for this.
 *
 * <p>Caveat worth keeping in mind when reading the numbers: desktop GL has no equivalent of
 * {@code GPU_DISJOINT_EXT}, so there is no way to detect that a clock change invalidated a sample.
 * Report medians and percentiles, never means.
 */
public final class GpuTimer {

    /** Frames of latency before a query pair is harvested. Three is enough for a queued frame. */
    private static final int RING_DEPTH = 4;

    private final int sectionCount;
    private final int[] startQueries;
    private final int[] endQueries;
    private final boolean[] issued;
    private final long[] accumulated;
    private final boolean supported;
    private int slot;

    public GpuTimer(int sectionCount) {
        this.sectionCount = sectionCount;
        this.supported = GLContext.getCapabilities().OpenGL33;
        this.accumulated = new long[sectionCount];
        if (!supported) {
            this.startQueries = null;
            this.endQueries = null;
            this.issued = null;
            return;
        }
        this.startQueries = new int[sectionCount * RING_DEPTH];
        this.endQueries = new int[sectionCount * RING_DEPTH];
        this.issued = new boolean[sectionCount * RING_DEPTH];
        for (int i = 0; i < startQueries.length; i++) {
            startQueries[i] = GL15.glGenQueries();
            endQueries[i] = GL15.glGenQueries();
        }
    }

    public boolean isSupported() {
        return supported;
    }

    public void begin(int section) {
        if (supported) {
            GL33.glQueryCounter(startQueries[index(section)], GL33.GL_TIMESTAMP);
        }
    }

    public void end(int section) {
        if (supported) {
            int i = index(section);
            GL33.glQueryCounter(endQueries[i], GL33.GL_TIMESTAMP);
            issued[i] = true;
        }
    }

    /**
     * Advances the ring and harvests whatever is ready. Call once per frame, after the last
     * {@link #end}.
     */
    public void endFrame() {
        if (!supported) {
            return;
        }
        slot = (slot + 1) % RING_DEPTH;
        // The slot about to be reused is the oldest, so its results are the most likely to be ready.
        for (int section = 0; section < sectionCount; section++) {
            int i = index(section);
            if (!issued[i]) {
                continue;
            }
            // GL_QUERY_RESULT_AVAILABLE is in GL15 while GL_TIMESTAMP is in GL33; the read is
            // always a cross-class expression in LWJGL 2.
            if (GL15.glGetQueryObjectui(endQueries[i], GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE
                    && GL15.glGetQueryObjectui(startQueries[i], GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE) {
                long start = GL33.glGetQueryObjecti64(startQueries[i], GL15.GL_QUERY_RESULT);
                long end = GL33.glGetQueryObjecti64(endQueries[i], GL15.GL_QUERY_RESULT);
                accumulated[section] += Math.max(0L, end - start);
                issued[i] = false;
            }
        }
    }

    /** Total GPU nanoseconds harvested for a section since the last {@link #reset}. */
    public long nanos(int section) {
        return accumulated[section];
    }

    public void reset() {
        java.util.Arrays.fill(accumulated, 0L);
    }

    private int index(int section) {
        return slot * sectionCount + section;
    }
}
