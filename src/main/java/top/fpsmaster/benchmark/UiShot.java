package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import top.fpsmaster.FPSMaster;
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
        if (mc.currentScreen == null) {
            return;  // still starting up
        }
        long now = System.currentTimeMillis();
        if (firstTickMillis == 0L) {
            firstTickMillis = now;
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
        ClientLogger.info("uishot", "captured " + name + " on " + mc.currentScreen.getClass().getSimpleName()
                + " - s16 height " + FPSMaster.fontManager.s16.getHeight()
                + ", s16 width of 'Multiplayer' " + FPSMaster.fontManager.s16.getStringWidth("Multiplayer")
                + ", s24 height " + FPSMaster.fontManager.s24.getHeight());
        mc.shutdown();
    }
}
