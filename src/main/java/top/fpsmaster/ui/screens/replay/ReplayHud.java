package top.fpsmaster.ui.screens.replay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.director.DirectorCamera;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.UiScale;

/**
 * The replay controls along the top of the screen.
 *
 * <p>While just watching a recording this is the transport row plus a seek bar. Once the edit
 * timeline is open the seek bar is dropped — that job belongs to {@link DirectorPanel} — and this
 * collapses to a small tooltip of pause / speed / view / export / files / stop.
 *
 * <p>Two things share the layout. The overlay draws it every frame so it stays readable while
 * flying, and {@link ReplayControlScreen} draws the same layout when the cursor is free so the same
 * controls can be clicked.
 *
 * <p>Laid out in the client's own scale rather than Minecraft's, so GUI Scale does not move the
 * hit targets.
 */
public final class ReplayHud {

    private static final float PANEL_MAX_WIDTH = 360f;
    private static final float PANEL_TOP = 9f;
    private static final float INSET = 8f;
    private static final float BUTTON_H = 15f;

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

    public static boolean contains(int mouseX, int mouseY) {
        float h = DirectorPanel.isOpen() ? 22f : 47f;
        return Hover.is(panelX, PANEL_TOP, panelWidth, h, mouseX, mouseY);
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
            EdgeUi.beginOverlay(guiWidth, guiHeight);
            try {
                // Far outside the panel, so nothing reads as hovered while the cursor is not there.
                render(player, guiWidth, null, -1000, -1000);
                if (DirectorPanel.isOpen()) {
                    DirectorPanel.draw(null, guiWidth, guiHeight, -1000, -1000);
                }
            } finally {
                EdgeUi.end();
            }
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
        if (DirectorPanel.isOpen()) {
            renderCompactToolbar(player, guiWidth, screen, mouseX, mouseY);
            return;
        }
        panelWidth = Math.min(PANEL_MAX_WIDTH, guiWidth - 24f);
        panelX = (guiWidth - panelWidth) / 2f;
        float panelHeight = 47f;
        barY = PANEL_TOP + 27.5f;

        UiChrome.panel(panelX, PANEL_TOP, panelWidth, panelHeight);

        // ---- row 1: clip name · transport buttons ----
        float rowY = PANEL_TOP + 6f;
        String clip = "";
        if (player.file() != null) {
            clip = player.file().getName();
            if (clip.endsWith(".edgereplay")) {
                clip = clip.substring(0, clip.length() - ".edgereplay".length());
            }
        }
        UiChrome.boldString(FPSMaster.fontManager.getFont(13), clip, panelX + INSET, rowY + 4f,
                ClickGuiTheme.textPrimary().getRGB());
        float clipW = FPSMaster.fontManager.getFont(13).getStringWidth(clip);
        if (player.recorderName() != null && !player.recorderName().isEmpty()) {
            FPSMaster.fontManager.getFont(11).drawString(
                    String.format(FPSMaster.i18n.get("replay.hud.by"), player.recorderName()),
                    panelX + INSET + clipW + 4f, rowY + 4.5f, ClickGuiTheme.textDisabled().getRGB());
        }

        float bx = panelX + panelWidth - INSET;
        bx -= BUTTON_H;
        boolean stopClicked = iconButton(screen, bx, rowY, "stop", true, mouseX, mouseY);
        bx -= 3f + BUTTON_H;
        boolean filesClicked = iconButton(screen, bx, rowY, "folder", false, mouseX, mouseY);
        bx -= 3f + BUTTON_H;
        boolean directorClicked = iconButton(screen, bx, rowY, "film", false, mouseX, mouseY);
        bx -= 3f + BUTTON_H;
        boolean viewClicked = iconButton(screen, bx, rowY, "eye", false, mouseX, mouseY);
        String speedText = formatSpeed(player.speed());
        float speedW = Math.max(22f, FPSMaster.fontManager.getFont(11).getStringWidth(speedText) + 8f);
        bx -= 3f + speedW;
        boolean speedHover = screen != null && Hover.is(bx, rowY, speedW, BUTTON_H, mouseX, mouseY);
        UiChrome.button(bx, rowY, speedW, BUTTON_H, speedHover);
        FPSMaster.fontManager.getFont(11).drawCenteredString(speedText, bx + speedW / 2f, rowY + 5f,
                ClickGuiTheme.textPrimary().getRGB());
        boolean speedClicked = screen != null && screen.consumePressInBounds(bx, rowY, speedW, BUTTON_H, 0) != null;
        bx -= 3f + BUTTON_H;
        boolean pauseClicked = iconButton(screen, bx, rowY, player.isPaused() ? "play" : "pause", false, mouseX, mouseY);

        if (pauseClicked) {
            togglePause();
        } else if (speedClicked) {
            cycleSpeed();
        } else if (viewClicked) {
            toggleView();
        } else if (directorClicked) {
            if (DirectorCamera.project() == null && player.isActive() && player.file() != null) {
                DirectorCamera.beginForReplay(player.file(), duration(player));
            }
            DirectorPanel.toggle();
        } else if (filesClicked) {
            openBrowser();
        } else if (stopClicked) {
            ReplayPlayer.instance().stop();
        }

        drawTimeline(player, screen, mouseX, mouseY);

        // ---- hint line ----
        String hint = FPSMaster.i18n.get(screen == null ? "replay.hud.hint.fly" : "replay.hud.hint");
        FPSMaster.fontManager.getFont(10).drawCenteredString(hint, panelX + panelWidth / 2f,
                PANEL_TOP + panelHeight - 8f, ClickGuiTheme.textDisabled().getRGB());
    }

