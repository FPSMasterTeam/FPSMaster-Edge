package top.fpsmaster.benchmark;

import com.google.gson.JsonObject;

import java.util.Arrays;

/**
 * Per-section CPU and GPU timing for a benchmark run.
 *
 * <p>Whole-frame frame rate is a blunt instrument for judging a targeted change: an optimisation
 * that halves the cost of the entity pass moves the frame time by whatever fraction of the frame
 * that pass happened to be, which on most scenes is small enough to sit inside the run-to-run noise
 * band. Requiring every change to shift end-to-end FPS therefore rejects real improvements. Timing
 * the section that was actually changed measures the thing that changed.
 *
 * <p>Sections are a fixed enum-like set of int constants so the hot path does no lookups and no
 * allocation. Everything here is behind {@link BenchmarkMode#ACTIVE}, which HotSpot folds away
 * outside benchmark runs.
 */
public final class BenchProfiler {

    public static final int SECTION_TERRAIN = 0;
    public static final int SECTION_ENTITIES = 1;
    public static final int SECTION_PARTICLES = 2;
    public static final int SECTION_HUD = 3;
    public static final int SECTION_CHUNK_UPLOAD = 4;
    public static final int SECTION_SKY = 5;
    public static final int SECTION_CLOUDS = 6;
    public static final int SECTION_HAND = 7;
    public static final int SECTION_TERRAIN_SETUP = 8;
    public static final int SECTION_FRAME_TOTAL = 9;
    /** Per-entity render, nested inside {@link #SECTION_ENTITIES}. */
    public static final int SECTION_ENTITY_RENDER = 10;
    /** Sub-phases of {@link #SECTION_ENTITY_RENDER}, to find where 9.6us per entity goes. */
    public static final int SECTION_ENTITY_MODEL = 11;
    public static final int SECTION_ENTITY_LAYERS = 12;
    public static final int SECTION_ENTITY_BRIGHTNESS = 13;
    public static final int SECTION_ENTITY_SHADOW = 14;
    /** Animated texture upload, the target Smart Animations would address. */
    public static final int SECTION_TEXTURE_ANIM = 15;
    /**
     * The block-entity pass, nested inside {@link #SECTION_ENTITIES}.
     *
     * <p>Signs, chests, enchanting tables, banners and skulls are drawn in immediate mode every
     * frame rather than baked into chunk geometry, and a sign additionally re-lays out its text
     * through the font renderer. That is the only per-frame cost block rendering carries, so it
     * needs its own bracket before any of it can be called worth optimising.
     */
    public static final int SECTION_BLOCK_ENTITIES = 16;
    /**
     * The chunk draws themselves, bracketed at {@code VertexBuffer.drawArrays}.
     *
     * <p>Exists to settle a contradiction rather than to find a target. The {@code terrain} section
     * brackets {@code renderBlockLayer}, which contains these draws, and reports 17us of GPU time —
     * while cancelling half of them saved 377us. Two measurements of the same work, forty times
     * apart. Either the outer bracket is not where the work is, or the GPU timestamps are not
     * attributing it, and putting a bracket directly around the draw call distinguishes those.
     */
    public static final int SECTION_TERRAIN_DRAW = 17;
    public static final int SECTION_COUNT = 18;

    private static final String[] NAMES = {
            "terrain", "entities", "particles", "hud", "chunkUpload",
            "sky", "clouds", "hand", "terrainSetup", "frameTotal", "entityRender",
            "entityModel", "entityLayers", "entityBrightness", "entityShadow", "textureAnim",
            "blockEntities", "terrainDraw",
    };

    private static final BenchProfiler INSTANCE = new BenchProfiler();

    /**
     * Per-frame samples per section, in capture order.
     *
     * <p>Microseconds in an {@code int}, not nanoseconds in a {@code long}. Section times are
     * single- to triple-digit microseconds, so the resolution is ample, and the narrower type halves
     * the footprint — which matters because this array is per section and the section count grows.
     * At sixteen sections the long version reserved 67 MB, and first-touching those pages when
     * recording began produced a 1.2 second ramp of slow frames at the start of every measurement
     * window: the instrument was perturbing what it measured.
     */
    private static final int CAPACITY = 131_072;

    private final int[][] cpuPerFrame = new int[SECTION_COUNT][];
    private final int[][] gpuPerFrame = new int[SECTION_COUNT][];
    private final long[] frameStartNanos = new long[SECTION_COUNT];
    private final long[] frameCpuNanos = new long[SECTION_COUNT];
    private final int[] depth = new int[SECTION_COUNT];
    private GpuTimer gpuTimer;
    private int count;
    private boolean recording;

    private BenchProfiler() {
        for (int i = 0; i < SECTION_COUNT; i++) {
            cpuPerFrame[i] = new int[CAPACITY];
            gpuPerFrame[i] = new int[CAPACITY];
        }
    }

    public static BenchProfiler instance() {
        return INSTANCE;
    }

