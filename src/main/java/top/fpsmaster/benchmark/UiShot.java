package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Screenshots the client's own interface and exits.
 *
 * <p>The scenario screenshots go through the benchmark runner, which needs a world, a player and a
 * camera path — so every gate in this project has been pointed at a scene with no client-drawn text
 * in it. A change to the font renderer could double the size of every string in the interface and
 * still pass, which is exactly what happened. This captures the screens that are nothing but
 * client-drawn chrome and text.
 *
 * <pre>
 *   -Dedge.uishot=6                     capture six seconds after each screen opens, then quit
 *   -Dedge.uishot.name=before           file name stem to write
 *   -Dedge.uishot.screen=clickgui       screen to open first ('' = main menu)
 *   -Dedge.uishot.screen=mainmenu,clickgui,music   several screens in one launch;
 *                                       each is written as &lt;name&gt;-&lt;screen&gt;.png
 * </pre>
 *
 * Known screens: mainmenu, clickgui, music, replay, multiplayer, oobe, configprofiles,
 * bgselector, language.
 */
public final class UiShot {

    private static final int DELAY_SECONDS = Integer.getInteger("edge.uishot", -1).intValue();

    private static List<String> screens;
    private static int screenIndex = -1;
    private static long screenOpenedMillis;
    private static long lastHeartbeatMillis;
    private static boolean done;

    private UiShot() {
    }

    public static void onClientTick() {
        if (DELAY_SECONDS < 0 || done) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        long heartbeatNow = System.currentTimeMillis();
        if (heartbeatNow - lastHeartbeatMillis >= 5000L) {
            lastHeartbeatMillis = heartbeatNow;
            ClientLogger.info("uishot", "tick: screen="
                    + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName())
                    + " world=" + (mc.theWorld != null)
                    + " display=" + mc.displayWidth + "x" + mc.displayHeight
                    + " lwjgl=" + org.lwjgl.opengl.Display.getWidth() + "x" + org.lwjgl.opengl.Display.getHeight()
                    + " pixelScale=" + org.lwjgl.opengl.Display.getPixelScaleFactor()
                    + " guiScale=" + mc.gameSettings.guiScale
                    + " srFactor=" + new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor()
                    + " uiScale=" + top.fpsmaster.features.impl.interfaces.ClientSettings.getUiScale()
                    + " follow=" + top.fpsmaster.features.impl.interfaces.ClientSettings.isFollowGameScaleEnabled());
        }
        if (mc.currentScreen == null && mc.theWorld == null) {
            return;  // still starting up
        }
        long now = System.currentTimeMillis();
        if (screens == null) {
            screens = new ArrayList<String>();
            for (String part : System.getProperty("edge.uishot.screen", "").split(",")) {
                screens.add(part.trim());
            }
            stressGlyphs();
            openNext();
            return;
        }
        if (now - screenOpenedMillis < DELAY_SECONDS * 1000L) {
            return;
        }
        capture();
        openNext();
    }

    private static void openNext() {
        Minecraft mc = Minecraft.getMinecraft();
        screenIndex++;
        if (screenIndex >= screens.size()) {
            done = true;
            mc.shutdown();
            return;
        }
        String screen = screens.get(screenIndex);
        try {
            display(screen);
        } catch (RuntimeException exception) {
            ClientLogger.error("uishot", "failed to open screen '" + screen + "': " + exception);
        }
        screenOpenedMillis = System.currentTimeMillis();
    }

    private static void display(String screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if ("clickgui".equals(screen)) {
            mc.displayGuiScreen(FPSMaster.moduleManager.mainPanel);
        } else if ("language".equals(screen)) {
            // Vanilla's own font on a screen that never moves, and whose entries span both
            // the bitmap page and the unicode pages - the two paths a font change can break.
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiLanguage(
                    null, mc.gameSettings, mc.getLanguageManager()));
        } else if ("replay".equals(screen)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.replay.ReplayScreen(null));
        } else if ("multiplayer".equals(screen)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.mc.GuiMultiplayer());
        } else if ("music".equals(screen)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.music.MusicScreen());
        } else if ("oobe".equals(screen)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.oobe.OobeScreen());
        } else if ("configprofiles".equals(screen)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.click.ConfigProfilesScreen(null));
        } else if ("bgselector".equals(screen)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.mainmenu.BackgroundSelector());
        } else if ("mainmenu".equals(screen) || screen.isEmpty()) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.mainmenu.MainMenu());
        } else {
            ClientLogger.error("uishot", "unknown screen '" + screen + "', capturing current screen");
        }
    }

    private static void capture() {
        Minecraft mc = Minecraft.getMinecraft();
        File directory = new File(mc.mcDataDir, "bench-results");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            ClientLogger.error("uishot", "could not create " + directory);
            return;
        }
        String name = System.getProperty("edge.uishot.name", "ui");
        String screen = screens.get(screenIndex);
        String file = screens.size() > 1
                ? name + "-" + (screen.isEmpty() ? "mainmenu" : screen) + ".png"
                : name + ".png";
        ScreenShotHelper.saveScreenshot(directory, file,
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        // Report a measurement alongside the image, so a size regression is a number and not a
        // judgement about a picture.
        ClientLogger.info("uishot", "captured " + file + " on "
                + (mc.currentScreen == null ? "in-world HUD" : mc.currentScreen.getClass().getSimpleName())
                + " - s16 height " + FPSMaster.fontManager.s16.getHeight()
                + ", s16 width of 'Multiplayer' " + FPSMaster.fontManager.s16.getStringWidth("Multiplayer")
                + ", s24 height " + FPSMaster.fontManager.s24.getHeight());
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
