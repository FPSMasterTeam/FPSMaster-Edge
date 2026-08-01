package top.fpsmaster.ui.screens.replay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.UiScale;

import java.awt.Color;

/**
 * The replay controls, along the top of the screen and always on it.
 *
 * <p>Two things share this. The overlay draws it every frame so the timeline is readable while
 * flying, and {@link ReplayControlScreen} draws the same layout when the cursor is free so the same
 * controls can be clicked. One layout function rather than two, because a control that is drawn in
 * one place and hit-tested from another is a control that will eventually disagree with itself.
 *
 * <p>Laid out in the client's own scale rather than Minecraft's. The overlay is normally drawn in
 * whatever the game's GUI Scale setting says, which would make the timeline change size — and with
 * it every drag target on it — when that setting moves. Here the transform is applied by hand so
 * both paths land in the same coordinates whichever way they were reached.
 */
public final class ReplayHud {

    private static final float PANEL_MAX_WIDTH = 760f;
    private static final float PANEL_HEIGHT = 66f;
    private static final float PANEL_TOP = 10f;
    private static final float INSET = 14f;
    private static final float BUTTON_HEIGHT = 22f;
    private static final float BUTTON_GAP = 6f;
    private static final float BAR_HEIGHT = 6f;
    private static final float KNOB = 5f;

    private static final int PANEL = new Color(0, 0, 0, 150).getRGB();
    private static final int TRACK = new Color(255, 255, 255, 45).getRGB();
    private static final int FILL = new Color(113, 127, 254).getRGB();
    private static final int KNOB_COLOR = new Color(226, 229, 255).getRGB();
    private static final int TEXT = new Color(235, 235, 235).getRGB();
    private static final int SUBTLE = new Color(185, 185, 185).getRGB();
    private static final int HINT = new Color(140, 140, 140).getRGB();

    private static final GuiButton PAUSE = new GuiButton("Pause", ReplayHud::togglePause).setText("Pause", false);
    private static final GuiButton SPEED = new GuiButton("Speed", ReplayHud::cycleSpeed).setText("1x", false);
    private static final GuiButton VIEW = new GuiButton("View", ReplayHud::toggleView).setText("View", false);
    private static final GuiButton FILES = new GuiButton("Recordings", ReplayHud::openBrowser).setText("Recordings", false);
    private static final GuiButton STOP = new GuiButton("Stop", () -> ReplayPlayer.instance().stop()).setText("Stop", false);
    private static final GuiButton[] BUTTONS = {PAUSE, SPEED, VIEW, FILES, STOP};

    private static float panelX;
    private static float panelWidth;
    private static float barX;
    private static float barY;
    private static float barWidth;

    private static boolean scrubbing;
    private static int scrubMillis;

    /** The clock is rebuilt on the second it shows, not on each of the thirty frames sharing one. */
    private static String cachedClock = "";
    private static int cachedSecond = -1;
    private static int cachedDurationSecond = -1;

    private ReplayHud() {
    }