    /** Must be called with a current GL context before any section is timed. */
    public void initGpuTimer() {
        if (Experiments.active(Experiments.NO_GPU_TIMER)) {
            // Ceiling probe on the instrumentation itself. Each section issues two timestamp
            // queries per frame, and a query can make the driver do more than record a number.
            return;
        }
        if (gpuTimer == null) {
            gpuTimer = new GpuTimer(SECTION_COUNT);
        }
    }

    public static void begin(int section) {
        BenchProfiler self = INSTANCE;
        // Guard against re-entry: a section that nests inside itself would otherwise double-count.
        if (self.depth[section]++ != 0) {
            return;
        }
        self.frameStartNanos[section] = System.nanoTime();
        if (self.gpuTimer != null) {
            self.gpuTimer.begin(section);
        }
    }

    public static void end(int section) {
        BenchProfiler self = INSTANCE;
        if (--self.depth[section] != 0) {
            return;
        }
        self.frameCpuNanos[section] += System.nanoTime() - self.frameStartNanos[section];
        if (self.gpuTimer != null) {
            self.gpuTimer.end(section);
        }
    }

    /** Closes the frame, moving this frame's accumulations into the sample arrays. */
    public void endFrame() {
        if (gpuTimer != null) {
            gpuTimer.endFrame();
        }
        if (recording && count < CAPACITY) {
            for (int i = 0; i < SECTION_COUNT; i++) {
                cpuPerFrame[i][count] = (int) (frameCpuNanos[i] / 1000L);
                // -1, not 0, when this frame produced no GPU sample for the section — either it
                // was never entered or its timestamps were not back yet. Writing 0 put those
                // frames into the distribution as if the GPU had done nothing, and a median over
                // an array that is mostly zeros is zero: entities and terrain both reported
                // gpuMicros p50 = 0 while the frame as a whole reported 1553us. The number was not
                // small, it was absent, and nothing in the output said so.
                long gpu = gpuTimer == null ? -1L : gpuTimer.nanos(i);
                gpuPerFrame[i][count] = gpu <= 0L ? -1 : (int) (gpu / 1000L);
            }
            count++;
        }
        Arrays.fill(frameCpuNanos, 0L);
        Arrays.fill(depth, 0);
        if (gpuTimer != null) {
            gpuTimer.reset();
        }
    }

    public void start() {
        count = 0;
        // Touch every page before recording so the page faults land in the warmup window rather
        // than in the measured one.
        for (int i = 0; i < SECTION_COUNT; i++) {
            java.util.Arrays.fill(cpuPerFrame[i], 0);
            java.util.Arrays.fill(gpuPerFrame[i], 0);
        }
        recording = true;
    }

    public void stop() {
        recording = false;
    }

    public void discardCollected() {
        count = 0;
    }

    /**
     * Per-section summary. Medians and percentiles only: desktop GL cannot report that a clock
     * change invalidated a GPU sample, so a mean would be at the mercy of undetectable outliers.
     *
     * <p>{@code frameTotal} brackets the whole world render, so the sections inside it can be
     * checked against it. Anything the individual brackets do not account for is time that has not
     * been looked at yet, which is exactly what should drive the next round of work rather than a
     * guess about where the cost is.
     */
    public JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("gpuTimingAvailable", gpuTimer != null && gpuTimer.isSupported());
        root.addProperty("frames", count);
        for (int i = 0; i < SECTION_COUNT; i++) {
            JsonObject section = new JsonObject();
            section.add("cpuMicros", describe(cpuPerFrame[i], count));
            if (gpuTimer != null && gpuTimer.isSupported()) {
                section.add("gpuMicros", describeGpu(gpuPerFrame[i], count));
            }
            root.add(NAMES[i], section);
        }
        return root;
    }

    /**
     * The same summary over only the frames that produced a GPU sample.
     *
     * <p>Carries {@code samples} alongside the percentiles, because a GPU figure means nothing
     * without knowing how many frames it came from. A section timed in a tenth of the frames is not
     * a section that is cheap.
     */
    private static JsonObject describeGpu(int[] samples, int length) {
        int[] valid = new int[length];
        int found = 0;
        for (int i = 0; i < length; i++) {
            if (samples[i] >= 0) {
                valid[found++] = samples[i];
            }
        }
        JsonObject json = describe(valid, found);
        json.addProperty("samples", Integer.valueOf(found));
        json.addProperty("coverage", found == 0 ? Double.valueOf(0.0d)
                : Double.valueOf(Math.round(1000.0d * found / length) / 10.0d));
        return json;
    }

    private static JsonObject describe(int[] samples, int length) {
        JsonObject json = new JsonObject();
        if (length == 0) {
            return json;
        }
        int[] sorted = Arrays.copyOf(samples, length);
        Arrays.sort(sorted);
        long total = 0L;
        for (int i = 0; i < length; i++) {
            total += sorted[i];
        }
        // Samples are already microseconds.
        json.addProperty("p50", (double) sorted[length / 2]);
        json.addProperty("p95", (double) sorted[Math.min(length - 1, (int) (length * 0.95d))]);
        json.addProperty("p99", (double) sorted[Math.min(length - 1, (int) (length * 0.99d))]);
        json.addProperty("totalMillis", total / 1000.0d);
        return json;
    }
}
