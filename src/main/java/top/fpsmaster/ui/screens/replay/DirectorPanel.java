package top.fpsmaster.ui.screens.replay;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.director.CameraKeyframe;
import top.fpsmaster.replay.director.CameraPose;
import top.fpsmaster.replay.director.CameraTrack;
import top.fpsmaster.replay.director.DirectorCamera;
import top.fpsmaster.replay.director.DirectorExporter;
import top.fpsmaster.replay.director.TimelineSegment;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

/**
 * The director workbench: keyframe timeline, cut list with per-segment speed, motion preview and
 * mp4 export — a bottom panel over the replay control screen, in the edge-ui language.
 *
 * <p>All state lives in {@link DirectorCamera} / {@link DirectorExporter}, so closing and
 * reopening the screen loses nothing.
 */
public final class DirectorPanel {

    private static final float[] SPEED_STEPS = {0.25f, 0.5f, 1f, 2f, 4f};
    private static final int[][] RESOLUTIONS = {{0, 0}, {1920, 1080}, {2560, 1440}, {3840, 2160}};

    private static boolean open;
    private static CameraKeyframe selected;
    private static TimelineSegment selectedSegment;
    private static boolean draggingKeyframe;

    private static boolean exportDialogOpen;
    private static int exportFps = 60;
    private static int exportResolution; // index into RESOLUTIONS

    private DirectorPanel() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void toggle() {
        open = !open;
    }

