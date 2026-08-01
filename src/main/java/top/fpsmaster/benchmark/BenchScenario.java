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
    private final JsonObject world;
    private final String replay;
    private final BenchCamera camera;
    private final BenchScreenshots screenshots;
    private final BenchStress stress;
    private final int settleSeconds;
    private final long settleTimeoutMillis;
    private final long warmupMillis;
    private final long discardMillis;
    private final long measureMillis;
    private final long replayMeasureFromMillis;

    private BenchScenario(String id, JsonObject world, String replay, BenchCamera camera,
                          BenchScreenshots screenshots, BenchStress stress, int settleSeconds,
                          long settleTimeoutMillis, long warmupMillis, long discardMillis,
                          long measureMillis, long replayMeasureFromMillis) {
        this.id = id;
        this.world = world;
        this.replay = replay;
        this.camera = camera;
        this.screenshots = screenshots;
        this.stress = stress;
        this.settleSeconds = settleSeconds;
        this.settleTimeoutMillis = settleTimeoutMillis;
        this.warmupMillis = warmupMillis;
        this.discardMillis = discardMillis;
        this.measureMillis = measureMillis;
        this.replayMeasureFromMillis = replayMeasureFromMillis;
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
                object(json, "world"),
                json.has("replay") ? json.get("replay").getAsString() : null,
                BenchCamera.parse(object(json, "camera")),
                BenchScreenshots.parse(json),
                BenchStress.parse(json),
                json.has("settleSeconds") ? json.get("settleSeconds").getAsInt() : 5,
                json.has("settleTimeoutMillis") ? json.get("settleTimeoutMillis").getAsLong() : 120_000L,
                json.has("warmupMillis") ? json.get("warmupMillis").getAsLong() : 30_000L,
                json.has("discardMillis") ? json.get("discardMillis").getAsLong() : 1_000L,
                json.has("measureMillis") ? json.get("measureMillis").getAsLong() : 90_000L,
                json.has("replayMeasureFromMillis")
                        ? json.get("replayMeasureFromMillis").getAsLong() : -1L);
    }

    private static JsonObject object(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : null;
    }

    public String id() {
        return id;
    }

    /**
     * Recording to play as the workload, or null for a generated world.
     *
     * <p>A recording is the only workload here that contains what actually costs a PvP client:
     * players with their own skins and nameplates, server-driven particles, scoreboard text, and
     * chunk data arriving while the camera moves. It is also repeatable in a way a live server is
     * not - the same packets at the same offsets every run - and the camera is the recorder's own,
     * so it cannot drift between the two sides of a comparison.
     */
    public String replay() {
        return replay;
    }

    /** World descriptor, or {@code null} to benchmark without entering a world. */
    public JsonObject world() {
        return world;
    }

    public BenchCamera camera() {
        return camera;
    }

    /** Fixed viewpoints captured after measurement, or {@code null} when the scenario has none. */
    public BenchScreenshots screenshots() {
        return screenshots;
    }

    /** Feature toggling during measurement, for leak hunting. Null for normal scenarios. */
    public BenchStress stress() {
        return stress;
    }

    /**
     * Consecutive seconds with no chunk rebuilds before measurement may begin.
     *
     * <p>{@code RenderChunk.renderChunksUpdated} is reset once a second by {@code runGameLoop},
     * which makes it the most direct available signal for "the visible world has finished
     * building".
     */
    public int settleSeconds() {
        return settleSeconds;
    }

    public long settleTimeoutMillis() {
        return settleTimeoutMillis;
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

    /**
     * Replay position the measured window opens at, or -1 to keep the wall-clock behaviour.
     *
     * <p>Without this a replay run measures a wall-clock slice, and the slice lands wherever the
     * recording happened to be when settle and discard finished — discard ends on a steady frame
     * time rather than a clock, so a slower run starts later in the recording and sees a different
     * scene. Measured on {@code replay-pit}: three variants of one ceiling probe rendered 21.5, 23.0
     * and 24.9 entities a frame, a 16% spread in the workload the probe was trying to hold still.
     *
     * <p>With it, every run measures the same span of the recording, so two runs are comparable
     * frame for frame.
     */
    public long replayMeasureFromMillis() {
        return replayMeasureFromMillis;
    }

    public long measureMillis() {
        return measureMillis;
    }
}
