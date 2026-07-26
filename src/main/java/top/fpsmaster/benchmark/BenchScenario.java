package top.fpsmaster.benchmark;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * One benchmark scenario, loaded from {@code <gameDir>/scenarios/<id>.json}.
 *
 * <p>Phase lengths are wall-clock rather than frame counts on purpose. A faster build must render
 * more frames over the same workload, not travel the same camera path in less time — otherwise the
 * two sides of an A/B are not rendering the same thing.
 */
public final class BenchScenario {

    private final String id;
    private final String world;
    private final long warmupMillis;
    private final long discardMillis;
    private final long measureMillis;

    private BenchScenario(String id, String world, long warmupMillis, long discardMillis, long measureMillis) {
        this.id = id;
        this.world = world;
        this.warmupMillis = warmupMillis;
        this.discardMillis = discardMillis;
        this.measureMillis = measureMillis;
    }

    public static BenchScenario load(File gameDir, String id) throws IOException {
        File file = new File(new File(gameDir, "scenarios"), id + ".json");
        if (!file.isFile()) {
            throw new IOException("scenario not found: " + file.getAbsolutePath());
        }
        String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        JsonObject json = new JsonParser().parse(raw).getAsJsonObject();
        return new BenchScenario(
                id,
                json.has("world") && !json.get("world").isJsonNull() ? json.get("world").getAsString() : null,
                json.has("warmupMillis") ? json.get("warmupMillis").getAsLong() : 30_000L,
                json.has("discardMillis") ? json.get("discardMillis").getAsLong() : 1_000L,
                json.has("measureMillis") ? json.get("measureMillis").getAsLong() : 90_000L);
    }

    public String id() {
        return id;
    }

    /** Save folder to load, or {@code null} to benchmark without entering a world. */
    public String world() {
        return world;
    }

    public long warmupMillis() {
        return warmupMillis;
    }

    /**
     * How much of the measurement window to throw away at the start. Defaults to one second:
     * {@code Minecraft.getDebugFPS()} reads 0 until its first flush, and it feeds the per-frame
     * chunk-upload budget, so those frames behave differently from the rest of the run.
     */
    public long discardMillis() {
        return discardMillis;
    }

    public long measureMillis() {
        return measureMillis;
    }
}