    /** Editing: a small top tooltip of transport buttons. Seek lives on the docked timeline. */
    private static void renderCompactToolbar(ReplayPlayer player, float guiWidth, ScaledGuiScreen screen,
                                            int mouseX, int mouseY) {
        String clip = "";
        if (player.file() != null) {
            clip = player.file().getName();
            if (clip.endsWith(".edgereplay")) {
                clip = clip.substring(0, clip.length() - ".edgereplay".length());
            }
        }
        String speedText = formatSpeed(player.speed());
        float speedW = Math.max(22f, FPSMaster.fontManager.getFont(11).getStringWidth(speedText) + 8f);
        float nameW = Math.min(86f, FPSMaster.fontManager.getFont(12).getStringWidth(clip));
        float buttonsW = BUTTON_H * 5f + speedW + 3f * 5f;
        panelWidth = 10f + nameW + 8f + buttonsW + 8f;
        panelX = (guiWidth - panelWidth) / 2f;
        float panelHeight = 22f;
        UiChrome.panel(panelX, PANEL_TOP, panelWidth, panelHeight);

        float rowY = PANEL_TOP + 3.5f;
        if (nameW > 0f) {
            FPSMaster.fontManager.getFont(12).drawString(
                    FPSMaster.fontManager.getFont(12).trimStringToWidth(clip, nameW),
                    panelX + 6f, rowY + 4f, ClickGuiTheme.textPrimary().getRGB());
        }

        float bx = panelX + panelWidth - 6f;
        bx -= BUTTON_H;
        boolean stopClicked = iconButton(screen, bx, rowY, "stop", true, mouseX, mouseY);
        bx -= 3f + BUTTON_H;
        boolean filesClicked = iconButton(screen, bx, rowY, "folder", false, mouseX, mouseY);
        bx -= 3f + BUTTON_H;
        boolean directorClicked = iconButton(screen, bx, rowY, "film", false, mouseX, mouseY);
        bx -= 3f + BUTTON_H;
        boolean viewClicked = iconButton(screen, bx, rowY, "eye", false, mouseX, mouseY);
        bx -= 3f + speedW;
        boolean speedHover = screen != null && Hover.is(bx, rowY, speedW, BUTTON_H, mouseX, mouseY);
        UiChrome.button(bx, rowY, speedW, BUTTON_H, speedHover);
        FPSMaster.fontManager.getFont(11).drawCenteredString(speedText, bx + speedW / 2f, rowY + 5f,
                ClickGuiTheme.textPrimary().getRGB());
        boolean speedClicked = screen != null && screen.consumePressInBounds(bx, rowY, speedW, BUTTON_H, 0) != null;
        bx -= 3f + BUTTON_H;
        boolean pauseClicked = iconButton(screen, bx, rowY, player.isPaused() ? "play" : "pause", false, mouseX, mouseY);

        if (pauseClicked) {
            togglePause();
        } else if (speedClicked) {
            cycleSpeed();
        } else if (viewClicked) {
            toggleView();
        } else if (directorClicked) {
            DirectorPanel.toggle();
        } else if (filesClicked) {
            openBrowser();
        } else if (stopClicked) {
            ReplayPlayer.instance().stop();
        }
    }

