package top.fpsmaster.ui.screens.replay;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;
import java.io.IOException;

/**
 * The controls for a running replay, on a screen rather than in the overlay.
 *
 * <p>Everything here needs a cursor, and a cursor and a free camera cannot both have the mouse. So
 * the overlay keeps the part that has to be visible while flying — where you are in the recording —
 * and this holds the part you stop to use. Escape opens it in place of the vanilla pause menu, which
 * is where a player already goes to reach for controls and which does nothing useful during
 * playback anyway.
 *
 * <p>Built on {@link ScaledGuiScreen} so it is laid out in the client's own scale rather than
 * Minecraft's. That matters more here than elsewhere: the recordings are watched at whatever GUI
 * scale the session was left at, and a scrubber that changes size with it is a scrubber whose drag
 * targets move.
 */
public class ReplayControlScreen extends ScaledGuiScreen {

    private static final float PANEL_WIDTH = 460f;
    private static final float PANEL_HEIGHT = 132f;
    private static final float BAR_HEIGHT = 6f;
    private static final float KNOB_RADIUS = 5f;

    private static final Color PANEL = new Color(0, 0, 0, 165);
    private static final Color TRACK = new Color(255, 255, 255, 45);
    private static final Color FILL = new Color(113, 127, 254);
    private static final Color KNOB = new Color(226, 229, 255);
    private static final Color SUBTLE = new Color(185, 185, 185);
    private static final Color HINT = new Color(140, 140, 140);

    private final GuiButton pauseButton;
    private final GuiButton speedButton;
    private final GuiButton viewButton;
    private final GuiButton filesButton;
    private final GuiButton stopButton;

    /** Set while the knob is being dragged, so the bar shows the drag rather than the clock. */
    private boolean scrubbing;
    private int scrubMillis;

    public ReplayControlScreen() {
        this.pauseButton = new GuiButton("Pause", this::togglePause).setText("Pause", false);
        this.speedButton = new GuiButton("Speed", this::cycleSpeed).setText("1x", false);
        this.viewButton = new GuiButton("View", this::toggleView).setText("View", false);
        this.filesButton = new GuiButton("Recordings", () -> mc.displayGuiScreen(new ReplayScreen(null)))
                .setText("Recordings", false);
        this.stopButton = new GuiButton("Stop", () -> ReplayPlayer.instance().stop())
                .setText("Stop", false);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            // The recording ended underneath the screen, or something else stopped it. Its own
            // teardown puts the browser up; staying here would leave controls for nothing.
            mc.displayGuiScreen(null);
            return;
        }

        float panelX = (guiWidth - PANEL_WIDTH) / 2f;
        float panelY = guiHeight - PANEL_HEIGHT - 24f;
        Rects.rounded(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 6, PANEL.getRGB());

        FPSMaster.fontManager.s18.drawString(player.file() == null ? "Replay" : player.file().getName(),
                panelX + 18f, panelY + 16f, Color.WHITE.getRGB());
        FPSMaster.fontManager.s16.drawString("recorded by " + player.recorderName(),
                panelX + 18f, panelY + 32f, SUBTLE.getRGB());

        float barX = panelX + 18f;
        float barWidth = PANEL_WIDTH - 36f;
        float barY = panelY + 56f;
        drawScrubber(player, barX, barY, barWidth, mouseX, mouseY);

        float buttonY = panelY + PANEL_HEIGHT - 40f;
        float buttonWidth = (PANEL_WIDTH - 36f - 4f * 8f) / 5f;
        pauseButton.setText(player.isPaused() ? "Resume" : "Pause", false);
        speedButton.setText(formatSpeed(player.speed()), false);
        viewButton.setText(player.isPossessing() ? "Free camera" : "Watch " + player.recorderName(), false);
        pauseButton.renderInScreen(this, barX, buttonY, buttonWidth, 24f, mouseX, mouseY);
        speedButton.renderInScreen(this, barX + (buttonWidth + 8f), buttonY, buttonWidth, 24f, mouseX, mouseY);
        viewButton.renderInScreen(this, barX + 2f * (buttonWidth + 8f), buttonY, buttonWidth, 24f, mouseX, mouseY);
        filesButton.renderInScreen(this, barX + 3f * (buttonWidth + 8f), buttonY, buttonWidth, 24f, mouseX, mouseY);
        stopButton.renderInScreen(this, barX + 4f * (buttonWidth + 8f), buttonY, buttonWidth, 24f, mouseX, mouseY);

