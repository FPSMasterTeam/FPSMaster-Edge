package top.fpsmaster.ui.screens.replay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.Color;

/**
 * The strip of playback state that is always on screen, kept deliberately thin.
 *
 * <p>It costs whatever it costs on every frame of every replay, including the recordings the
 * benchmark measures, so this is the one piece that has to be cheap. The earlier version measured
 * 59-64us a frame for two rounded rectangles and three centred strings, both of which are more
 * expensive than they look.
 *
 * <p>A rounded rectangle is not a rectangle: {@code Rects.rounded} draws four corner textures plus
 * three fills, so a 3-pixel-tall bar with a 1-pixel radius was fourteen textured draws a frame for
 * rounding nobody can see at that size. Plain fills now.
 *
 * <p>A centred string is laid out twice, once to measure and once to draw, and these were long. The
 * two that were pure instruction have moved into {@link ReplayControlScreen}, which is where someone
 * goes to act on them. What is left is the clock, rebuilt only when the second it shows changes
 * rather than on each of the thirty frames that share one.
 */
public final class ReplayHud {

    private static final int BAR_WIDTH = 220;
    private static final int BAR_HEIGHT = 3;

    private static final int TRACK = new Color(255, 255, 255, 60).getRGB();
    private static final int FILL = new Color(113, 127, 254).getRGB();
    private static final int TEXT = new Color(235, 235, 235).getRGB();

    private static String cachedClock = "";
    private static int cachedSecond = -1;
    private static int cachedDurationSecond = -1;
    private static boolean cachedPaused;

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
        // Clear of the vanilla overlay when there is one. Possessing turns the hotbar, health and
        // hunger back on, and this used to sit straight on top of them.
        float y = resolution.getScaledHeight() - (player.isPossessing() ? 78f : 46f);
        float barX = centerX - BAR_WIDTH / 2f;

        int duration = duration(player);
        float progress = duration <= 0 ? 0f
                : Math.min(1f, player.elapsedMillis() / (float) duration);

        Rects.fill(barX, y, BAR_WIDTH, BAR_HEIGHT, TRACK);
        Rects.fill(barX, y, BAR_WIDTH * progress, BAR_HEIGHT, FILL);
        FPSMaster.fontManager.s16.drawCenteredString(clock(player, duration), centerX, y + 7f, TEXT);
    }

    /**
     * The duration only becomes known as the reader reaches the end, so early on the bar is measured
     * against what has been read so far rather than pretending to know the whole file.
     */
    static int duration(ReplayPlayer player) {
        return Math.max(player.durationMillis(), player.elapsedMillis());
    }

    private static String clock(ReplayPlayer player, int duration) {
        int second = player.elapsedMillis() / 1000;
        int durationSecond = duration / 1000;
        boolean paused = player.isPaused();
        if (second != cachedSecond || durationSecond != cachedDurationSecond || paused != cachedPaused) {
            cachedSecond = second;
            cachedDurationSecond = durationSecond;
            cachedPaused = paused;
            cachedClock = ReplayScreen.formatDuration(player.elapsedMillis()) + " / "
                    + ReplayScreen.formatDuration(duration) + (paused ? "  (paused)" : "");
        }
        return cachedClock;
    }
}
