package top.fpsmaster.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures fixed viewpoints at the end of a run so a change can be checked for visual regressions.
 *
 * <p>This is the safety net for culling work. Skipping geometry that should have been drawn makes
 * frame times <em>better</em>, so a timing comparison alone cannot tell an optimisation from a bug —
 * the faster number and the missing entity look identical in the report. Comparing rendered output
 * against a stored reference is the only check that distinguishes them.
 *
 * <p>Shots are taken after the measurement window closes, so the capture cost never lands inside the
 * timed samples.
 */
public final class BenchScreenshots {

    /**
     * Frames to render at a viewpoint before capturing. The camera jumps to a shot position rather
     * than easing into it, so the first frames afterwards can still be resolving chunk visibility
     * and entity interpolation.
     */
    private static final int SETTLE_FRAMES = 10;

    private final List<Shot> shots = new ArrayList<Shot>();
    private int currentShot;
    private int framesAtCurrentShot;

    private static final class Shot {
        final String name;
        final long pathMillis;

        Shot(String name, long pathMillis) {
            this.name = name;
            this.pathMillis = pathMillis;
        }
    }

    public static BenchScreenshots parse(JsonObject scenario) {
        if (scenario == null || !scenario.has("screenshots")) {
            return null;
        }
        BenchScreenshots gate = new BenchScreenshots();
        for (JsonElement element : scenario.getAsJsonArray("screenshots")) {
            JsonObject shot = element.getAsJsonObject();
            gate.shots.add(new Shot(shot.get("name").getAsString(), shot.get("t").getAsLong()));
        }
        return gate.shots.isEmpty() ? null : gate;
    }

    /** Position of the viewpoint currently being captured, as an offset into the camera path. */
    public long currentPathMillis() {
        return shots.get(currentShot).pathMillis;
    }

    /**
     * Advances the capture sequence by one frame.
     *
     * @return true once every shot has been written
     */
    public boolean advance(Minecraft mc) {
        if (++framesAtCurrentShot <= SETTLE_FRAMES) {
            return false;
        }
        capture(mc, shots.get(currentShot).name);
        framesAtCurrentShot = 0;
        return ++currentShot >= shots.size();
    }

    private void capture(Minecraft mc, String name) {
        File dir = new File(new File(mc.mcDataDir, "bench-results"), "shots");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            ClientLogger.error("benchmark", "could not create screenshot directory: " + dir);
            return;
        }
        ScreenShotHelper.saveScreenshot(dir, name + ".png",
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        ClientLogger.info("benchmark", "captured screenshot " + name);
    }

    private BenchScreenshots() {
    }

    public static JsonArray namesOf(BenchScreenshots gate) {
        JsonArray names = new JsonArray();
        if (gate != null) {
            for (Shot shot : gate.shots) {
                names.add(new com.google.gson.JsonPrimitive(shot.name));
            }
        }
        return names;
    }
}
