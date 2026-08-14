package top.fpsmaster.ui.screens.replay;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.director.CameraKeyframe;
import top.fpsmaster.replay.director.CameraPose;
import top.fpsmaster.replay.director.CameraTrack;
import top.fpsmaster.replay.director.DirectorCamera;
import top.fpsmaster.replay.director.DirectorExporter;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

/**
 * The director workbench: a bottom panel over the replay control screen with the keyframe
 * timeline, per-keyframe transition/easing editing, motion preview and mp4 export.
 *
 * <p>Pure chrome in the edge-ui language; all state lives in {@link DirectorCamera} /
 * {@link DirectorExporter} so closing and reopening the screen loses nothing.
 */
public final class DirectorPanel {

    /** Workbench visibility, toggled from the replay HUD's film button. */
    private static boolean open;

    private static CameraKeyframe selected;
    private static boolean draggingKeyframe;
    private static int exportFps = 60;

    private DirectorPanel() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void toggle() {
        open = !open;
    }

    /** Whether the workbench (or the export overlay) currently wants the whole screen. */
    public static void draw(ScaledGuiScreen screen, float guiWidth, float guiHeight, int mouseX, int mouseY) {
        if (DirectorExporter.isRunning()) {
            drawExportOverlay(screen, guiWidth, guiHeight, mouseX, mouseY);
            return;
        }
        if (!open) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        CameraTrack track = DirectorCamera.track();
        if (!track.keyframes.contains(selected)) {
            selected = null;
        }

        float w = Math.min(480f, guiWidth - 24f);
        float x = (guiWidth - w) / 2f;
        boolean editing = selected != null;
        float h = editing ? 78f : 60f;
        float y = guiHeight - 12f - h;
        UiChrome.panel(x, y, w, h);

        // ---- header ----
        UiChrome.boldString(FPSMaster.fontManager.s14, FPSMaster.i18n.get("director.title"),
                x + 8f, y + 6f, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(11).drawString(
                String.format(FPSMaster.i18n.get("director.keyframes"), track.keyframes.size()),
                x + 8f + FPSMaster.fontManager.s14.getStringWidth(FPSMaster.i18n.get("director.title")) + 5f,
                y + 7.5f, ClickGuiTheme.textDisabled().getRGB());

        // preview toggle + export on the right
        float right = x + w - 8f;
        String exportLabel = FPSMaster.i18n.get("director.export");
        float exportW = FPSMaster.fontManager.getFont(12).getStringWidth(exportLabel) + 24f;
        right -= exportW;
        if (buttonSmall(screen, right, y + 4.5f, exportW, 14f, "film", exportLabel,
                !track.isEmpty(), mouseX, mouseY)) {
            DirectorExporter.acknowledge();
            DirectorExporter.start(exportFps);
        }
        // fps selector
        right -= 4f;
        int[] rates = {24, 30, 60};
        for (int i = rates.length - 1; i >= 0; i--) {
            String label = rates[i] + "";
            float fw = FPSMaster.fontManager.getFont(11).getStringWidth(label) + 8f;
            right -= fw + 2f;
            boolean on = exportFps == rates[i];
            boolean hover = Hover.is(right, y + 5f, fw, 13f, mouseX, mouseY);
            if (on) {
                Rects.rounded(right, y + 5f, fw, 13f, 4, ClickGuiTheme.accentSoft().getRGB(), false);
            } else if (hover) {
                Rects.rounded(right, y + 5f, fw, 13f, 4, ClickGuiTheme.layerHover().getRGB(), false);
            }
            FPSMaster.fontManager.getFont(11).drawCenteredString(label, right + fw / 2f, y + 8.5f,
                    (on ? ClickGuiTheme.accentText() : ClickGuiTheme.textSecondary()).getRGB());
            if (screen.consumePressInBounds(right, y + 5f, fw, 13f, 0) != null) {
                exportFps = rates[i];
            }
        }
        right -= 8f;
        String previewLabel = FPSMaster.i18n.get("director.preview");
        float plw = FPSMaster.fontManager.getFont(12).getStringWidth(previewLabel);
        right -= UiChrome.SWITCH_SM_W;
        UiChrome.drawSwitchSm(right, y + 5.5f, DirectorCamera.isPreviewEnabled(),
                DirectorCamera.isPreviewEnabled() ? 1f : 0f);
        if (screen.consumePressInBounds(right - 2f, y + 4f, UiChrome.SWITCH_SM_W + 4f, 14f, 0) != null) {
            DirectorCamera.setPreviewEnabled(!DirectorCamera.isPreviewEnabled());
        }
        right -= plw + 4f;
        FPSMaster.fontManager.getFont(12).drawString(previewLabel, right, y + 7f,
                ClickGuiTheme.textSecondary().getRGB());

        // ---- timeline ----
        float tlX = x + 8f;
        float tlW = w - 16f;
        float tlY = y + 24f;
        drawTimeline(screen, player, track, tlX, tlY, tlW, mouseX, mouseY);

        // ---- action row ----
        float rowY = y + 40f;
        float bx = tlX;
        String addLabel = FPSMaster.i18n.get(selectedNearPlayhead(player) ? "director.key.update" : "director.key.add");
        float addW = FPSMaster.fontManager.getFont(12).getStringWidth(addLabel) + 24f;
        if (buttonSmall(screen, bx, rowY, addW, 14f, "plus", addLabel, !player.isPossessing(), mouseX, mouseY)) {
            addKeyframe(player);
        }
        bx += addW + 4f;
        if (editing) {
            String delLabel = FPSMaster.i18n.get("director.key.delete");
            float delW = FPSMaster.fontManager.getFont(12).getStringWidth(delLabel) + 24f;
            boolean delHover = Hover.is(bx, rowY, delW, 14f, mouseX, mouseY);
            UiChrome.dangerButton(bx, rowY, delW, 14f, delHover);
            Icons.draw("delete", bx + 6f, rowY + 3.5f, 7f, ClickGuiTheme.danger().getRGB());
            FPSMaster.fontManager.getFont(12).drawString(delLabel, bx + 16f, rowY + 4f,
                    ClickGuiTheme.danger().getRGB());
            if (screen.consumePressInBounds(bx, rowY, delW, 14f, 0) != null) {
                track.remove(selected);
                selected = null;
                DirectorCamera.markDirty();
                DirectorCamera.saveIfDirty();
            }
        } else {
            FPSMaster.fontManager.getFont(11).drawString(FPSMaster.i18n.get("director.hint"),
                    bx + 4f, rowY + 4f, ClickGuiTheme.textDisabled().getRGB());
        }

        // ---- transition / easing editors for the selection ----
        if (editing) {
            float edY = y + h - 20f;
            float segX = tlX;
            segX = enumSeg(screen, segX, edY, FPSMaster.i18n.get("director.transition"),
                    new String[]{i("director.transition.linear"), i("director.transition.smooth"), i("director.transition.cut")},
                    selected.transition.ordinal(), mouseX, mouseY, new EnumPick() {
                        @Override
                        public void pick(int index) {
                            selected.transition = CameraKeyframe.Transition.values()[index];
                            DirectorCamera.markDirty();
                            DirectorCamera.saveIfDirty();
                        }
                    });
            segX += 10f;
            enumSeg(screen, segX, edY, FPSMaster.i18n.get("director.easing"),
                    new String[]{i("director.easing.linear"), i("director.easing.ease"), i("director.easing.in"), i("director.easing.out"), i("director.easing.inout")},
                    selected.easing.ordinal(), mouseX, mouseY, new EnumPick() {
                        @Override
                        public void pick(int index) {
                            selected.easing = CameraKeyframe.Easing.values()[index];
                            DirectorCamera.markDirty();
                            DirectorCamera.saveIfDirty();
                        }
                    });
        }
    }

    private static String i(String key) {
        return FPSMaster.i18n.get(key);
    }

    private interface EnumPick {
        void pick(int index);
    }

    /** Label + compact segmented control; returns the x after the segment. */
    private static float enumSeg(ScaledGuiScreen screen, float x, float y, String label,
                                 String[] options, int current, int mouseX, int mouseY, EnumPick pick) {
        FPSMaster.fontManager.getFont(11).drawString(label, x, y + 3.5f, ClickGuiTheme.textDisabled().getRGB());
        float cursor = x + FPSMaster.fontManager.getFont(11).getStringWidth(label) + 4f;
        float segH = 13f;
        float[] widths = new float[options.length];
        float total = 3f;
        for (int idx = 0; idx < options.length; idx++) {
            widths[idx] = FPSMaster.fontManager.getFont(11).getStringWidth(options[idx]) + 8f;
            total += widths[idx] + (idx > 0 ? 1f : 0f);
        }
        UiChrome.seg(cursor, y, total, segH);
        float ox = cursor + 1.5f;
        for (int idx = 0; idx < options.length; idx++) {
            boolean on = idx == current;
            boolean hover = Hover.is(ox, y + 1.5f, widths[idx], segH - 3f, mouseX, mouseY);
            UiChrome.segOption(ox, y + 1.5f, widths[idx], segH - 3f, options[idx], on, hover);
            if (screen.consumePressInBounds(ox, y, widths[idx], segH, 0) != null) {
                pick.pick(idx);
            }
            ox += widths[idx] + 1f;
        }
        return cursor + total;
    }

    private static boolean buttonSmall(ScaledGuiScreen screen, float x, float y, float w, float h,
                                       String icon, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && Hover.is(x, y, w, h, mouseX, mouseY);
        UiChrome.fillButton(x, y, w, h, hover, false);
        if (!enabled) {
            Rects.rounded(x, y, w, h, UiChrome.CTL_RADIUS, ClickGuiTheme.mask(110).getRGB(), false);
        }
        Icons.draw(icon, x + 6f, y + (h - 7f) / 2f, 7f, 0xFFFFFFFF);
        FPSMaster.fontManager.getFont(12).drawString(label, x + 16f, y + h / 2f - 3f, 0xFFFFFFFF);
        return enabled && screen.consumePressInBounds(x, y, w, h, 0) != null;
    }

    // ------------------------------------------------------------------
    // Timeline
    // ------------------------------------------------------------------

    private static void drawTimeline(ScaledGuiScreen screen, ReplayPlayer player, CameraTrack track,
                                     float x, float y, float w, int mouseX, int mouseY) {
        int duration = ReplayHud.duration(player);
        if (duration <= 0) {
            duration = 1;
        }
        // track bar
        Rects.rounded(x, y + 4f, w, 3f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        // covered span
        if (!track.isEmpty()) {
            float sx = x + w * Math.min(1f, track.startMillis() / (float) duration);
            float ex = x + w * Math.min(1f, track.endMillis() / (float) duration);
            Rects.rounded(sx, y + 4f, Math.max(1.5f, ex - sx), 3f, 1, ClickGuiTheme.accentSoft().getRGB(), false);
        }
        // playhead
        float playX = x + w * Math.min(1f, player.elapsedMillis() / (float) duration);
        Rects.fill(playX, y - 1f, 1f, 13f, ClickGuiTheme.accent().getRGB());

        // keyframe diamonds
        CameraKeyframe hoveredFrame = null;
        for (CameraKeyframe frame : track.keyframes) {
            float kx = x + w * Math.min(1f, frame.timeMillis / (float) duration);
            boolean isSelected = frame == selected;
            boolean hover = Hover.is(kx - 3.5f, y, 7f, 11f, mouseX, mouseY);
            if (hover) {
                hoveredFrame = frame;
            }
            float size = isSelected ? 6f : 5f;
            Color fill = isSelected ? ClickGuiTheme.accent()
                    : hover ? ClickGuiTheme.accentText()
                    : new Color(255, 255, 255, 200);
            Rects.rounded(kx - size / 2f, y + 5.5f - size / 2f, size, size, (int) (size / 2f) - 1,
                    fill.getRGB(), false);
        }

        // interactions: drag selected diamond, click diamond to select, click bar to seek
        if (draggingKeyframe && selected != null) {
            if (screen.isDragging(DirectorPanel.class)) {
                float t = Math.max(0f, Math.min(1f, (mouseX - x) / w));
                selected.timeMillis = (int) (t * duration);
            } else {
                draggingKeyframe = false;
                screen.releaseDrag(DirectorPanel.class);
                track.sort();
                DirectorCamera.markDirty();
                DirectorCamera.saveIfDirty();
            }
            return;
        }
        if (hoveredFrame != null) {
            String tip = ReplayScreen.formatDuration(hoveredFrame.timeMillis);
            FPSMaster.fontManager.getFont(11).drawCenteredString(tip,
                    x + w * Math.min(1f, hoveredFrame.timeMillis / (float) duration), y - 8f,
                    ClickGuiTheme.textPrimary().getRGB());
        }
        ScaledGuiScreen.PointerEvent press = screen.consumePressInBounds(x - 4f, y - 2f, w + 8f, 15f, 0);
        if (press != null) {
            CameraKeyframe hit = null;
            for (CameraKeyframe frame : track.keyframes) {
                float kx = x + w * Math.min(1f, frame.timeMillis / (float) duration);
                if (Math.abs(press.x - kx) <= 4f) {
                    hit = frame;
                    break;
                }
            }
            if (hit != null) {
                selected = hit;
                if (screen.acquireDrag(DirectorPanel.class, 0)) {
                    draggingKeyframe = true;
                }
            } else {
                float t = Math.max(0f, Math.min(1f, (press.x - x) / w));
                player.seek((int) (t * duration));
            }
        }
    }

    private static boolean selectedNearPlayhead(ReplayPlayer player) {
        if (selected == null) {
            return false;
        }
        return Math.abs(selected.timeMillis - player.elapsedMillis()) <= DirectorCamera.MERGE_WINDOW_MILLIS;
    }

    private static void addKeyframe(ReplayPlayer player) {
        CameraPose pose = DirectorCamera.capturePose();
        if (pose == null) {
            return;
        }
        selected = DirectorCamera.track().add(player.elapsedMillis(), pose, DirectorCamera.MERGE_WINDOW_MILLIS);
        DirectorCamera.markDirty();
        DirectorCamera.saveIfDirty();
    }

    // ------------------------------------------------------------------
    // Export overlay
    // ------------------------------------------------------------------

    private static void drawExportOverlay(ScaledGuiScreen screen, float guiWidth, float guiHeight,
                                          int mouseX, int mouseY) {
        float w = 190f;
        float h = 64f;
        float x = (guiWidth - w) / 2f;
        float y = guiHeight - 12f - h;
        UiChrome.panel(x, y, w, h);
        UiChrome.boldString(FPSMaster.fontManager.s14, FPSMaster.i18n.get("director.export.running"),
                x + 11f, y + 9f, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(11).drawString(DirectorExporter.progressText(),
                x + 11f, y + 21f, ClickGuiTheme.textSecondary().getRGB());
        // progress bar
        float barY = y + 32f;
        Rects.rounded(x + 11f, barY, w - 22f, 3f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        float p = DirectorExporter.progress();
        if (p > 0f) {
            Rects.rounded(x + 11f, barY, Math.max(2f, (w - 22f) * p), 3f, 1,
                    ClickGuiTheme.accent().getRGB(), false);
        }
        if (UiChrome.buttonClicked(screen, x + w - 11f - 40f, y + h - 24f, 40f, 16f, null,
                FPSMaster.i18n.get("common.cancel"), UiChrome.Style.GHOST, mouseX, mouseY)) {
            DirectorExporter.cancel();
        }
    }

    /** One-line result banner drawn after an export ends; click dismisses. */
    public static void drawResultBanner(ScaledGuiScreen screen, float guiWidth, float guiHeight,
                                        int mouseX, int mouseY) {
        DirectorExporter.State state = DirectorExporter.state();
        if (state != DirectorExporter.State.DONE && state != DirectorExporter.State.FAILED) {
            return;
        }
        String text = state == DirectorExporter.State.DONE
                ? String.format(FPSMaster.i18n.get("director.export.done"),
                DirectorExporter.outputFile() == null ? "" : DirectorExporter.outputFile().getName())
                : DirectorExporter.errorMessage();
        float tw = FPSMaster.fontManager.getFont(12).getStringWidth(text) + 24f;
        float x = (guiWidth - tw) / 2f;
        float y = guiHeight - 12f - 18f;
        Rects.rounded(x - 0.5f, y - 0.5f, tw + 1f, 19f, UiChrome.CTL_RADIUS + 1,
                (state == DirectorExporter.State.DONE ? ClickGuiTheme.ok() : ClickGuiTheme.danger()).getRGB(), false);
        Rects.rounded(x, y, tw, 18f, UiChrome.CTL_RADIUS, new Color(14, 14, 14, 245).getRGB(), false);
        Icons.draw(state == DirectorExporter.State.DONE ? "check" : "alert", x + 6f, y + 5.5f, 7f,
                (state == DirectorExporter.State.DONE ? ClickGuiTheme.ok() : ClickGuiTheme.danger()).getRGB());
        FPSMaster.fontManager.getFont(12).drawString(text, x + 17f, y + 6f, ClickGuiTheme.textPrimary().getRGB());
        if (screen.consumePressInBounds(x, y, tw, 18f, 0) != null) {
            DirectorExporter.acknowledge();
        }
    }
}
