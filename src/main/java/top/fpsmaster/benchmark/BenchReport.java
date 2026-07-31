package top.fpsmaster.benchmark;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Serialises one benchmark run to {@code <gameDir>/bench-results/result.json}.
 *
 * <p>Raw frame times are always written alongside the summary: the comparison tooling pools samples
 * across runs, which cannot be done from pre-aggregated percentiles.
 */
public final class BenchReport {

    /** Frame times are stored as nanoseconds and only converted to FPS in the summary. */
    private static final double NANOS_PER_MILLI = 1_000_000.0d;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0d;

    private BenchReport() {
    }

    public static void write(File gameDir, FrameSampler sampler, DisplayWatch displayWatch,
                             long[] countersAtMeasureStart, long gcCountBefore, long gcMillisBefore,
                             int replayFromMillis, int replayToMillis)
            throws IOException {
        long[] samples = sampler.samples();

        JsonObject root = new JsonObject();
        root.addProperty("scenario", BenchmarkMode.scenario());
        root.addProperty("variant", BenchmarkMode.variant());
        root.addProperty("wallClockUtcMillis", System.currentTimeMillis());
        if (BenchmarkMode.overrides() != null) {
            root.add("overrides", BenchmarkMode.overrides());
        }
        // Which span of the recording this measured. Two replay runs are only comparable frame for
        // frame if these match; without them, an analysis has to assume alignment it cannot check.
        if (replayFromMillis >= 0) {
            JsonObject window = new JsonObject();
            window.addProperty("fromMillis", Integer.valueOf(replayFromMillis));
            window.addProperty("toMillis", Integer.valueOf(replayToMillis));
            root.add("replayWindow", window);
        }
        root.add("gl", describeGl());
        root.add("java", describeJava());
        root.add("settings", describeSettings());
        root.add("gc", describeGc(gcCountBefore, gcMillisBefore));
        long[] countersNow = BenchCounters.values();
        // Both are reported: "did this code path run at all" is answered by the run total (some
        // paths, such as display-list compilation, fire only during startup), while "how much work
        // did the measured window do" is answered by the delta.
        root.add("counters", BenchCounters.toJson(
                BenchCounters.difference(countersNow, countersAtMeasureStart)));
        root.add("countersTotal", BenchCounters.toJson(countersNow));
        root.add("summary", summarise(samples, displayWatch));
        root.add("sections", BenchProfiler.instance().snapshot());
        root.add("memory", describeMemory());

        // Gson 2.2.4 ships with Minecraft 1.8.9 and has no primitive JsonArray.add overloads.
        JsonArray raw = new JsonArray();
        for (long sample : samples) {
            raw.add(new JsonPrimitive(sample));
        }
        root.add("frameNanos", raw);

        File outDir = new File(gameDir, "bench-results");
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            throw new IOException("could not create benchmark result directory: " + outDir);
        }
        File out = new File(outDir, "result.json");
        String json = new GsonBuilder().create().toJson(root);
        Files.write(out.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the summary statistics. Deliberately explicit about which definition is used, since
     * "1% low" has several incompatible ones in circulation.
     */
    private static JsonObject summarise(long[] samples, DisplayWatch displayWatch) {
        JsonObject summary = new JsonObject();
        summary.addProperty("frameCount", samples.length);
        // Always reported, even when zero: a silently dropped frame is indistinguishable from
        // a clean run, and one dragged window is enough to move the 1% low by tens of percent.
        summary.addProperty("disturbedFrames", displayWatch.disturbedFrames());
        summary.addProperty("unfocusedFrames", displayWatch.unfocusedFrames());
        if (samples.length == 0) {
            return summary;
        }

        long total = 0L;
        for (long sample : samples) {
            total += sample;
        }
        long[] sorted = samples.clone();
        Arrays.sort(sorted);

        // avg FPS is frames divided by elapsed time, never the mean of per-frame FPS values, which
        // short frames would drag upwards.
        summary.addProperty("durationSeconds", total / NANOS_PER_SECOND);
        summary.addProperty("avgFps", samples.length / (total / NANOS_PER_SECOND));
        summary.addProperty("meanFrameMs", total / (double) samples.length / NANOS_PER_MILLI);
        summary.addProperty("p50FrameMs", percentile(sorted, 50.0d) / NANOS_PER_MILLI);
        summary.addProperty("p95FrameMs", percentile(sorted, 95.0d) / NANOS_PER_MILLI);
        summary.addProperty("p99FrameMs", percentile(sorted, 99.0d) / NANOS_PER_MILLI);
        summary.addProperty("maxFrameMs", sorted[sorted.length - 1] / NANOS_PER_MILLI);
        // 1% low, GamersNexus definition: the mean of the slowest 1% of frames, expressed as FPS.
        summary.addProperty("onePercentLowFps", slowestTailFps(sorted, 0.01d));
        // 0.1% low needs roughly 20k frames to mean anything; below that the comparison tooling
        // pools samples across runs instead. It is emitted here only when the sample set supports it.
        if (samples.length >= 20_000) {
            summary.addProperty("pointOnePercentLowFps", slowestTailFps(sorted, 0.001d));
        }
        return summary;
    }

    private static double slowestTailFps(long[] sortedAscending, double fraction) {
        int tail = Math.max(1, (int) Math.round(sortedAscending.length * fraction));
        long total = 0L;
        for (int i = sortedAscending.length - tail; i < sortedAscending.length; i++) {
            total += sortedAscending[i];
        }
        return NANOS_PER_SECOND / (total / (double) tail);
    }

    private static long percentile(long[] sortedAscending, double percent) {
        int index = (int) Math.ceil(percent / 100.0d * sortedAscending.length) - 1;
        return sortedAscending[Math.min(Math.max(index, 0), sortedAscending.length - 1)];
    }

    private static JsonObject describeGl() {
        JsonObject gl = new JsonObject();
        // Recorded on every run: a hybrid-graphics machine can silently hand the context to the
        // integrated GPU, which would make the run incomparable to the rest of the series.
        gl.addProperty("renderer", GL11.glGetString(GL11.GL_RENDERER));
        gl.addProperty("vendor", GL11.glGetString(GL11.GL_VENDOR));
        gl.addProperty("version", GL11.glGetString(GL11.GL_VERSION));
        gl.addProperty("displayWidth", Display.getWidth());
        gl.addProperty("displayHeight", Display.getHeight());
        return gl;
    }

    private static JsonObject describeJava() {
        JsonObject java = new JsonObject();
        java.addProperty("version", System.getProperty("java.version"));
        java.addProperty("vmName", System.getProperty("java.vm.name"));
        java.addProperty("maxHeapMb", Runtime.getRuntime().maxMemory() / (1024L * 1024L));
        return java;
    }

    private static JsonObject describeSettings() {
        GameSettings settings = Minecraft.getMinecraft().gameSettings;
        JsonObject json = new JsonObject();
        json.addProperty("limitFramerate", settings.limitFramerate);
        json.addProperty("enableVsync", settings.enableVsync);
        json.addProperty("renderDistanceChunks", settings.renderDistanceChunks);
        json.addProperty("useVbo", settings.useVbo);
        json.addProperty("fboEnable", settings.fboEnable);
        json.addProperty("fancyGraphics", settings.fancyGraphics);
        json.addProperty("particleSetting", settings.particleSetting);
        json.addProperty("ambientOcclusion", settings.ambientOcclusion);
        json.addProperty("mipmapLevels", settings.mipmapLevels);
        json.addProperty("guiScale", settings.guiScale);
        return json;
    }

    private static JsonObject describeGc(long countBefore, long millisBefore) {
        JsonObject gc = new JsonObject();
        gc.addProperty("collections", gcCollectionCount() - countBefore);
        gc.addProperty("collectionMillis", gcCollectionMillis() - millisBefore);
        return gc;
    }

    /**
     * Heap figures for the leak and footprint goals. Sampled at report time, which is right after
     * the measurement window closes, so it reflects the steady state of the measured workload.
     */
    private static JsonObject describeMemory() {
        Runtime runtime = Runtime.getRuntime();
        JsonObject json = new JsonObject();
        json.addProperty("heapUsedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L));
        json.addProperty("heapCommittedMb", runtime.totalMemory() / (1024L * 1024L));
        json.addProperty("heapMaxMb", runtime.maxMemory() / (1024L * 1024L));
        return json;
    }

    public static long gcCollectionCount() {
        long total = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            if (count > 0L) {
                total += count;
            }
        }
        return total;
    }

    public static long gcCollectionMillis() {
        long total = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long millis = bean.getCollectionTime();
            if (millis > 0L) {
                total += millis;
            }
        }
        return total;
    }
}