    private static boolean iconButton(ScaledGuiScreen screen, float x, float y, String icon,
                                      boolean danger, int mouseX, int mouseY) {
        boolean hover = screen != null && Hover.is(x, y, BUTTON_H, BUTTON_H, mouseX, mouseY);
        if (danger) {
            UiChrome.dangerButton(x, y, BUTTON_H, BUTTON_H, hover);
        } else {
            UiChrome.button(x, y, BUTTON_H, BUTTON_H, hover);
        }
        int color = danger ? ClickGuiTheme.danger().getRGB()
                : (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB();
        Icons.draw(icon, x + 4.25f, y + 4.25f, 6.5f, color);
        return screen != null && screen.consumePressInBounds(x, y, BUTTON_H, BUTTON_H, 0) != null;
    }

    private static void drawTimeline(ReplayPlayer player, ScaledGuiScreen screen, int mouseX, int mouseY) {
        int duration = duration(player);

        String elapsed = ReplayScreen.formatDuration(scrubbing ? scrubMillis : player.elapsedMillis());
        String total = ReplayScreen.formatDuration(duration);
        float timeW = 16f;
        barX = panelX + INSET + timeW + 4f;
        barWidth = panelWidth - INSET * 2f - (timeW + 4f) * 2f;

        if (screen != null) {
            // A generous grab area: the bar is thin and the pointer should not have to be.
            if (screen.beginDrag(ReplayHud.class, barX, barY - 5f, barWidth, 12f)) {
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

        FPSMaster.fontManager.getFont(11).drawString(elapsed, panelX + INSET, barY - 2.5f,
                ClickGuiTheme.textSecondary().getRGB());
        Rects.rounded(barX, barY - 1.25f, barWidth, 2.5f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        if (progress > 0f) {
            Rects.rounded(barX, barY - 1.25f, barWidth * progress, 2.5f, 1, ClickGuiTheme.accent().getRGB(), false);
        }
        if (screen != null && (scrubbing || Hover.is(barX, barY - 5f, barWidth, 12f, mouseX, mouseY))) {
            Rects.rounded(barX + barWidth * progress - 3f, barY - 3f, 6f, 6f, 3, 0xFFFFFFFF, false);
        }
        float totalW = FPSMaster.fontManager.getFont(11).getStringWidth(total);
        FPSMaster.fontManager.getFont(11).drawString(total, panelX + panelWidth - INSET - totalW,
                barY - 2.5f, ClickGuiTheme.textSecondary().getRGB());

        if (player.isSeeking()) {
            FPSMaster.fontManager.getFont(11).drawCenteredString(
                    "seeking " + Math.round(player.seekProgress() * 100f) + "%",
                    barX + barWidth / 2f, barY + 5f, ClickGuiTheme.accentText().getRGB());
        }
    }

    /**
     * The duration only becomes known as the reader reaches the end, so early on the bar is measured
     * against what has been read so far rather than pretending to know the whole file.
     */
    static int duration(ReplayPlayer player) {
        return Math.max(player.durationMillis(), player.elapsedMillis());
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
        Minecraft.getMinecraft().displayGuiScreen(
                new ReplayScreen(null, DirectorCamera.project() != null));
    }
}