    /** Overlay path: draw it, do not react to anything. The cursor belongs to the camera here. */
    public static void draw() {
        ReplayPlayer player = ReplayPlayer.instance();
        Minecraft mc = Minecraft.getMinecraft();
        if (!player.isActive() || mc.currentScreen instanceof ReplayControlScreen) {
            // Tested on the open screen rather than the one currently rendering: the overlay is
            // drawn before any screen is, so asking which is rendering would always answer none and
            // the bar would be drawn twice.
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        float vanillaScale = Math.max(1, resolution.getScaleFactor());
        float layoutScale = (float) ClientSettings.getUiScale();
        if (layoutScale <= 0f) {
            layoutScale = 1f;
        }
        float renderScale = layoutScale / vanillaScale;
        float guiWidth = mc.displayWidth / layoutScale;
        float guiHeight = mc.displayHeight / layoutScale;

        UiScale.begin(layoutScale, vanillaScale, guiWidth, guiHeight, mc.displayWidth, mc.displayHeight);
        GL11.glPushMatrix();
        try {
            GL11.glScalef(renderScale, renderScale, 1f);
            // Far outside the panel, so nothing reads as hovered while the cursor is not there.
            render(player, guiWidth, null, -1000, -1000);
        } finally {
            GL11.glPopMatrix();
            UiScale.end();
        }
    }

    /** Screen path: same bar, hit-tested and draggable. Called inside the screen's own transform. */
    static void drawInteractive(ScaledGuiScreen screen, float guiWidth, int mouseX, int mouseY) {
        render(ReplayPlayer.instance(), guiWidth, screen, mouseX, mouseY);
    }

    private static void render(ReplayPlayer player, float guiWidth, ScaledGuiScreen screen,
                               int mouseX, int mouseY) {
        panelWidth = Math.min(PANEL_MAX_WIDTH, guiWidth - 32f);
        panelX = (guiWidth - panelWidth) / 2f;
        barX = panelX + INSET;
        barWidth = panelWidth - INSET * 2f;
        barY = PANEL_TOP + 40f;

        Rects.rounded(panelX, PANEL_TOP, panelWidth, PANEL_HEIGHT, 6, PANEL);

        float buttonWidth = (barWidth - BUTTON_GAP * (BUTTONS.length - 1)) / BUTTONS.length;
        PAUSE.setText(player.isPaused() ? "Resume" : "Pause", false);
        SPEED.setText(formatSpeed(player.speed()), false);
        VIEW.setText(player.isPossessing() ? "Free camera" : "Watch " + player.recorderName(), false);
        for (int i = 0; i < BUTTONS.length; i++) {
            float x = barX + i * (buttonWidth + BUTTON_GAP);
            if (screen == null) {
                BUTTONS[i].render(x, PANEL_TOP + 10f, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY);
            } else {
                BUTTONS[i].renderInScreen(screen, x, PANEL_TOP + 10f, buttonWidth, BUTTON_HEIGHT,
                        mouseX, mouseY);
            }
        }

        drawTimeline(player, screen, mouseX, mouseY);
    }

    private static void drawTimeline(ReplayPlayer player, ScaledGuiScreen screen, int mouseX, int mouseY) {
        int duration = duration(player);

        if (screen != null) {
            // A generous grab area: the bar is six pixels tall and the pointer should not have to be.
            if (screen.beginDrag(ReplayHud.class, barX, barY - 8f, barWidth, BAR_HEIGHT + 16f)) {
                scrubbing = true;
            }
            if (scrubbing) {
                float fraction = barWidth <= 0f ? 0f
                        : Math.max(0f, Math.min(1f, (mouseX - barX) / barWidth));
                scrubMillis = Math.round(duration * fraction);
                if (!screen.isDragging(ReplayHud.class)) {
                    // Only on release. A seek backwards rebuilds the world from the start of the
                    // file, so one per frame of a drag across the bar is dozens of rebuilds to reach
                    // one destination.
                    scrubbing = false;
                    screen.releaseDrag(ReplayHud.class);
                    player.seek(scrubMillis);
                }
            }
        } else {
            scrubbing = false;
        }

        int shown = scrubbing ? scrubMillis : player.elapsedMillis();
        float progress = duration <= 0 ? 0f : Math.max(0f, Math.min(1f, shown / (float) duration));

        Rects.fill(barX, barY, barWidth, BAR_HEIGHT, TRACK);
        Rects.fill(barX, barY, barWidth * progress, BAR_HEIGHT, FILL);
        if (screen != null
                && (scrubbing || Hover.is(barX, barY - 8f, barWidth, BAR_HEIGHT + 16f, mouseX, mouseY))) {
            Rects.rounded(barX + barWidth * progress - KNOB, barY + BAR_HEIGHT / 2f - KNOB,
                    KNOB * 2f, KNOB * 2f, (int) KNOB, KNOB_COLOR);
        }

        FPSMaster.fontManager.s16.drawString(clock(shown, duration), barX, barY + 11f, TEXT);
        if (player.isSeeking()) {
            FPSMaster.fontManager.s16.drawCenteredString(
                    "seeking  " + Math.round(player.seekProgress() * 100f) + "%",
                    barX + barWidth / 2f, barY + 11f, FILL);
        } else if (player.isPaused()) {
            FPSMaster.fontManager.s16.drawCenteredString("paused", barX + barWidth / 2f, barY + 11f,
                    SUBTLE);
        }
        String hint = screen == null ? "Esc for the cursor" : "Esc to fly";
        FPSMaster.fontManager.s16.drawString(hint,
                barX + barWidth - FPSMaster.fontManager.s16.getStringWidth(hint), barY + 11f, HINT);
    }

    /**
     * The duration only becomes known as the reader reaches the end, so early on the bar is measured
     * against what has been read so far rather than pretending to know the whole file.
     */
    static int duration(ReplayPlayer player) {
        return Math.max(player.durationMillis(), player.elapsedMillis());
    }

    private static String clock(int shown, int duration) {
        int second = shown / 1000;
        int durationSecond = duration / 1000;
        if (second != cachedSecond || durationSecond != cachedDurationSecond) {
            cachedSecond = second;
            cachedDurationSecond = durationSecond;
            cachedClock = ReplayScreen.formatDuration(shown) + " / " + ReplayScreen.formatDuration(duration);
        }
        return cachedClock;
    }

    /** "0.25x" reads better than "0.25000001x", and "2x" better than "2.0x". */
    static String formatSpeed(float speed) {
        return speed == Math.round(speed) ? Math.round(speed) + "x" : speed + "x";
    }

    private static void togglePause() {
        ReplayPlayer.instance().togglePause();
    }

    /**
     * Steps to the next rate and wraps. A cycle rather than a slider: the useful rates are a handful
     * of powers of two and the points between them are not worth aiming at.
     */
    private static void cycleSpeed() {
        ReplayPlayer player = ReplayPlayer.instance();
        for (int i = 0; i < ReplayPlayer.SPEEDS.length; i++) {
            if (ReplayPlayer.SPEEDS[i] == player.speed()) {
                player.setSpeed(ReplayPlayer.SPEEDS[(i + 1) % ReplayPlayer.SPEEDS.length]);
                return;
            }
        }
        player.setSpeed(1.0f);
    }

    /**
     * Possession is the recorder's own eyes, and is only available once their avatar exists — which
     * is as soon as the movement track has produced one. Leaving it is always available.
     */
    private static void toggleView() {
        ReplayPlayer player = ReplayPlayer.instance();
        if (player.isPossessing()) {
            player.release();
        } else if (player.hasAvatar()) {
            player.possess();
        }
    }

    private static void openBrowser() {
        Minecraft.getMinecraft().displayGuiScreen(new ReplayScreen(null));
    }
}