    public static void draw(ScaledGuiScreen screen, float guiWidth, float guiHeight, int mouseX, int mouseY) {
        if (DirectorExporter.isRunning()) {
            return; // the exporter paints its own presentation straight to the window
        }
        if (!open) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        CameraTrack track = DirectorCamera.track();
        int duration = Math.max(1, ReplayHud.duration(player));
        if (!track.keyframes.contains(selected)) {
            selected = null;
        }
        if (!track.segments.contains(selectedSegment)) {
            selectedSegment = null;
        }

        float w = Math.min(480f, guiWidth - 24f);
        float x = (guiWidth - w) / 2f;
        boolean editorRow = selected != null || selectedSegment != null;
        float h = editorRow ? 88f : 70f;
        float y = guiHeight - 12f - h;
        UiChrome.panel(x, y, w, h);

        drawHeader(screen, track, player, duration, x, y, w, mouseX, mouseY);

        float tlX = x + 8f;
        float tlW = w - 16f;
        drawTimeline(screen, player, track, duration, tlX, y + 25f, tlW, mouseX, mouseY);
        drawSegmentStrip(screen, track, duration, tlX, y + 39f, tlW, mouseX, mouseY);
        drawActionRow(screen, player, track, duration, tlX, y + 49f, tlW, mouseX, mouseY);

        if (selected != null) {
            drawKeyframeEditor(screen, tlX, y + h - 20f, mouseX, mouseY);
        } else if (selectedSegment != null) {
            drawSegmentEditor(screen, track, duration, tlX, y + h - 20f, mouseX, mouseY);
        }

        if (exportDialogOpen) {
            drawExportDialog(screen, player, track, duration, guiWidth, guiHeight, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------

    private static void drawHeader(ScaledGuiScreen screen, CameraTrack track, ReplayPlayer player,
                                   int duration, float x, float y, float w, int mouseX, int mouseY) {
        UiChrome.boldString(FPSMaster.fontManager.s14, FPSMaster.i18n.get("director.title"),
                x + 8f, y + 6f, ClickGuiTheme.textPrimary().getRGB());
        long outSpan = DirectorExporter.exportSpanMillis(track, duration);
        String meta = String.format(FPSMaster.i18n.get("director.keyframes"), track.keyframes.size())
                + (outSpan > 0 ? " · " + String.format(FPSMaster.i18n.get("director.outlen"),
                ReplayScreen.formatDuration(outSpan)) : "");
        FPSMaster.fontManager.getFont(11).drawString(meta,
                x + 8f + FPSMaster.fontManager.s14.getStringWidth(FPSMaster.i18n.get("director.title")) + 5f,
                y + 7.5f, ClickGuiTheme.textDisabled().getRGB());

        float right = x + w - 8f;
        String exportLabel = FPSMaster.i18n.get("director.export");
        float exportW = FPSMaster.fontManager.getFont(12).getStringWidth(exportLabel) + 24f;
        right -= exportW;
        if (buttonSmall(screen, right, y + 4.5f, exportW, 14f, "film", exportLabel,
                outSpan > 0, mouseX, mouseY)) {
            DirectorExporter.acknowledge();
            exportDialogOpen = true;
        }
        right -= 8f + UiChrome.SWITCH_SM_W;
        UiChrome.drawSwitchSm(right, y + 5.5f, DirectorCamera.isPreviewEnabled(),
                DirectorCamera.isPreviewEnabled() ? 1f : 0f);
        if (screen.consumePressInBounds(right - 2f, y + 4f, UiChrome.SWITCH_SM_W + 4f, 14f, 0) != null) {
            DirectorCamera.setPreviewEnabled(!DirectorCamera.isPreviewEnabled());
        }
        String previewLabel = FPSMaster.i18n.get("director.preview");
        float plw = FPSMaster.fontManager.getFont(12).getStringWidth(previewLabel);
        right -= plw + 4f;
        FPSMaster.fontManager.getFont(12).drawString(previewLabel, right, y + 7f,
                ClickGuiTheme.textSecondary().getRGB());
    }

    // ------------------------------------------------------------------
    // Timeline (keyframes)
    // ------------------------------------------------------------------

    private static void drawTimeline(ScaledGuiScreen screen, ReplayPlayer player, CameraTrack track,
                                     int duration, float x, float y, float w, int mouseX, int mouseY) {
        Rects.rounded(x, y + 4f, w, 3f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        if (!track.isEmpty()) {
            float sx = x + w * Math.min(1f, track.startMillis() / (float) duration);
            float ex = x + w * Math.min(1f, track.endMillis() / (float) duration);
            Rects.rounded(sx, y + 4f, Math.max(1.5f, ex - sx), 3f, 1, ClickGuiTheme.accentSoft().getRGB(), false);
        }
        float playX = x + w * Math.min(1f, player.elapsedMillis() / (float) duration);
        Rects.fill(playX, y - 1f, 1f, 20f, ClickGuiTheme.accent().getRGB());

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
            FPSMaster.fontManager.getFont(11).drawCenteredString(
                    ReplayScreen.formatDuration(hoveredFrame.timeMillis),
                    x + w * Math.min(1f, hoveredFrame.timeMillis / (float) duration), y - 8f,
                    ClickGuiTheme.textPrimary().getRGB());
        }
        ScaledGuiScreen.PointerEvent press = screen.consumePressInBounds(x - 4f, y - 2f, w + 8f, 13f, 0);
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
                selectedSegment = null;
                if (screen.acquireDrag(DirectorPanel.class, 0)) {
                    draggingKeyframe = true;
                }
            } else {
                float t = Math.max(0f, Math.min(1f, (press.x - x) / w));
                player.seek((int) (t * duration));
            }
        }
    }

    // ------------------------------------------------------------------
    // Segment strip (cut list)
    // ------------------------------------------------------------------

    private static void drawSegmentStrip(ScaledGuiScreen screen, CameraTrack track, int duration,
                                         float x, float y, float w, int mouseX, int mouseY) {
        float stripH = 5f;
        if (track.segments.isEmpty()) {
            Rects.rounded(x, y, w, stripH, 2, ClickGuiTheme.layer().getRGB(), false);
            return;
        }
        for (TimelineSegment segment : track.segments) {
            float sx = x + w * Math.min(1f, segment.startMillis / (float) duration);
            float ex = x + w * Math.min(1f, segment.endMillis / (float) duration);
            float sw = Math.max(1f, ex - sx - 0.5f);
            boolean isSelected = segment == selectedSegment;
            Color fill;
            if (segment.excluded) {
                fill = new Color(240, 80, 110, isSelected ? 120 : 60);
            } else if (segment.speed != 1f) {
                fill = new Color(226, 185, 61, isSelected ? 200 : 130);
            } else {
                fill = isSelected ? ClickGuiTheme.accent() : ClickGuiTheme.accentSoft();
            }
            Rects.rounded(sx, y, sw, stripH, 2, fill.getRGB(), false);
            if (isSelected) {
                Rects.rounded(sx, y - 1f, sw, 1f, 0, ClickGuiTheme.textPrimary().getRGB(), false);
            }
            // speed tag on stretched-enough segments
            if (!segment.excluded && segment.speed != 1f && sw > 22f) {
                FPSMaster.fontManager.getFont(10).drawCenteredString(formatSpeed(segment.speed),
                        sx + sw / 2f, y - 0.5f, new Color(226, 185, 61).getRGB());
            }
            if (screen.consumePressInBounds(sx, y - 1f, sw, stripH + 2f, 0) != null) {
                selectedSegment = segment;
                selected = null;
            }
        }
    }

    private static String formatSpeed(float speed) {
        return speed == Math.round(speed) ? Math.round(speed) + "x" : speed + "x";
    }

    // ------------------------------------------------------------------
    // Action row
    // ------------------------------------------------------------------

    private static void drawActionRow(ScaledGuiScreen screen, ReplayPlayer player, CameraTrack track,
                                      int duration, float x, float y, float w, int mouseX, int mouseY) {
        float bx = x;
        String addLabel = FPSMaster.i18n.get(selectedNearPlayhead(player) ? "director.key.update" : "director.key.add");
        float addW = FPSMaster.fontManager.getFont(12).getStringWidth(addLabel) + 24f;
        if (buttonSmall(screen, bx, y, addW, 14f, "plus", addLabel, !player.isPossessing(), mouseX, mouseY)) {
            addKeyframe(player);
        }
        bx += addW + 4f;
        if (selected != null) {
            String delLabel = FPSMaster.i18n.get("director.key.delete");
            float delW = FPSMaster.fontManager.getFont(12).getStringWidth(delLabel) + 24f;
            if (dangerSmall(screen, bx, y, delW, 14f, delLabel, mouseX, mouseY)) {
                track.remove(selected);
                selected = null;
                saveEdit(track);
            }
            bx += delW + 4f;
        }

        // cut tools, right-aligned
        float right = x + w;
        String[] cutKeys = {"director.cut.split", "director.cut.out", "director.cut.in"};
        String[] cutIcons = {"close", "next", "prev"};
        for (int i = 0; i < cutKeys.length; i++) {
            String label = FPSMaster.i18n.get(cutKeys[i]);
            float bw = FPSMaster.fontManager.getFont(12).getStringWidth(label) + 22f;
            right -= bw + (i > 0 ? 3f : 0f);
            boolean hover = Hover.is(right, y, bw, 14f, mouseX, mouseY);
            UiChrome.button(right, y, bw, 14f, hover);
            Icons.draw(cutIcons[i], right + 5f, y + 3.5f, 7f,
                    (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
            FPSMaster.fontManager.getFont(12).drawString(label, right + 14.5f, y + 4f,
                    (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
            if (screen.consumePressInBounds(right, y, bw, 14f, 0) != null) {
                int at = player.elapsedMillis();
                if (i == 2) {
                    track.trimStart(at, duration);
                } else if (i == 1) {
                    track.trimEnd(at, duration);
                } else {
                    track.splitAt(at, duration);
                }
                saveEdit(track);
            }
        }
        if (selected == null && selectedSegment == null) {
            float hintMax = right - bx - 8f;
            String hint = FPSMaster.fontManager.getFont(11).trimStringToWidth(
                    FPSMaster.i18n.get("director.hint"), hintMax);
            FPSMaster.fontManager.getFont(11).drawString(hint, bx + 4f, y + 4f,
                    ClickGuiTheme.textDisabled().getRGB());
        }
    }

    // ------------------------------------------------------------------
    // Editors
    // ------------------------------------------------------------------

    private static void drawKeyframeEditor(ScaledGuiScreen screen, float x, float y, int mouseX, int mouseY) {
        float segX = x;
        segX = enumSeg(screen, segX, y, FPSMaster.i18n.get("director.transition"),
                new String[]{i("director.transition.linear"), i("director.transition.smooth"), i("director.transition.cut")},
                selected.transition.ordinal(), mouseX, mouseY, new EnumPick() {
                    @Override
                    public void pick(int index) {
                        selected.transition = CameraKeyframe.Transition.values()[index];
                        saveEdit(DirectorCamera.track());
                    }
                });
        segX += 10f;
        enumSeg(screen, segX, y, FPSMaster.i18n.get("director.easing"),
                new String[]{i("director.easing.linear"), i("director.easing.ease"), i("director.easing.in"), i("director.easing.out"), i("director.easing.inout")},
                selected.easing.ordinal(), mouseX, mouseY, new EnumPick() {
                    @Override
                    public void pick(int index) {
                        selected.easing = CameraKeyframe.Easing.values()[index];
                        saveEdit(DirectorCamera.track());
                    }
                });
    }

    private static void drawSegmentEditor(ScaledGuiScreen screen, final CameraTrack track, int duration,
                                          float x, float y, int mouseX, int mouseY) {
        final TimelineSegment segment = selectedSegment;
        // keep / exclude toggle
        String toggleLabel = FPSMaster.i18n.get(segment.excluded ? "director.seg.include" : "director.seg.exclude");
        float tw = FPSMaster.fontManager.getFont(12).getStringWidth(toggleLabel) + 22f;
        boolean hover = Hover.is(x, y, tw, 14f, mouseX, mouseY);
        if (segment.excluded) {
            UiChrome.button(x, y, tw, 14f, hover);
        } else {
            UiChrome.dangerButton(x, y, tw, 14f, hover);
        }
        int toggleColor = segment.excluded
                ? (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB()
                : ClickGuiTheme.danger().getRGB();
        Icons.draw(segment.excluded ? "check" : "close", x + 5f, y + 3.5f, 7f, toggleColor);
        FPSMaster.fontManager.getFont(12).drawString(toggleLabel, x + 14.5f, y + 4f, toggleColor);
        if (screen.consumePressInBounds(x, y, tw, 14f, 0) != null) {
            segment.excluded = !segment.excluded;
            saveEdit(track);
        }
        float segX = x + tw + 10f;

        // speed selector
        int speedIndex = 2;
        for (int idx = 0; idx < SPEED_STEPS.length; idx++) {
            if (SPEED_STEPS[idx] == segment.speed) {
                speedIndex = idx;
            }
        }
        String[] speedLabels = new String[SPEED_STEPS.length];
        for (int idx = 0; idx < SPEED_STEPS.length; idx++) {
            speedLabels[idx] = formatSpeed(SPEED_STEPS[idx]);
        }
        segX = enumSeg(screen, segX, y, FPSMaster.i18n.get("director.seg.speed"), speedLabels,
                speedIndex, mouseX, mouseY, new EnumPick() {
                    @Override
                    public void pick(int index) {
                        segment.speed = SPEED_STEPS[index];
                        saveEdit(track);
                    }
                });
        segX += 10f;

        // merge neighbours with identical state
        String mergeLabel = FPSMaster.i18n.get("director.seg.merge");
        float mw = FPSMaster.fontManager.getFont(12).getStringWidth(mergeLabel) + 12f;
        boolean mergeHover = Hover.is(segX, y, mw, 14f, mouseX, mouseY);
        UiChrome.ghostButton(segX, y, mw, 14f, mergeHover);
        FPSMaster.fontManager.getFont(12).drawString(mergeLabel, segX + 6f, y + 4f,
                (mergeHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (screen.consumePressInBounds(segX, y, mw, 14f, 0) != null) {
            track.mergeAdjacent();
            selectedSegment = null;
            saveEdit(track);
        }
    }

    // ------------------------------------------------------------------
    // Export dialog + overlay
    // ------------------------------------------------------------------

    private static void drawExportDialog(ScaledGuiScreen screen, ReplayPlayer player, CameraTrack track,
                                         int duration, float guiWidth, float guiHeight, int mouseX, int mouseY) {
        UiChrome.veil(guiWidth, guiHeight, 0.85f);
        float w = 210f;
        float h = 96f;
        float x = (guiWidth - w) / 2f;
        float y = (guiHeight - h) / 2f;
        UiChrome.panel(x, y, w, h);
        UiChrome.boldString(FPSMaster.fontManager.s14, FPSMaster.i18n.get("director.export.title"),
                x + 11f, y + 9f, ClickGuiTheme.textPrimary().getRGB());

        // fps
        int[] rates = {24, 30, 60};
        String[] rateLabels = {"24", "30", "60"};
        int rateIndex = exportFps == 24 ? 0 : exportFps == 30 ? 1 : 2;
        enumSeg(screen, x + 11f, y + 24f, FPSMaster.i18n.get("director.export.fps"), rateLabels,
                rateIndex, mouseX, mouseY, new EnumPick() {
                    @Override
                    public void pick(int index) {
                        exportFps = new int[]{24, 30, 60}[index];
                    }
                });

        // resolution
        String[] resLabels = new String[RESOLUTIONS.length];
        resLabels[0] = FPSMaster.i18n.get("director.export.res.window");
        for (int idx = 1; idx < RESOLUTIONS.length; idx++) {
            resLabels[idx] = RESOLUTIONS[idx][1] + "p";
        }
        enumSeg(screen, x + 11f, y + 42f, FPSMaster.i18n.get("director.export.res"), resLabels,
                exportResolution, mouseX, mouseY, new EnumPick() {
                    @Override
                    public void pick(int index) {
                        exportResolution = index;
                    }
                });

        long outSpan = DirectorExporter.exportSpanMillis(track, duration);
        long frames = outSpan * exportFps / 1000L + 1;
        FPSMaster.fontManager.getFont(11).drawString(
                String.format(FPSMaster.i18n.get("director.export.estimate"),
                        ReplayScreen.formatDuration(outSpan), frames),
                x + 11f, y + 60f, ClickGuiTheme.textDisabled().getRGB());

        float by = y + h - 26f;
        if (UiChrome.buttonClicked(screen, x + w - 11f - 58f, by, 58f, UiChrome.BTN_H, "film",
                FPSMaster.i18n.get("director.export.start"), UiChrome.Style.PRIMARY, mouseX, mouseY)) {
            exportDialogOpen = false;
            DirectorExporter.start(exportFps, RESOLUTIONS[exportResolution][0], RESOLUTIONS[exportResolution][1]);
        }
        if (UiChrome.buttonClicked(screen, x + w - 11f - 58f - 5f - 40f, by, 40f, UiChrome.BTN_H, null,
                FPSMaster.i18n.get("common.cancel"), UiChrome.Style.GHOST, mouseX, mouseY)) {
            exportDialogOpen = false;
        }
        if (screen.consumePressOutside(x, y, w, h) != null) {
            exportDialogOpen = false;
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

    // ------------------------------------------------------------------
    // Small shared widgets
    // ------------------------------------------------------------------

    private static String i(String key) {
        return FPSMaster.i18n.get(key);
    }

    private interface EnumPick {
        void pick(int index);
    }

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

    private static boolean dangerSmall(ScaledGuiScreen screen, float x, float y, float w, float h,
                                       String label, int mouseX, int mouseY) {
        boolean hover = Hover.is(x, y, w, h, mouseX, mouseY);
        UiChrome.dangerButton(x, y, w, h, hover);
        Icons.draw("delete", x + 6f, y + 3.5f, 7f, ClickGuiTheme.danger().getRGB());
        FPSMaster.fontManager.getFont(12).drawString(label, x + 16f, y + 4f, ClickGuiTheme.danger().getRGB());
        return screen.consumePressInBounds(x, y, w, h, 0) != null;
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
        selectedSegment = null;
        saveEdit(DirectorCamera.track());
    }

    private static void saveEdit(CameraTrack track) {
        DirectorCamera.markDirty();
        DirectorCamera.saveIfDirty();
    }
}
