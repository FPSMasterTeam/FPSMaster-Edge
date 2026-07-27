package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;

/**
 * Screenshots the client's own interface and exits.
 *
 * <p>The scenario screenshots go through the benchmark runner, which needs a world, a player and a
 * camera path — so every gate in this project has been pointed at a scene with no client-drawn text
 * in it. A change to the font renderer could double the size of every string in the interface and
 * still pass, which is exactly what happened. This captures the one screen that is nothing but
 * client text.
 *
 * <pre>
 *   -Dedge.uishot=6            capture the main menu six seconds in, then quit
 *   -Dedge.uishot.name=before  file name to write
 *   -Dedge.uishot.screen=clickgui  open the click GUI first
 * </pre>
 */
public final class UiShot {

    private static final int DELAY_SECONDS = Integer.getInteger("edge.uishot", -1).intValue();

    private static long firstTickMillis;
    private static boolean captured;

    private UiShot() {
    }

    public static void onClientTick() {
        if (DELAY_SECONDS < 0 || captured) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null && mc.theWorld == null) {
            return;  // still starting up
        }
        long now = System.currentTimeMillis();
        if (firstTickMillis == 0L) {
            firstTickMillis = now;
            // The click GUI is the densest text in the client - hundreds of distinct characters,
            // enough to make the glyph atlas grow - so it is the screen worth pointing this at.
            String screen = System.getProperty("edge.uishot.screen", "");
            if ("clickgui".equals(screen)) {
                mc.displayGuiScreen(FPSMaster.moduleManager.mainPanel);
            } else if ("replay".equals(screen)) {
                mc.displayGuiScreen(new top.fpsmaster.ui.screens.replay.ReplayScreen(null));
            }
            stressGlyphs();
            return;
        }
        if (now - firstTickMillis < DELAY_SECONDS * 1000L) {
            return;
        }
        captured = true;

        File directory = new File(mc.mcDataDir, "bench-results");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            ClientLogger.error("uishot", "could not create " + directory);
            return;
        }
        String name = System.getProperty("edge.uishot.name", "ui");
        ScreenShotHelper.saveScreenshot(directory, name + ".png",
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        // Report a measurement alongside the image, so a size regression is a number and not a
        // judgement about a picture.
        ClientLogger.info("uishot", "captured " + name + " on "
                + (mc.currentScreen == null ? "in-world HUD" : mc.currentScreen.getClass().getSimpleName())
                + " - s16 height " + FPSMaster.fontManager.s16.getHeight()
                + ", s16 width of 'Multiplayer' " + FPSMaster.fontManager.s16.getStringWidth("Multiplayer")
                + ", s24 height " + FPSMaster.fontManager.s24.getHeight());

        // HUD components live in a space defined by the vanilla gui scale; the draggable area has to
        // match the reach of the mouse in that same space or part of the screen becomes unreachable.
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        float mouseReachX = sr.getScaledWidth() * sr.getScaleFactor() / 2f;
        float mouseReachY = sr.getScaledHeight() * sr.getScaleFactor() / 2f;
        float boundsX = sr.getScaledWidth() / 2f * sr.getScaleFactor();
        float boundsY = sr.getScaledHeight() / 2f * sr.getScaleFactor();
        float oldBoundsX = (float) (sr.getScaledWidth() / 2f * ClientSettings.getUiScale());
        ClientLogger.info("uishot", "hud space: mouse reaches " + mouseReachX + "x" + mouseReachY
                + ", drag bounds " + boundsX + "x" + boundsY
                + " (previously " + oldBoundsX + " wide, uiScale " + ClientSettings.getUiScale()
                + ", vanilla scale " + sr.getScaleFactor() + ")");
        mc.shutdown();
    }

    /**
     * Fills the glyph atlas with distinct characters so it has to grow before anything is drawn.
     *
     * <p>A freshly started client never gets near that; one that has been showing Chinese text for a
     * while does. Growth throws away every cached glyph, and getting that wrong stays invisible
     * until a string happens to be laid out across the moment it happens.
     */
    private static void stressGlyphs() {
        int count = Integer.getInteger("edge.uishot.stress", 0).intValue();
        if (count <= 0) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < count; index++) {
            text.append((char) (0x4E00 + index));
            if (text.length() >= 64) {
                FPSMaster.fontManager.s16.getStringWidth(text.toString());
                FPSMaster.fontManager.s18.getStringWidth(text.toString());
                text.setLength(0);
            }
        }
        ClientLogger.info("uishot", "stressed the atlas with " + count + " distinct characters");
    }
}
