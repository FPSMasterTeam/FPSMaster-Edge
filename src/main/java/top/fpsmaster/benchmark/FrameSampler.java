package top.fpsmaster.benchmark;

/**
 * Records raw per-frame CPU times for a benchmark run.
 *
 * <p>Samples are appended as nanosecond deltas between successive frame boundaries. Nothing is
 * aggregated while recording: percentiles are derived from the full sample set afterwards, because
 * a running average would destroy exactly the tail-latency information the run exists to measure.
 *
 * <p>Vanilla's {@code Minecraft.frameTimer} is not usable for this — it is a 240-slot ring buffer,
 * which at 300 FPS holds only 0.8 seconds of history.
 */
public final class FrameSampler {

    private final long[] frameNanos;
    private int count;
    private long previousFrameNano;
    private boolean recording;

    public FrameSampler(int capacity) {
        this.frameNanos = new long[capacity];
    }

    /**
     * Marks a frame boundary. Must be called once per frame, from the render thread, whether or not
     * recording is active — the first call after {@link #start} establishes the baseline timestamp.
     */
    public void onFrame(long nowNano) {
        if (previousFrameNano != 0L && recording && count < frameNanos.length) {
            frameNanos[count++] = nowNano - previousFrameNano;
        }
        previousFrameNano = nowNano;
    }

    public void start() {
        count = 0;
        previousFrameNano = 0L;
        recording = true;
    }

    public void stop() {
        recording = false;
    }

    /**
     * Drops the samples collected so far but keeps recording.
     *
     * <p>Used to discard the first second of a measurement window: {@code getDebugFPS()} is 0 until
     * the first counter flush, and it feeds the chunk-upload budget in
     * {@code EntityRenderer.updateCameraAndRender}, so early frames are not comparable to the rest.
     */
    public void discardCollected() {
        count = 0;
    }

    public boolean isRecording() {
        return recording;
    }

    public int sampleCount() {
        return count;
    }

    public boolean isFull() {
        return count >= frameNanos.length;
    }

    /** Returns a copy of the collected samples, in capture order. */
    public long[] samples() {
        long[] copy = new long[count];
        System.arraycopy(frameNanos, 0, copy, 0, count);
        return copy;
    }
}