        FPSMaster.fontManager.s16.drawCenteredString("Esc to close  -  P to pause  -  drag the bar to seek",
                guiWidth / 2f, panelY + PANEL_HEIGHT + 8f, HINT.getRGB());
    }

    /**
     * The bar, and the drag on it.
     *
     * <p>The knob follows the pointer for the whole drag and the seek only happens on release. A
     * seek is not free — backwards it rebuilds the world from the start of the file — so running one
     * per frame of a drag across the bar would be dozens of rebuilds to reach one destination.
     */
    private void drawScrubber(ReplayPlayer player, float barX, float barY, float barWidth,
                              int mouseX, int mouseY) {
        int duration = ReplayHud.duration(player);

        // A generous grab area: the bar is six pixels tall and the pointer should not have to be.
        boolean grabbed = beginDrag(this, barX, barY - 8f, barWidth, BAR_HEIGHT + 16f);
        if (grabbed) {
            scrubbing = true;
        }
        if (scrubbing) {
            float fraction = barWidth <= 0f ? 0f
                    : Math.max(0f, Math.min(1f, (mouseX - barX) / barWidth));
            scrubMillis = Math.round(duration * fraction);
            if (!isDragging(this)) {
                scrubbing = false;
                releaseDrag(this);
                player.seek(scrubMillis);
            }
        }

        int shown = scrubbing ? scrubMillis : player.elapsedMillis();
        float progress = duration <= 0 ? 0f : Math.max(0f, Math.min(1f, shown / (float) duration));

        Rects.rounded(barX, barY, barWidth, BAR_HEIGHT, 3, TRACK.getRGB());
        Rects.rounded(barX, barY, barWidth * progress, BAR_HEIGHT, 3, FILL.getRGB());
        if (scrubbing || Hover.is(barX, barY - 8f, barWidth, BAR_HEIGHT + 16f, mouseX, mouseY)) {
            Rects.rounded(barX + barWidth * progress - KNOB_RADIUS, barY + BAR_HEIGHT / 2f - KNOB_RADIUS,
                    KNOB_RADIUS * 2f, KNOB_RADIUS * 2f, (int) KNOB_RADIUS, KNOB.getRGB());
        }

        FPSMaster.fontManager.s16.drawString(ReplayScreen.formatDuration(shown),
                barX, barY + 14f, SUBTLE.getRGB());
        String total = ReplayScreen.formatDuration(duration);
        FPSMaster.fontManager.s16.drawString(total,
                barX + barWidth - FPSMaster.fontManager.s16.getStringWidth(total), barY + 14f,
                SUBTLE.getRGB());

        if (player.isSeeking()) {
            // A seek runs a slice per tick rather than all at once, so it has a middle to show.
            FPSMaster.fontManager.s16.drawCenteredString(
                    "seeking  " + Math.round(player.seekProgress() * 100f) + "%",
                    barX + barWidth / 2f, barY + 14f, FILL.getRGB());
        }
    }

    private void togglePause() {
        ReplayPlayer.instance().togglePause();
    }

    /**
     * Steps to the next rate and wraps.
     *
     * <p>A cycle rather than a slider: the useful rates are a handful of powers of two and the
     * points between them are not worth aiming at.
     */
    private void cycleSpeed() {
        ReplayPlayer player = ReplayPlayer.instance();
        float current = player.speed();
        for (int i = 0; i < ReplayPlayer.SPEEDS.length; i++) {
            if (ReplayPlayer.SPEEDS[i] == current) {
                player.setSpeed(ReplayPlayer.SPEEDS[(i + 1) % ReplayPlayer.SPEEDS.length]);
                return;
            }
        }
        player.setSpeed(1.0f);
    }

    /** "0.25x" reads better than "0.25000001x", and "2x" better than "2.0x". */
    static String formatSpeed(float speed) {
        if (speed == Math.round(speed)) {
            return Math.round(speed) + "x";
        }
        return String.valueOf(speed) + "x";
    }

    /**
     * Possession is the recorder's own eyes and it is only available once their avatar exists, which
     * is as soon as the movement track has produced one. Leaving it is always available.
     */
    private void toggleView() {
        ReplayPlayer player = ReplayPlayer.instance();
        if (player.isPossessing()) {
            player.release();
        } else if (player.hasAvatar()) {
            player.possess();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_P) {
            togglePause();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** The world carries on behind this, which is the point of scrubbing with it open. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
