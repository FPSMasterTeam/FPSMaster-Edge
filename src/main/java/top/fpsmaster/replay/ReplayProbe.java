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
    private static boolean disconnected;
    private static boolean respawned;
    private static int stirStage;

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
        maybeStirContainer();
        maybeDisconnect();
        maybeRespawn();
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

    /**
     * Opens the inventory during a recording and moves an item, so the container path can be tested.
     *
     * <p>Written straight into the container rather than through a click, which is what a click
     * would have produced locally anyway - the recorder observes the slots, not the action. Without
     * this there is no way to produce a recording containing a container interaction without a
     * person sitting there doing it.
     */
    private static void maybeStirContainer() {
        int at = Integer.getInteger("edge.replay.probeContainerAt", -1).intValue();
        if (at < 0 || stirStage > 2 || !ReplayRecorder.instance().isRecording()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        long elapsed = ReplayRecorder.instance().elapsedMillis() / 1000L;
        if (stirStage == 0 && elapsed >= at) {
            mc.thePlayer.inventory.mainInventory[0] =
                    new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_sword);
            mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.thePlayer));
            stirStage = 1;
            ClientLogger.info("replay", "probe: opened the inventory with a sword in hotbar 0");
        } else if (stirStage == 1 && elapsed >= at + 3) {
            // 36 is the first hotbar slot of the player container, 9 the first main-inventory slot.
            net.minecraft.inventory.Container container = mc.thePlayer.openContainer;
            net.minecraft.item.ItemStack moved = container.getSlot(36).getStack();
            container.putStackInSlot(36, null);
            container.putStackInSlot(9, moved);
            stirStage = 2;
            ClientLogger.info("replay", "probe: moved the sword from slot 36 to slot 9");
        } else if (stirStage == 2 && elapsed >= at + 6) {
            mc.displayGuiScreen(null);
            stirStage = 3;
            ClientLogger.info("replay", "probe: closed the inventory");
        }
    }

    /**
     * Does exactly what the in-game menu's Disconnect button does, while a replay is playing.
     *
     * <p>Copied statement for statement from {@code GuiIngameMenu.actionPerformed} case 1, taking
     * the branch a replay session falls into: no integrated server and no realm. Reproducing it
     * here is the only way to test that path without a human clicking the button.
     */
    private static void maybeDisconnect() {
        int at = Integer.getInteger("edge.replay.probeDisconnectAt", -1).intValue();
        if (at < 0 || disconnected || !ReplayPlayer.instance().isActive()
                || ReplayPlayer.instance().elapsedMillis() / 1000 < at) {
            return;
        }
        disconnected = true;
        Minecraft mc = Minecraft.getMinecraft();
        ClientLogger.info("replay", "probe: pressing Disconnect mid-playback");
        mc.theWorld.sendQuittingDisconnectingPacket();
        mc.loadWorld(null);
        mc.displayGuiScreen(new net.minecraft.client.gui.GuiMultiplayer(
                new net.minecraft.client.gui.GuiMainMenu()));
        ClientLogger.info("replay", "probe: disconnect returned without throwing");
    }

    /**
     * Feeds a dimension change into playback, the way a recording of someone walking through a
     * portal would. Vanilla's respawn handler rebuilds the world and replaces the player, which is
     * the case playback has to put itself back together after.
     */
    private static void maybeRespawn() {
        int at = Integer.getInteger("edge.replay.probeRespawnAt", -1).intValue();
        if (at < 0 || respawned || !ReplayPlayer.instance().isActive()
                || ReplayPlayer.instance().elapsedMillis() / 1000 < at) {
            return;
        }
        respawned = true;
        Minecraft mc = Minecraft.getMinecraft();
        ClientLogger.info("replay", "probe: injecting a dimension change");
        new net.minecraft.network.play.server.S07PacketRespawn(
                -1, net.minecraft.world.EnumDifficulty.NORMAL,
                net.minecraft.world.WorldType.DEFAULT,
                net.minecraft.world.WorldSettings.GameType.SURVIVAL)
                .processPacket(mc.getNetHandler());
        ClientLogger.info("replay", "probe: dimension change returned without throwing");
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
        sample.addProperty("dimension", Integer.valueOf(mc.thePlayer == null ? -99 : mc.thePlayer.dimension));
        sample.addProperty("gameType", mc.playerController == null ? "?" : String.valueOf(mc.playerController.getCurrentGameType()));
        sample.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName());
        if (mc.thePlayer != null && mc.thePlayer.openContainer != null) {
            sample.addProperty("containerWindow", Integer.valueOf(mc.thePlayer.openContainer.windowId));
            sample.addProperty("slot9", describeSlot(mc, 9));
            sample.addProperty("slot36", describeSlot(mc, 36));
        }
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

    private static String describeSlot(Minecraft mc, int slot) {
        if (slot >= mc.thePlayer.openContainer.inventorySlots.size()) {
            return "-";
        }
        net.minecraft.item.ItemStack stack = mc.thePlayer.openContainer.getSlot(slot).getStack();
        return stack == null ? "empty" : stack.getDisplayName();
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
