package top.fpsmaster.ui.screens.replay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.Color;

/** Playback state and the two controls worth remembering, drawn only while a replay is running. */
public final class ReplayHud {

    private static final int BAR_WIDTH = 220;
    private static final int BAR_HEIGHT = 3;

    private ReplayHud() {
    }

    public static void draw() {
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        float centerX = resolution.getScaledWidth() / 2f;
        float y = resolution.getScaledHeight() - 46f;

        float barX = centerX - BAR_WIDTH / 2f;
        Rects.rounded(Math.round(barX), Math.round(y), BAR_WIDTH, BAR_HEIGHT, 1,
                new Color(255, 255, 255, 60).getRGB());
        // The duration only becomes known as the reader reaches the end, so early on the bar is
        // measured against what has been read so far rather than pretending to know the whole file.
        int duration = Math.max(player.durationMillis(), player.elapsedMillis());
        float progress = duration <= 0 ? 0f : Math.min(1f, player.elapsedMillis() / (float) duration);
        Rects.rounded(Math.round(barX), Math.round(y), Math.round(BAR_WIDTH * progress), BAR_HEIGHT, 1,
                new Color(113, 127, 254).getRGB());

        String time = ReplayScreen.formatDuration(player.elapsedMillis()) + " / "
                + ReplayScreen.formatDuration(duration)
                + (player.isPaused() ? "  (paused)" : "");
        FPSMaster.fontManager.s16.drawCenteredString(time, centerX, y + 7f, Color.WHITE.getRGB());

        String hint = player.isPossessing()
                ? "Sneak to leave " + player.recorderName()
                : "Look at " + player.recorderName() + " and attack to watch from their eyes";
        FPSMaster.fontManager.s16.drawCenteredString(hint, centerX, y + 17f,
                new Color(190, 190, 190).getRGB());
    }
}
