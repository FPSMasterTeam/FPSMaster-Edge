package top.fpsmaster.benchmark;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;

/**
 * World setup for a benchmark scenario.
 *
 * <p>Superflat worlds are generated in-process from a fixed seed and preset: generation is
 * deterministic and effectively free, which removes the need to ship a save file for every scenario
 * that only needs somewhere flat to stand. Scenarios that genuinely need terrain name an existing
 * save folder instead, which the launcher copies in fresh for each run.
 */
public final class BenchWorld {

    private BenchWorld() {
    }

    /** Starts the integrated server for the scenario's world. Returns immediately; load is async. */
    public static void launch(Minecraft mc, JsonObject world) {
        String folder = world.get("folder").getAsString();
        boolean exists = new File(new File(mc.mcDataDir, "saves"), folder).isDirectory();

        if (exists) {
            // null settings means "load the existing save as-is".
            ClientLogger.info("benchmark", "loading existing world '" + folder + "'");
            mc.launchIntegratedServer(folder, folder, null);
            return;
        }

        long seed = world.has("seed") ? world.get("seed").getAsLong() : 0L;
        WorldType type = world.has("type")
                ? WorldType.parseWorldType(world.get("type").getAsString())
                : WorldType.FLAT;
        if (type == null) {
            throw new IllegalArgumentException("unknown world type: " + world.get("type").getAsString());
        }
        WorldSettings settings = new WorldSettings(seed, WorldSettings.GameType.CREATIVE,
                false /* mapFeatures */, false /* hardcore */, type);
        settings.enableCommands();
        if (world.has("generatorOptions")) {
            // For superflat, WorldSettings.worldName carries the layer preset.
            settings.setWorldName(world.get("generatorOptions").getAsString());
        }
        ClientLogger.info("benchmark", "generating world '" + folder + "' seed=" + seed + " type=" + type);
        mc.launchIntegratedServer(folder, folder, settings);
    }

    /** Whether the world is loaded far enough to start positioning the camera. */
    public static boolean isReady(Minecraft mc) {
        return mc.theWorld != null && mc.thePlayer != null && mc.currentScreen == null;
    }

    /**
     * Tracks whether the terrain has stopped rebuilding.
     *
     * <p>Reads the same per-second counter the F3 overlay shows. Vanilla resets it once a second in
     * {@code runGameLoop}, so a second in which it never rose above zero is a second in which no
     * chunk was rebuilt.
     */
    public static final class SettleTracker {

        private final int requiredCleanSeconds;
        private long secondStartMillis;
        private boolean secondDirty;
        private int cleanSeconds;

        public SettleTracker(int requiredCleanSeconds) {
            this.requiredCleanSeconds = requiredCleanSeconds;
        }

        public void reset(long nowMillis) {
            secondStartMillis = nowMillis;
            secondDirty = false;
            cleanSeconds = 0;
        }

        /** Returns true once the world has been quiet for the required number of seconds. */
        public boolean update(long nowMillis) {
            if (RenderChunk.renderChunksUpdated > 0) {
                secondDirty = true;
            }
            if (nowMillis - secondStartMillis >= 1_000L) {
                cleanSeconds = secondDirty ? 0 : cleanSeconds + 1;
                secondDirty = false;
                secondStartMillis = nowMillis;
            }
            return cleanSeconds >= requiredCleanSeconds;
        }

        public int cleanSeconds() {
            return cleanSeconds;
        }
    }
}
