package top.fpsmaster.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

/**
 * Automated check that a recording actually plays.
 *
 * <p>Playback is the kind of feature that is easy to declare working and hard to be sure of: the log
 * can be clean, the file can round-trip, and the screen can still be empty. So a run driven by
 * {@code -Dedge.replay.play} samples the world at fixed points, writes what it found, and takes a
 * screenshot at each — a recording that plays into nothing shows up as zero entities and a blank
 * image rather than as silence.
 *
 * <pre>
 *   -Dedge.replay.play=&lt;name&gt;  -Dedge.replay.probeAt=5,20,40
 * </pre>
 */
public final class ReplayProbe {

    private static int[] probeSeconds;
    private static int nextProbe;
    private static final JsonArray SAMPLES = new JsonArray();
    private static boolean finished;
    private static int possessFrom = -1;
    private static boolean thirdPerson;

    private ReplayProbe() {
    }

    static {
        String possess = System.getProperty("edge.replay.probePossessFrom");
        if (possess != null && !possess.isEmpty()) {
            possessFrom = Integer.parseInt(possess.trim());
        }
        thirdPerson = Boolean.getBoolean("edge.replay.probeThirdPerson");
        String requested = System.getProperty("edge.replay.probeAt");
        if (requested != null && !requested.isEmpty()) {
            String[] parts = requested.split(",");
            probeSeconds = new int[parts.length];
            for (int index = 0; index < parts.length; index++) {
                probeSeconds[index] = Integer.parseInt(parts[index].trim());
            }
        }
    }

    public static void onClientTick() {
        if (probeSeconds == null || finished || !ReplayPlayer.instance().isActive()) {
            return;
        }
        int elapsedSeconds = ReplayPlayer.instance().elapsedMillis() / 1000;
        Minecraft mc = Minecraft.getMinecraft();
        // Applied on its own schedule, not at a probe point: a screenshot reads the framebuffer as
        // it stands, which is the frame rendered before this tick. Changing the camera and capturing
        // in one tick photographs the state the probe just left behind.
        if (possessFrom >= 0 && elapsedSeconds >= possessFrom && !ReplayPlayer.instance().isPossessing()) {
            ReplayPlayer.instance().possess();
            mc.gameSettings.thirdPersonView = thirdPerson ? 1 : 0;
        }
        if (nextProbe >= probeSeconds.length || elapsedSeconds < probeSeconds[nextProbe]) {
            return;
        }
        SAMPLES.add(sample(mc, elapsedSeconds));
        capture(mc, "replay-" + probeSeconds[nextProbe] + "s");
        nextProbe++;
        if (nextProbe >= probeSeconds.length) {
            finish(mc);
        }
    }

    private static JsonObject sample(Minecraft mc, int elapsedSeconds) {
        JsonObject sample = new JsonObject();
        sample.addProperty("atSeconds", Integer.valueOf(elapsedSeconds));
        sample.addProperty("entities", Integer.valueOf(
                mc.theWorld == null ? 0 : mc.theWorld.loadedEntityList.size()));
        sample.addProperty("players", Integer.valueOf(
                mc.theWorld == null ? 0 : mc.theWorld.playerEntities.size()));
        sample.addProperty("tabList", Integer.valueOf(
                mc.getNetHandler() == null ? 0 : mc.getNetHandler().getPlayerInfoMap().size()));
        sample.addProperty("renderInfo", mc.renderGlobal == null ? "" : mc.renderGlobal.getDebugInfoRenders());
        sample.addProperty("possessing", Boolean.valueOf(ReplayPlayer.instance().isPossessing()));
        sample.addProperty("viewEntity", mc.getRenderViewEntity() == null
                ? "none" : mc.getRenderViewEntity().getName() + "#" + mc.getRenderViewEntity().getEntityId());
        if (mc.thePlayer != null) {
            sample.addProperty("cameraX", Double.valueOf(mc.thePlayer.posX));
            sample.addProperty("cameraY", Double.valueOf(mc.thePlayer.posY));
            sample.addProperty("cameraZ", Double.valueOf(mc.thePlayer.posZ));
            sample.addProperty("cameraIsSpectator", Boolean.valueOf(mc.thePlayer.isSpectator()));
        }
        JsonObject avatar = ReplayPlayer.instance().avatarState();
        if (avatar != null) {
            sample.add("avatar", avatar);
        }
        return sample;
    }

    private static void capture(Minecraft mc, String name) {
        File directory = new File(mc.mcDataDir, "bench-results");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            ClientLogger.error("replay", "could not create " + directory);
            return;
        }
        ScreenShotHelper.saveScreenshot(directory, name + ".png",
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void finish(Minecraft mc) {
        finished = true;
        JsonObject result = new JsonObject();
        result.addProperty("replay", ReplayPlayer.instance().file() == null
                ? "" : ReplayPlayer.instance().file().getName());
        result.addProperty("recorder", ReplayPlayer.instance().recorderName());
        result.add("samples", SAMPLES);

        File directory = new File(mc.mcDataDir, "bench-results");
        Writer writer = null;
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new java.io.IOException("could not create " + directory);
            }
            writer = new FileWriter(new File(directory, "replay-probe.json"));
            writer.write(result.toString());
        } catch (Exception failure) {
            ClientLogger.error("replay", "could not write the probe result: " + failure);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception closeFailure) {
                    ClientLogger.error("replay", "could not close the probe result: " + closeFailure);
                }
            }
        }
        ClientLogger.info("replay", "probe complete, shutting down");
        mc.shutdown();
    }
}
