package top.fpsmaster.ui.screens.replay;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.director.CameraChannel;
import top.fpsmaster.replay.director.CameraKeyframe;
import top.fpsmaster.replay.director.CameraPose;
import top.fpsmaster.replay.director.CameraTrack;
import top.fpsmaster.replay.director.DirectorCamera;
import top.fpsmaster.replay.director.DirectorExporter;
import top.fpsmaster.replay.director.EditClip;
import top.fpsmaster.replay.director.EditProject;
import top.fpsmaster.replay.director.PropKeyframe;
import top.fpsmaster.replay.director.SpeedPoint;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;
import java.util.List;

/**
 * Docked NLE: clip lane + per-property camera tracks under the game view.
 *
 * <p>Painted immediate-mode through {@link DirectorUi} (same contract as {@link UiChrome}: draw this
 * frame, hit-test this frame). Height is drag-resizable. Project state lives in
 * {@link DirectorCamera}, so closing the panel does not throw the edit away.
 */
public final class DirectorPanel {

    private static final float[] SPEED_STEPS = {0.25f, 0.5f, 1f, 2f, 4f};
    private static final int[][] RESOLUTIONS = {{0, 0}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
    private static final int[] CLIP_COLORS = {0xFF5B67F2, 0xFF2BB59A, 0xFFD59A3A};
    private static final float HEIGHT_DEFAULT = 136f;
    private static final float HEIGHT_MIN = 64f;
    private static final float SPEED_Y_MIN = 0.25f;
    private static final float SPEED_Y_MAX = 8f;
    private static final Object DRAG_RESIZE = new Object();
    private static final Object DRAG_HEAD = new Object();
    private static final Object DRAG_CLIP = new Object();
    private static final Object DRAG_TRIM = new Object();
    private static final Object DRAG_KF = new Object();
    private static final Object DRAG_CURVE = new Object();
    private static final Object DRAG_PAN = new Object();
    private static final Object DRAG_SCROLL = new Object();
    private static final Object DRAG_FOV = new Object();
    private static final float FOV_MIN = 30f;
    private static final float FOV_MAX = 110f;

    private static boolean open;
    private static boolean razor;
    private static float panelHeight = HEIGHT_DEFAULT;
    private static int selectedClip = -1;
    private static PropKeyframe selectedProp;
    private static CameraChannel selectedChannel = CameraChannel.POSITION;
    private static boolean draggingKeyframe;
    private static float view0;
    private static float ppm;
    private static boolean needsFit = true;
    private static float lastPanMouseX;
    private static boolean draggingTrimLeft;
    private static int draggingClip = -1;
    private static int insertClip = -1;
    private static int menuClip = -1;
    private static boolean menuOnKeyframe;
    private static float menuX;
    private static float menuY;
    private static int selectedPoint = -1;
    private static int dragHandle;
    private static boolean curveDirty;
    private static boolean exportDialogOpen;
    private static int exportFps = 60;
    private static int exportResolution;
    private static String toast = "";
    private static long toastUntil;
    private static boolean scrubbing;
    private static long scrubOutputMillis;
    private static float fovHitX;
    private static float fovHitY;
    private static float fovHitW;
    private static float fovHitH;

    private DirectorPanel() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static float top(float guiHeight) {
        return guiHeight - panelHeight;
    }

    public static boolean covers(float guiHeight, int mouseX, int mouseY) {
        return open && mouseY >= top(guiHeight);
    }

    public static CameraChannel selectedChannel() {
        return selectedChannel;
    }

    public static void setOpen(boolean value) {
        open = value;
        if (!value) {
            menuClip = -1;
            razor = false;
        }
    }

    public static void toggle() {
        setOpen(!open);
    }

    public static void toggleRazor() {
        razor = !razor;
        menuClip = -1;
    }

    public static void splitAtPlayhead() {
        EditProject project = DirectorCamera.project();
        if (project == null) {
            return;
        }
        DirectorCamera.noteBeforeChange();
        project.splitAtOutput(DirectorCamera.currentOutputTime());
        selectedClip = project.clipIndexAtOutput(DirectorCamera.currentOutputTime());
        save();
    }

    public static PropKeyframe selectedKeyframe() {
        return selectedProp;
    }

    public static boolean handleKey(int keyCode) {
        if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_P) {
            ReplayPlayer.instance().togglePause();
            return true;
        }
        if (keyCode == Keyboard.KEY_V) {
            razor = false;
            return true;
        }
        if (keyCode == Keyboard.KEY_C) {
            toggleRazor();
            return true;
        }
        if (keyCode == Keyboard.KEY_S
                && !Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                && !Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                && !Keyboard.isKeyDown(Keyboard.KEY_LMETA)
                && !Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            splitAtPlayhead();
            return true;
        }
        if (keyCode == Keyboard.KEY_K) {
            addKeyframe(ReplayPlayer.instance());
            return true;
        }
        if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
            if (selectedPoint > 0 && selectedClip >= 0) {
                EditProject project = DirectorCamera.project();
                if (project != null && selectedClip < project.clips.size()) {
                    DirectorCamera.noteBeforeChange();
                    project.clips.get(selectedClip).removeCurvePoint(selectedPoint);
                    selectedPoint = -1;
                    save();
                    return true;
                }
            }
            deleteSelected();
            return true;
        }
        if (keyCode == Keyboard.KEY_D
                && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA)
                || Keyboard.isKeyDown(Keyboard.KEY_RMETA))) {
            duplicateSelectedClip();
            return true;
        }
        if (keyCode == Keyboard.KEY_LBRACKET) {
            nudgeSelection(-1);
            return true;
        }
        if (keyCode == Keyboard.KEY_RBRACKET) {
            nudgeSelection(1);
            return true;
        }
        if (keyCode == Keyboard.KEY_HOME) {
            DirectorCamera.seekToOutput(0);
            return true;
        }
        if (keyCode == Keyboard.KEY_END) {
            EditProject project = DirectorCamera.project();
            long dur = project == null ? ReplayPlayer.instance().durationMillis()
                    : project.outputDurationMillis();
            DirectorCamera.seekToOutput(dur);
            return true;
        }
        if (keyCode == Keyboard.KEY_LEFT) {
            DirectorCamera.seekToOutput(Math.max(0, DirectorCamera.currentOutputTime() - 200));
            return true;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            DirectorCamera.seekToOutput(DirectorCamera.currentOutputTime() + 200);
            return true;
        }
        if (keyCode == Keyboard.KEY_F) {
            needsFit = true;
            return true;
        }
        return false;
    }

    private static void nudgeSelection(int delta) {
        EditProject project = DirectorCamera.project();
        if (project == null || project.clips.isEmpty()) {
            return;
        }
        if (selectedProp != null) {
            List<PropKeyframe> keys = DirectorCamera.track().channel(selectedChannel);
            int i = keys.indexOf(selectedProp) + delta;
            if (i >= 0 && i < keys.size()) {
                selectedProp = keys.get(i);
                DirectorCamera.seekToOutput(project.outputTimeFor(
                        Math.max(0, indexContainingSource(project, selectedProp.timeMillis)),
                        selectedProp.timeMillis));
            }
            return;
        }
        selectedClip = Math.max(0, Math.min(project.clips.size() - 1, selectedClip + delta));
        DirectorCamera.seekToOutput(project.outputStartOf(selectedClip));
    }

    public static void deleteSelected() {
        EditProject project = DirectorCamera.project();
        CameraTrack track = DirectorCamera.track();
        if (selectedProp != null && track.channel(selectedChannel).contains(selectedProp)) {
            DirectorCamera.noteBeforeChange();
            track.remove(selectedChannel, selectedProp);
            selectedProp = null;
            save();
            return;
        }
        if (project != null && selectedClip >= 0) {
            DirectorCamera.noteBeforeChange();
            project.removeClip(selectedClip);
            selectedClip = Math.min(selectedClip, project.clips.size() - 1);
            save();
        }
    }

    public static void duplicateSelectedClip() {
        EditProject project = DirectorCamera.project();
        if (project == null || selectedClip < 0) {
            return;
        }
        DirectorCamera.noteBeforeChange();
        int next = project.duplicateClip(selectedClip);
        if (next >= 0) {
            selectedClip = next;
            save();
            toast(FPSMaster.i18n.get("edit.duplicate.done"));
        }
    }

    public static void draw(ScaledGuiScreen screen, float guiWidth, float guiHeight, int mouseX, int mouseY) {
        if (DirectorExporter.isRunning() || !open) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        CameraTrack track = DirectorCamera.track();
        EditProject project = DirectorCamera.project();
        int sourceDuration = Math.max(1, ReplayHud.duration(player));
        if (project != null) {
            project.ensureDuration(sourceDuration);
        }
        track.migratePackedKeyframes();
        if (selectedProp != null && !track.channel(selectedChannel).contains(selectedProp)) {
            selectedProp = null;
        }
        boolean rolling = Keyboard.isKeyDown(Keyboard.KEY_Q)
                || Keyboard.isKeyDown(Keyboard.KEY_E)
                || Keyboard.isKeyDown(Keyboard.KEY_R);
        if (selectedProp != null && selectedChannel == CameraChannel.ROLL && rolling) {
            selectedProp.a = DirectorCamera.roll();
            DirectorCamera.markDirty();
        }
        if (project != null && (selectedClip < 0 || selectedClip >= project.clips.size())) {
            selectedClip = project.clips.isEmpty() ? -1 : 0;
        }

        boolean anyCurve = project != null && clipHasCurve(project, selectedClip);
        float clipLaneH = anyCurve ? DirectorUi.CLIP_CURVE_H : DirectorUi.CLIP_H;
        float inspectH = selectedProp != null ? DirectorUi.INSPECT_H + 2f : 0f;
        float needed = 4f + DirectorUi.TOOLBAR_H + inspectH + DirectorUi.RULER_H + clipLaneH + 2f
                + CameraChannel.values().length * DirectorUi.PROP_ROW + DirectorUi.SCROLL_H + 8f;
        if (panelHeight < needed) {
            panelHeight = needed;
        }
        float maxH = Math.max(HEIGHT_MIN, guiHeight * 0.55f);
        panelHeight = Math.max(HEIGHT_MIN, Math.min(maxH, panelHeight));
        float y = guiHeight - panelHeight;

        DirectorUi.beginFrame();

        // Immediate-mode hit-testing is first-come: while a popup is open the rest of the panel
        // paints but does not consume, so the menu / dialog (drawn last) actually receive the click.
        boolean overlayOpen = screen != null && (exportDialogOpen || menuClip >= 0);
        ScaledGuiScreen hits = overlayOpen ? null : screen;
        if (beginDrag(hits, DRAG_RESIZE, 0f, y - 2f, guiWidth, 8f)) {
            panelHeight = Math.max(HEIGHT_MIN, Math.min(maxH, guiHeight - mouseY));
        }

        Rects.fill(0f, y, guiWidth, panelHeight, new Color(14, 14, 16, 240).getRGB());
        Rects.fill(0f, y, guiWidth, DirectorUi.TOOLBAR_H + 4f, new Color(20, 20, 22, 250).getRGB());
        Rects.fill(0f, y, guiWidth, 0.5f, ClickGuiTheme.strokeStrong().getRGB());
        Rects.rounded(guiWidth / 2f - 16f, y + 1.5f, 32f, 1.5f, 1, ClickGuiTheme.textDisabled().getRGB(), false);

        float pad = 8f;
        float toolsY = y + 4f;
        drawTools(hits, player, project, sourceDuration, pad, toolsY, guiWidth - pad * 2f, mouseX, mouseY);

        float bodyY = toolsY + DirectorUi.TOOLBAR_H;
        Rects.fill(0f, bodyY - 0.5f, guiWidth, 0.5f, ClickGuiTheme.divider().getRGB());
        if (selectedProp != null) {
            drawInspector(hits, pad, bodyY, guiWidth - pad * 2f, mouseX, mouseY);
            bodyY += DirectorUi.INSPECT_H + 2f;
        }

        float headerW = DirectorUi.HEADER_W;
        float laneX = pad + headerW + 4f;
        float laneW = guiWidth - pad - laneX;
        float bodyH = guiHeight - bodyY - 4f;

        int wheel = overlayOpen || screen == null ? 0 : screen.consumeWheel();
        long outDur = project != null && !project.clips.isEmpty()
                ? Math.max(1L, project.outputDurationMillis())
                : Math.max(1, sourceDuration);
        boolean zoomWheel = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA)
                || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
        if (wheel != 0 && Hover.is(fovHitX, fovHitY, fovHitW, fovHitH, mouseX, mouseY)) {
            applyFovEdit(DirectorCamera.flyFov() + (wheel > 0 ? 2f : -2f));
        } else if (wheel != 0 && Hover.is(pad, bodyY, guiWidth - pad * 2f, bodyH, mouseX, mouseY)) {
            float visible = ppm <= 0f ? laneW : laneW / ppm;
            boolean canPan = visible < outDur - 1f;
            if (zoomWheel || !canPan) {
                zoomView(laneX, mouseX, laneW, outDur, wheel);
            } else {
                panView(laneW, outDur, wheel);
            }
        } else if (wheel != 0 && mouseY < y) {
            DirectorCamera.nudgeFov(wheel > 0 ? 2f : -2f);
            if (selectedProp != null && selectedChannel == CameraChannel.FOV) {
                selectedProp.a = DirectorCamera.flyFov();
                DirectorCamera.markDirty();
            }
        }

        drawRulerAndClips(hits, player, project, track, sourceDuration,
                pad, laneX, bodyY, headerW, laneW, bodyH, clipLaneH, overlayOpen, mouseX, mouseY);

        if (screen != null && menuClip >= 0 && project != null) {
            drawContextMenu(screen, project, track, mouseX, mouseY);
        }
        if (screen != null && exportDialogOpen) {
            drawExportDialog(screen, player, track, sourceDuration, guiWidth, guiHeight, mouseX, mouseY);
        }
        if (!toast.isEmpty() && System.currentTimeMillis() < toastUntil) {
            float tw = FPSMaster.fontManager.getFont(12).getStringWidth(toast) + 16f;
            Rects.rounded(guiWidth - tw - 10f, y - 22f, tw, 16f, 5,
                    new Color(18, 18, 20, 240).getRGB(), false);
            FPSMaster.fontManager.getFont(12).drawString(toast, guiWidth - tw - 2f, y - 17f,
                    ClickGuiTheme.textPrimary().getRGB());
        } else {
            toast = "";
        }
        DirectorUi.endFrame(guiWidth, guiHeight);
    }

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
        float y = 56f;
        Rects.rounded(x - 0.5f, y - 0.5f, tw + 1f, 19f, UiChrome.CTL_RADIUS + 1,
                (state == DirectorExporter.State.DONE ? ClickGuiTheme.ok() : ClickGuiTheme.danger()).getRGB(), false);
        Rects.rounded(x, y, tw, 18f, UiChrome.CTL_RADIUS, new Color(14, 14, 14, 245).getRGB(), false);
        Icons.draw(state == DirectorExporter.State.DONE ? "check" : "alert", x + 6f, y + 5.5f, 7f,
                (state == DirectorExporter.State.DONE ? ClickGuiTheme.ok() : ClickGuiTheme.danger()).getRGB());
        FPSMaster.fontManager.getFont(12).drawString(text, x + 17f, y + 6f, ClickGuiTheme.textPrimary().getRGB());
        if (DirectorUi.click(screen, x, y, tw, 18f, 0)) {
            DirectorExporter.acknowledge();
        }
    }

    private static void drawTools(ScaledGuiScreen screen, ReplayPlayer player, EditProject project,
                                  int sourceDuration, float x, float y, float w, int mouseX, int mouseY) {
        DirectorUi.Bar bar = new DirectorUi.Bar(screen, x, y, w, DirectorUi.TOOLBAR_H, mouseX, mouseY);
        if (bar.iconLeft(player.isPaused() ? "play" : "pause",
                FPSMaster.i18n.get(player.isPaused() ? "edit.tool.play" : "edit.tool.pause"), false, true)) {
            player.togglePause();
        }
        long shown = project != null ? DirectorCamera.currentOutputTime() : player.elapsedMillis();
        if (scrubbing) {
            shown = scrubOutputMillis;
        }
        long total = project != null && !project.clips.isEmpty()
                ? project.outputDurationMillis() : sourceDuration;
        bar.labelLeft(ReplayScreen.formatDuration(shown) + " / " + ReplayScreen.formatDuration(total),
                ClickGuiTheme.textPrimary().getRGB());
        bar.labelLeft(String.format(FPSMaster.i18n.get("edit.roll"), Math.round(DirectorCamera.roll())),
                ClickGuiTheme.textSecondary().getRGB());
        float fov = DirectorCamera.flyFov();
        bar.labelLeft("FOV " + Math.round(fov) + "°", ClickGuiTheme.textSecondary().getRGB());
        float slW = 52f;
        fovHitX = bar.left - 2f;
        fovHitY = bar.btnY - 1f;
        fovHitW = slW + 4f;
        fovHitH = DirectorUi.TOOL_H + 2f;
        float fovT = (fov - FOV_MIN) / (FOV_MAX - FOV_MIN);
        boolean fovHeld = beginDrag(screen, DRAG_FOV, fovHitX, fovHitY, fovHitW, fovHitH);
        if (fovHeld) {
            fovT = Math.max(0f, Math.min(1f, (mouseX - bar.left) / slW));
            applyFovEdit(FOV_MIN + fovT * (FOV_MAX - FOV_MIN));
        } else if (draggingClip < 0 && !dragging(screen, DRAG_FOV) && Hover.is(fovHitX, fovHitY, fovHitW, fovHitH, mouseX, mouseY)) {
            DirectorUi.tip(FPSMaster.i18n.get("edit.fov.tip"), bar.left + slW / 2f, bar.btnY);
        }
        UiChrome.slider(bar.left, bar.btnY + 1f, slW, fovT, true);
        bar.left += slW + 6f;
        bar.ruleLeft();
        if (bar.iconLeft("zap", FPSMaster.i18n.get("edit.tool.razor"), razor, true)) {
            toggleRazor();
        }
        if (bar.textLeft(FPSMaster.i18n.get("director.cut.split"),
                FPSMaster.i18n.get("edit.tool.split"), false, true)) {
            splitAtPlayhead();
        }
        bar.ruleLeft();
        if (bar.iconLeft("plus", FPSMaster.i18n.get("edit.tool.key"), false,
                !player.isPossessing())) {
            addKeyframe(player);
        }
        if (bar.iconLeft("copy", FPSMaster.i18n.get("edit.duplicate"), false, selectedClip >= 0)) {
            duplicateSelectedClip();
        }
        if (bar.textLeft(FPSMaster.i18n.get("edit.tool.fit"),
                FPSMaster.i18n.get("edit.tool.fit.tip"), false, true)) {
            needsFit = true;
        }

        if (bar.textRight(FPSMaster.i18n.get("edit.redo"), FPSMaster.i18n.get("edit.redo.tip"),
                false, DirectorCamera.canRedo())) {
            DirectorCamera.redo();
        }
        if (bar.textRight(FPSMaster.i18n.get("edit.undo"), FPSMaster.i18n.get("edit.undo.tip"),
                false, DirectorCamera.canUndo())) {
            DirectorCamera.undo();
        }
        if (bar.iconRight("disc", FPSMaster.i18n.get("edit.save"), false, true)) {
            DirectorCamera.markDirty();
            DirectorCamera.saveIfDirty();
            toast(FPSMaster.i18n.get("edit.saved"));
        }
        if (bar.iconRight("export", FPSMaster.i18n.get("director.export"), false, true)) {
            DirectorExporter.acknowledge();
            exportDialogOpen = true;
        }
        if (bar.iconRight("eye", FPSMaster.i18n.get("director.preview"),
                DirectorCamera.isPreviewEnabled(), true)) {
            DirectorCamera.setPreviewEnabled(!DirectorCamera.isPreviewEnabled());
        }
    }

    private static void drawRulerAndClips(ScaledGuiScreen screen, ReplayPlayer player, EditProject project,
                                          CameraTrack track, int sourceDuration,
                                          float headerX, float laneX, float y, float headerW, float laneW, float h,
                                          float clipH, boolean overlayOpen, int mouseX, int mouseY) {
        long outDur = project != null && !project.clips.isEmpty()
                ? Math.max(1L, project.outputDurationMillis())
                : Math.max(1, sourceDuration);
        long playOut = project != null ? DirectorCamera.currentOutputTime() : player.elapsedMillis();
        if (needsFit || ppm <= 0f) {
            fitView(laneW, outDur);
            needsFit = false;
        }
        clampView(outDur, laneW);

        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (dragging(screen, DRAG_PAN) && ppm > 0f) {
            view0 -= (mouseX - lastPanMouseX) / ppm;
            lastPanMouseX = mouseX;
            clampView(outDur, laneW);
        }
        if (dragging(screen, DRAG_SCROLL) && ppm > 0f) {
            float visible = laneW / ppm;
            float maxView = Math.max(0f, outDur - visible);
            float thumb = maxView <= 0f ? laneW : Math.max(18f, laneW * (visible / Math.max(1L, outDur)));
            float span = Math.max(1f, laneW - thumb);
            view0 = ((mouseX - laneX) - thumb / 2f) / span * maxView;
            clampView(outDur, laneW);
        }

        if (dragging(screen, DRAG_HEAD)) {
            scrubbing = true;
            scrubOutputMillis = tAt(laneX, mouseX, outDur);
        }
        if (scrubbing) {
            if (screen == null) {
                scrubbing = false;
            } else if (!dragging(screen, DRAG_HEAD)) {
                long dest = scrubOutputMillis;
                scrubbing = false;
                releaseDrag(screen, DRAG_HEAD);
                DirectorCamera.seekToOutput(dest);
            }
        }
        long displayedOut = scrubbing ? scrubOutputMillis : playOut;

        Rects.rounded(headerX, y, headerW, h, 3, new Color(10, 10, 12, 200).getRGB(), false);
        Rects.rounded(laneX, y, laneW, h, 3, new Color(8, 8, 10, 200).getRGB(), false);

        Rects.rounded(laneX, y, laneW, DirectorUi.RULER_H, 2, ClickGuiTheme.layer().getRGB(), false);
        drawRuler(laneX, y, laneW, outDur);

        float laneY = y + DirectorUi.RULER_H + 1f;
        float propsTop = laneY + clipH + 2f;
        FPSMaster.fontManager.getFont(10).drawString(FPSMaster.i18n.get("edit.track.footage"),
                headerX + 5f, laneY + clipH / 2f - 3f, ClickGuiTheme.textSecondary().getRGB());
        Rects.rounded(laneX, laneY, laneW, clipH, 3, new Color(10, 10, 12, 180).getRGB(), false);

        if (project != null) {
            long acc = 0;
            for (int i = 0; i < project.clips.size(); i++) {
                EditClip clip = project.clips.get(i);
                float cx = xOf(laneX, acc);
                float cw = Math.max(4f, (float) clip.outputLength() * ppm - 1f);
                if (cx + cw < laneX || cx > laneX + laneW) {
                    acc += clip.outputLength();
                    continue;
                }
                boolean sel = i == selectedClip;
                Color fill = new Color(CLIP_COLORS[i % CLIP_COLORS.length], true);
                if (!sel) {
                    fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 200);
                }
                float visX = Math.max(cx, laneX);
                float visW = Math.min(cx + cw, laneX + laneW) - visX;
                if (visW > 1f) {
                    Rects.rounded(visX, laneY + 2f, visW, clipH - 4f, 3, fill.getRGB(), false);
                    if (sel) {
                        Rects.rounded(visX - 0.5f, laneY + 1.5f, visW + 1f, clipH - 3f, 3, 0x66FFFFFF, false);
                    }
                }
                if (clip.hasCurve()) {
                    drawSpeedCurve(screen, clip, i, sel, cx, laneY + 2f, cw, clipH - 4f, overlayOpen, mouseX, mouseY);
                } else if (cw > 28f && visW > 20f) {
                    String label = clip.name == null || clip.name.isEmpty()
                            ? ReplayScreen.formatDuration(clip.sourceLength())
                            : clip.name;
                    FPSMaster.fontManager.getFont(11).drawString(
                            FPSMaster.fontManager.getFont(11).trimStringToWidth(label, visW - 16f),
                            visX + 5f, laneY + clipH / 2f - 3f, 0xFFFFFFFF);
                    if (clip.speed != 1f && visW > 40f) {
                        String sp = formatSpeed(clip.speed);
                        FPSMaster.fontManager.getFont(10).drawString(sp,
                                visX + visW - 4f - FPSMaster.fontManager.getFont(10).getStringWidth(sp),
                                laneY + 3f, 0xCCFFFFFF);
                    }
                }
                boolean hoverLeft = Hover.is(cx, laneY, DirectorUi.TRIM, clipH, mouseX, mouseY);
                boolean hoverRight = Hover.is(cx + cw - DirectorUi.TRIM, laneY, DirectorUi.TRIM, clipH, mouseX, mouseY);
                if ((hoverLeft || (dragging(screen, DRAG_TRIM) && draggingTrimLeft && draggingClip == i))
                        && visW > 2f) {
                    Rects.fill(visX, laneY + 3f, 2f, clipH - 6f, 0xFFFFFFFF);
                }
                if ((hoverRight || (dragging(screen, DRAG_TRIM) && !draggingTrimLeft && draggingClip == i))
                        && visW > 2f) {
                    Rects.fill(Math.min(cx + cw, laneX + laneW) - 2f, laneY + 3f, 2f, clipH - 6f, 0xFFFFFFFF);
                }
                acc += clip.outputLength();

                if (overlayOpen || scrubbing) {
                    continue;
                }
                if (razor) {
                    if (pressed(screen, cx, laneY, cw, clipH, 0)) {
                        selectedClip = i;
                        selectedProp = null;
                        DirectorCamera.noteBeforeChange();
                        project.splitAtOutput(tAt(laneX, mouseX, outDur));
                        save();
                    }
                    continue;
                }
                if (pressed(screen, cx, laneY, DirectorUi.TRIM, clipH, 0)) {
                    selectedClip = i;
                    DirectorCamera.noteBeforeChange();
                    if (acquireDrag(screen, DRAG_TRIM, 0)) {
                        draggingTrimLeft = true;
                        draggingClip = i;
                    }
                } else if (pressed(screen, cx + cw - DirectorUi.TRIM, laneY, DirectorUi.TRIM, clipH, 0)) {
                    selectedClip = i;
                    DirectorCamera.noteBeforeChange();
                    if (acquireDrag(screen, DRAG_TRIM, 0)) {
                        draggingTrimLeft = false;
                        draggingClip = i;
                    }
                } else if (pressed(screen, cx, laneY, cw, clipH, 1)) {
                    selectedClip = i;
                    menuOnKeyframe = false;
                    menuClip = i;
                    menuX = mouseX;
                    menuY = mouseY - 4f;
                } else if (pressed(screen, cx, laneY, cw, clipH, 0)) {
                    selectedClip = i;
                    selectedProp = null;
                    menuClip = -1;
                    DirectorCamera.noteBeforeChange();
                    if (acquireDrag(screen, DRAG_CLIP, 0)) {
                        draggingClip = i;
                    }
                }
            }
        }

        if (dragging(screen, DRAG_TRIM) && draggingClip >= 0 && project != null
                && draggingClip < project.clips.size()) {
            EditClip clip = project.clips.get(draggingClip);
            long start = project.outputStartOf(draggingClip);
            long localOut = tAt(laneX, mouseX, outDur) - start;
            int src = clip.srcIn + clip.sourceOffsetForOutput(Math.max(0L, localOut));
            if (draggingTrimLeft) {
                project.trimSource(draggingClip, Math.min(src, clip.srcOut - EditProject.MIN_CLIP_SOURCE), clip.srcOut);
            } else {
                project.trimSource(draggingClip, clip.srcIn, Math.max(src, clip.srcIn + EditProject.MIN_CLIP_SOURCE));
            }
        } else if (dragging(screen, DRAG_CLIP) && project != null && draggingClip >= 0) {
            insertClip = project.clipIndexAtOutput(tAt(laneX, mouseX, outDur));
            long mark = project.outputStartOf(insertClip);
            if (insertClip > draggingClip) {
                mark = project.outputStartOf(insertClip) + project.clips.get(insertClip).outputLength();
            }
            float mx = xOf(laneX, mark);
            if (mx >= laneX && mx <= laneX + laneW) {
                Rects.fill(mx - 0.75f, laneY, 1.5f, clipH, ClickGuiTheme.accent().getRGB());
            }
        } else if (draggingClip >= 0) {
            if (insertClip >= 0 && insertClip != draggingClip && project != null) {
                project.moveClip(draggingClip, insertClip);
                selectedClip = insertClip;
            }
            save();
            draggingClip = -1;
            insertClip = -1;
            draggingTrimLeft = false;
            releaseDrag(screen, DRAG_TRIM);
            releaseDrag(screen, DRAG_CLIP);
        }

        drawPropertyLanes(screen, player, project, track, outDur, displayedOut,
                headerX, laneX, propsTop, headerW, laneW, overlayOpen, mouseX, mouseY);

        if (!dragging(screen, DRAG_CURVE) && curveDirty) {
            curveDirty = false;
            save();
            releaseDrag(screen, DRAG_CURVE);
        }

        // Keys and clips already had their chance. Playhead lives on the ruler so a key sitting
        // on the current time is still clickable. Shift/Alt/middle-drag pans the view.
        float playX = xOf(laneX, displayedOut);
        if (!overlayOpen && !shift && !alt && !dragging(screen, DRAG_PAN) && !dragging(screen, DRAG_KF)
                && !dragging(screen, DRAG_CLIP) && !dragging(screen, DRAG_TRIM)
                && !dragging(screen, DRAG_CURVE) && !dragging(screen, DRAG_SCROLL)) {
            if (beginDrag(screen, DRAG_HEAD, playX - 4f, y, 8f, DirectorUi.RULER_H + 3f)
                    || beginDrag(screen, DRAG_HEAD, laneX, y, laneW, DirectorUi.RULER_H)) {
                scrubbing = true;
                scrubOutputMillis = tAt(laneX, mouseX, outDur);
                displayedOut = scrubOutputMillis;
                playX = xOf(laneX, displayedOut);
            }
        }
        if (!overlayOpen && !scrubbing && !dragging(screen, DRAG_HEAD) && !dragging(screen, DRAG_KF)
                && !dragging(screen, DRAG_CLIP) && !dragging(screen, DRAG_TRIM)
                && !dragging(screen, DRAG_CURVE)) {
            if (beginDrag(screen, DRAG_PAN, 2, laneX, y, laneW, h)
                    || ((shift || alt) && beginDrag(screen, DRAG_PAN, laneX, y, laneW, h))) {
                lastPanMouseX = mouseX;
            }
        }

        drawScrollBar(screen, laneX, y + h - DirectorUi.SCROLL_H, laneW, outDur, mouseX, mouseY);

        if (playX >= laneX - 1f && playX <= laneX + laneW + 1f) {
            DirectorUi.playhead(playX, y, h - DirectorUi.SCROLL_H, scrubbing);
            if (scrubbing) {
                String clock = ReplayScreen.formatDuration(displayedOut);
                float clockW = FPSMaster.fontManager.getFont(10).getStringWidth(clock);
                float clockX = playX + 7f;
                if (clockX + clockW > laneX + laneW) {
                    clockX = playX - 7f - clockW;
                }
                FPSMaster.fontManager.getFont(10).drawString(clock, clockX, y + 2f,
                        ClickGuiTheme.accentText().getRGB());
            }
        }
        if (player.isSeeking()) {
            FPSMaster.fontManager.getFont(10).drawCenteredString(
                    "seeking " + Math.round(player.seekProgress() * 100f) + "%",
                    laneX + laneW / 2f, y + 2f, ClickGuiTheme.accentText().getRGB());
        }
    }

    private static void drawRuler(float x, float y, float w, long outDur) {
        long step = 1000;
        long[] steps = {200, 500, 1000, 2000, 5000, 10_000, 15_000, 30_000, 60_000};
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] * ppm >= 36f) {
                step = steps[i];
                break;
            }
            step = steps[i];
        }
        long t0 = Math.max(0L, ((long) view0 / step) * step);
        for (long t = t0; t <= outDur; t += step) {
            float px = xOf(x, t);
            if (px > x + w) {
                break;
            }
            if (px < x) {
                continue;
            }
            FPSMaster.fontManager.getFont(10).drawString(ReplayScreen.formatDuration(t),
                    px + 2f, y + 2f, ClickGuiTheme.textDisabled().getRGB());
        }
    }

    private static void fitView(float w, long outDur) {
        view0 = 0f;
        ppm = w / (float) Math.max(1L, outDur);
        needsFit = false;
    }

    private static void clampView(long outDur, float w) {
        float minPpm = w / (Math.max(1L, outDur) * 4f);
        float maxPpm = 0.5f;
        if (ppm < minPpm) {
            ppm = minPpm;
        }
        if (ppm > maxPpm) {
            ppm = maxPpm;
        }
        float maxView = Math.max(0f, outDur - w / ppm);
        if (view0 < 0f) {
            view0 = 0f;
        }
        if (view0 > maxView) {
            view0 = maxView;
        }
    }

    private static void zoomView(float originX, float mx, float w, long outDur, int wheel) {
        if (ppm <= 0f) {
            fitView(w, outDur);
        }
        long t = tAt(originX, mx, outDur);
        float factor = wheel > 0 ? 1.18f : (1f / 1.18f);
        ppm *= factor;
        clampView(outDur, w);
        view0 = t - (mx - originX) / ppm;
        clampView(outDur, w);
    }

    private static void panView(float w, long outDur, int wheel) {
        if (ppm <= 0f) {
            return;
        }
        float visible = w / ppm;
        float notches;
        if (Math.abs(wheel) >= 120) {
            notches = wheel / 120f;
        } else {
            notches = wheel > 0 ? 1f : -1f;
        }
        view0 -= notches * visible * 0.18f;
        clampView(outDur, w);
    }

    private static void drawScrollBar(ScaledGuiScreen screen, float x, float y, float w, long outDur,
                                      int mouseX, int mouseY) {
        if (ppm <= 0f || w <= 8f) {
            return;
        }
        float visible = w / ppm;
        float maxView = Math.max(0f, outDur - visible);
        Rects.rounded(x, y, w, DirectorUi.SCROLL_H, 2, ClickGuiTheme.layer().getRGB(), false);
        if (maxView <= 1f) {
            Rects.rounded(x, y, w, DirectorUi.SCROLL_H, 2, ClickGuiTheme.layerActive().getRGB(), false);
            return;
        }
        float thumb = Math.max(18f, w * (visible / Math.max(1L, outDur)));
        float tx = x + (w - thumb) * (view0 / maxView);
        boolean hover = Hover.is(x, y - 1f, w, DirectorUi.SCROLL_H + 2f, mouseX, mouseY)
                || dragging(screen, DRAG_SCROLL);
        Rects.rounded(tx, y, thumb, DirectorUi.SCROLL_H, 2,
                (hover ? ClickGuiTheme.accent() : ClickGuiTheme.strokeStrong()).getRGB(), false);
        if (beginDrag(screen, DRAG_SCROLL, x, y - 2f, w, DirectorUi.SCROLL_H + 4f)) {
            view0 = ((mouseX - x) - thumb / 2f) / Math.max(1f, w - thumb) * maxView;
            clampView(outDur, w);
        }
    }

    private static float xOf(float originX, long time) {
        return originX + (time - view0) * ppm;
    }

    private static long tAt(float originX, float mx, long outDur) {
        if (ppm <= 0f) {
            return 0L;
        }
        long t = (long) (view0 + (mx - originX) / ppm);
        if (t < 0L) {
            return 0L;
        }
        if (t > outDur) {
            return outDur;
        }
        return t;
    }

    private static int keyTime(ReplayPlayer player) {
        EditProject project = DirectorCamera.project();
        if (project == null || project.clips.isEmpty()) {
            return player.elapsedMillis();
        }
        return project.mapOutputToSource(DirectorCamera.currentOutputTime());
    }

    private static int sourceAtOutput(EditProject project, long outputMillis) {
        if (project == null || project.clips.isEmpty()) {
            return (int) outputMillis;
        }
        return project.mapOutputToSource(outputMillis);
    }

    private static long outputForSource(EditProject project, int sourceMillis) {
        if (project == null || project.clips.isEmpty()) {
            return sourceMillis;
        }
        int idx = indexContainingSource(project, sourceMillis);
        if (idx < 0) {
            return -1;
        }
        return project.outputTimeFor(idx, sourceMillis);
    }

    private static void drawInspector(ScaledGuiScreen screen, float x, float y, float w, int mouseX, int mouseY) {
        Rects.rounded(x, y, w, DirectorUi.INSPECT_H, 3, ClickGuiTheme.layer().getRGB(), false);
        String label = FPSMaster.i18n.get(selectedChannel.i18n);
        FPSMaster.fontManager.getFont(11).drawString(label, x + 6f, y + 3.5f, ClickGuiTheme.textSecondary().getRGB());
        CameraKeyframe.Easing[] easings = CameraKeyframe.Easing.values();
        String[] easeLabels = new String[easings.length];
        int easeIndex = 0;
        for (int i = 0; i < easings.length; i++) {
            easeLabels[i] = FPSMaster.i18n.get(easingKey(easings[i]));
            if (selectedProp.easing == easings[i]) {
                easeIndex = i;
            }
        }
        DirectorUi.enumSeg(screen, x + 44f, y + 1.5f, FPSMaster.i18n.get("director.easing"), easeLabels,
                easeIndex, mouseX, mouseY, new DirectorUi.EnumPick() {
                    @Override
                    public void pick(int index) {
                        DirectorCamera.noteBeforeChange();
                        selectedProp.easing = CameraKeyframe.Easing.values()[index];
                        save();
                    }
                });
        if (selectedChannel == CameraChannel.POSITION) {
            CameraKeyframe.Transition[] paths = CameraKeyframe.Transition.values();
            String[] pathLabels = new String[paths.length];
            int pathIndex = 0;
            for (int i = 0; i < paths.length; i++) {
                pathLabels[i] = FPSMaster.i18n.get("director.transition." + paths[i].name().toLowerCase());
                if (selectedProp.path == paths[i]) {
                    pathIndex = i;
                }
            }
            DirectorUi.enumSeg(screen, x + w * 0.52f, y + 1.5f, FPSMaster.i18n.get("director.transition"), pathLabels,
                    pathIndex, mouseX, mouseY, new DirectorUi.EnumPick() {
                        @Override
                        public void pick(int index) {
                            DirectorCamera.noteBeforeChange();
                            selectedProp.path = CameraKeyframe.Transition.values()[index];
                            save();
                        }
                    });
        }
        if (selectedChannel == CameraChannel.FOV) {
            float fov = selectedProp.a;
            String fovText = Math.round(fov) + "°";
            float tx = x + w - 92f;
            FPSMaster.fontManager.getFont(11).drawString(fovText, tx, y + 3.5f,
                    ClickGuiTheme.textPrimary().getRGB());
            float slX = tx + 22f;
            float slW = 56f;
            float t = (fov - FOV_MIN) / (FOV_MAX - FOV_MIN);
            if (beginDrag(screen, DRAG_FOV, slX - 2f, y, slW + 4f, DirectorUi.INSPECT_H)) {
                t = Math.max(0f, Math.min(1f, (mouseX - slX) / slW));
                applyFovEdit(FOV_MIN + t * (FOV_MAX - FOV_MIN));
            }
            UiChrome.slider(slX, y + 1.5f, slW, t, true);
        }
    }

    private static void drawPropertyLanes(ScaledGuiScreen screen, ReplayPlayer player, EditProject project,
                                          CameraTrack track, long outDur, long displayedOut,
                                          float headerX, float laneX, float y, float headerW, float laneW,
                                          boolean overlayOpen, int mouseX, int mouseY) {
        CameraChannel[] channels = CameraChannel.values();
        int sourceNow = sourceAtOutput(project, displayedOut);
        if (!scrubbing && draggingKeyframe && selectedProp != null) {
            if (dragging(screen, DRAG_KF)) {
                long out = tAt(laneX, mouseX, outDur);
                selectedProp.timeMillis = sourceAtOutput(project, out);
            } else {
                draggingKeyframe = false;
                releaseDrag(screen, DRAG_KF);
                track.sort();
                save();
            }
        }
        for (int row = 0; row < channels.length; row++) {
            CameraChannel channel = channels[row];
            float ry = y + row * DirectorUi.PROP_ROW;
            boolean on = selectedChannel == channel;
            PropKeyframe atHead = track.nearest(channel, sourceNow, DirectorCamera.MERGE_WINDOW_MILLIS);
            boolean keyedHere = atHead != null;
            Rects.rounded(headerX, ry, headerW, DirectorUi.PROP_ROW - 1f, 2,
                    (on ? ClickGuiTheme.layerActive() : ClickGuiTheme.layer()).getRGB(), false);
            if (on) {
                UiChrome.accentMark(headerX, ry + 2f, DirectorUi.PROP_ROW - 5f);
            }
            float dx = headerX + 10f;
            float dy = ry + (DirectorUi.PROP_ROW - 1f) / 2f;
            if (keyedHere) {
                DirectorUi.fillDiamond(dx, dy, 3.4f, DirectorUi.KEY_FILL);
            } else {
                DirectorUi.strokeDiamond(dx, dy, 3.4f, track.channel(channel).isEmpty()
                        ? ClickGuiTheme.textDisabled().getRGB() : DirectorUi.KEY_FILL);
            }
            FPSMaster.fontManager.getFont(10).drawString(FPSMaster.i18n.get(channel.i18n),
                    headerX + 16f, ry + 2.5f, ClickGuiTheme.textSecondary().getRGB());
            Rects.rounded(laneX, ry, laneW, DirectorUi.PROP_ROW - 1f, 2,
                    (on ? ClickGuiTheme.layerActive() : ClickGuiTheme.layer()).getRGB(), false);

            if (!overlayOpen && !scrubbing && pressed(screen, headerX, ry, 18f, DirectorUi.PROP_ROW, 0)) {
                selectedChannel = channel;
                if (atHead != null) {
                    DirectorCamera.noteBeforeChange();
                    track.remove(channel, atHead);
                    if (selectedProp == atHead) {
                        selectedProp = null;
                    }
                    save();
                } else if (!ReplayPlayer.instance().isPossessing()) {
                    CameraPose pose = DirectorCamera.capturePose();
                    if (pose != null) {
                        DirectorCamera.noteBeforeChange();
                        selectedProp = track.add(channel, sourceNow, pose,
                                DirectorCamera.MERGE_WINDOW_MILLIS);
                        selectedClip = -1;
                        save();
                    }
                }
            } else if (!overlayOpen && !scrubbing && pressed(screen, headerX, ry, headerW, DirectorUi.PROP_ROW, 0)) {
                selectedChannel = channel;
                if (atHead != null) {
                    selectedProp = atHead;
                    selectedClip = -1;
                }
            }

            List<PropKeyframe> keys = track.channel(channel);
            for (int i = 0; i < keys.size(); i++) {
                PropKeyframe key = keys.get(i);
                long out = outputForSource(project, key.timeMillis);
                if (out < 0) {
                    continue;
                }
                float kx = xOf(laneX, out);
                if (kx < laneX - 4f || kx > laneX + laneW + 4f) {
                    continue;
                }
                boolean sel = key == selectedProp && channel == selectedChannel;
                float radius = sel ? 4.2f : 3.4f;
                int color = sel ? ClickGuiTheme.accent().getRGB() : DirectorUi.KEY_FILL;
                DirectorUi.fillDiamond(kx, ry + (DirectorUi.PROP_ROW - 1f) / 2f, radius, color);
                if (Hover.is(kx - 5f, ry, 10f, DirectorUi.PROP_ROW, mouseX, mouseY)) {
                    DirectorUi.tip(keyTip(channel, key), kx, ry);
                }
                if (overlayOpen || scrubbing) {
                    continue;
                }
                if (pressed(screen, kx - 5f, ry, 10f, DirectorUi.PROP_ROW, 0)) {
                    selectedChannel = channel;
                    selectedProp = key;
                    selectedClip = -1;
                    if (acquireDrag(screen, DRAG_KF, 0)) {
                        DirectorCamera.noteBeforeChange();
                        draggingKeyframe = true;
                    }
                } else if (pressed(screen, kx - 5f, ry, 10f, DirectorUi.PROP_ROW, 1)) {
                    selectedChannel = channel;
                    selectedProp = key;
                    menuOnKeyframe = true;
                    menuClip = 0;
                    menuX = mouseX;
                    menuY = mouseY;
                }
            }
        }
    }

    private static String keyTip(CameraChannel channel, PropKeyframe key) {
        String time = ReplayScreen.formatDuration(key.timeMillis);
        if (channel == CameraChannel.POSITION) {
            return String.format("%.1f  %.1f  %.1f  ·  %s", key.a, key.b, key.c, time);
        }
        return Math.round(key.a) + "°  ·  " + time;
    }

    private static int indexContainingSource(EditProject project, int source) {
        for (int i = 0; i < project.clips.size(); i++) {
            EditClip clip = project.clips.get(i);
            if (source >= clip.srcIn && source <= clip.srcOut) {
                return i;
            }
        }
        return -1;
    }

    private static boolean clipHasCurve(EditProject project, int index) {
        return project != null && index >= 0 && index < project.clips.size()
                && project.clips.get(index).hasCurve();
    }

    private static void drawSpeedCurve(ScaledGuiScreen screen, EditClip clip, int clipIndex, boolean sel,
                                       float cx, float cy, float cw, float ch, boolean overlayOpen,
                                       int mouseX, int mouseY) {
        int srcLen = Math.max(1, clip.sourceLength());
        long outLen = Math.max(1L, clip.outputLength());
        float y1 = cy + speedToY(1f, ch);
        Rects.fill(cx + 2f, y1, Math.max(1f, cw - 4f), 0.6f, 0x55FFFFFF);
        float prevX = -1f;
        float prevY = 0f;
        for (int step = 0; step <= 24; step++) {
            float u = step / 24f;
            float px = cx + cw * (clip.outputOffsetForSource((int) (u * srcLen)) / (float) outLen);
            float py = cy + speedToY(clip.speedAt(u), ch);
            if (prevX >= 0f) {
                DirectorUi.line(prevX, prevY, px, py, 0xEEFFFFFF);
            }
            prevX = px;
            prevY = py;
        }
        if (!sel || clip.curve == null) {
            return;
        }
        if (dragging(screen, DRAG_CURVE) && selectedClip == clipIndex && selectedPoint >= 0
                && selectedPoint < clip.curve.size()) {
            SpeedPoint point = clip.curve.get(selectedPoint);
            float u = outputXToU(clip, (mouseX - cx) / Math.max(1f, cw));
            float s = yToSpeed(mouseY - cy, ch);
            if (dragHandle == 0) {
                if (selectedPoint > 0 && selectedPoint < clip.curve.size() - 1) {
                    float lo = clip.curve.get(selectedPoint - 1).p + 0.03f;
                    float hi = clip.curve.get(selectedPoint + 1).p - 0.03f;
                    point.p = Math.max(lo, Math.min(hi, u));
                }
                point.s = s;
            } else if (dragHandle < 0) {
                point.inDx = u - point.p;
                point.inDy = s - point.s;
            } else {
                point.outDx = u - point.p;
                point.outDy = s - point.s;
            }
            curveDirty = true;
            return;
        }
        for (int i = 0; i < clip.curve.size(); i++) {
            SpeedPoint point = clip.curve.get(i);
            float px = cx + cw * (clip.outputOffsetForSource((int) (point.p * srcLen)) / (float) outLen);
            float py = cy + speedToY(point.s, ch);
            if (i == selectedPoint) {
                float inX = cx + cw * (clip.outputOffsetForSource((int) (clamp01(point.p + point.inDx) * srcLen)) / (float) outLen);
                float inY = cy + speedToY(EditClip.clampSpeed(point.s + point.inDy), ch);
                float outX = cx + cw * (clip.outputOffsetForSource((int) (clamp01(point.p + point.outDx) * srcLen)) / (float) outLen);
                float outY = cy + speedToY(EditClip.clampSpeed(point.s + point.outDy), ch);
                DirectorUi.line(px, py, inX, inY, 0xAAFFFFFF);
                DirectorUi.line(px, py, outX, outY, 0xAAFFFFFF);
                Rects.rounded(inX - 2.2f, inY - 2.2f, 4.4f, 4.4f, 2, 0xFFFFFFFF);
                Rects.rounded(outX - 2.2f, outY - 2.2f, 4.4f, 4.4f, 2, 0xFFFFFFFF);
                if (!overlayOpen && pressed(screen, inX - 4f, inY - 4f, 8f, 8f, 0)) {
                    selectedPoint = i;
                    dragHandle = -1;
                    acquireDrag(screen, DRAG_CURVE, 0);
                    return;
                }
                if (!overlayOpen && pressed(screen, outX - 4f, outY - 4f, 8f, 8f, 0)) {
                    selectedPoint = i;
                    dragHandle = 1;
                    acquireDrag(screen, DRAG_CURVE, 0);
                    return;
                }
            }
            Rects.rounded(px - 2.6f, py - 2.6f, 5.2f, 5.2f, 2,
                    i == selectedPoint ? ClickGuiTheme.accent().getRGB() : 0xFFFFFFFF);
            if (!overlayOpen && pressed(screen, px - 4f, py - 4f, 8f, 8f, 0)) {
                selectedClip = clipIndex;
                selectedPoint = i;
                dragHandle = 0;
                acquireDrag(screen, DRAG_CURVE, 0);
                return;
            }
        }
        if (!overlayOpen && pressed(screen, cx, cy, cw, ch, 0)) {
            float u = outputXToU(clip, (mouseX - cx) / Math.max(1f, cw));
            SpeedPoint added = clip.addCurvePoint(u, yToSpeed(mouseY - cy, ch));
            selectedClip = clipIndex;
            selectedPoint = clip.curve.indexOf(added);
            save();
        }
        if (!dragging(screen, DRAG_CURVE) && dragHandle != 0 && selectedPoint >= 0) {
            dragHandle = 0;
            releaseDrag(screen, DRAG_CURVE);
            save();
        }
    }

    private static float outputXToU(EditClip clip, float x01) {
        long local = (long) (clamp01(x01) * clip.outputLength());
        int src = clip.sourceOffsetForOutput(local);
        int len = Math.max(1, clip.sourceLength());
        return Math.max(0f, Math.min(1f, src / (float) len));
    }

    private static float speedToY(float speed, float h) {
        double u = (Math.log(Math.max(SPEED_Y_MIN, Math.min(SPEED_Y_MAX, speed))) / Math.log(2) + 2.0) / 5.0;
        return (float) ((1.0 - u) * Math.max(4f, h - 4f) + 2f);
    }

    private static float yToSpeed(float y, float h) {
        float t = 1f - Math.max(0f, Math.min(1f, (y - 2f) / Math.max(4f, h - 4f)));
        float s = (float) Math.pow(2.0, t * 5.0 - 2.0);
        return EditClip.clampSpeed(s);
    }

    private static void drawContextMenu(ScaledGuiScreen screen, EditProject project, CameraTrack track,
                                        int mouseX, int mouseY) {
        if (menuOnKeyframe) {
            float w = 88f;
            float h = 36f;
            float x = menuX;
            float y = menuY - h;
            if (y < 8f) {
                y = menuY + 6f;
            }
            UiChrome.panel(x, y, w, h, 6);
            if (DirectorUi.menuRow(screen, x, y + 4f, w, FPSMaster.i18n.get("director.key.delete"), false, mouseX, mouseY)) {
                deleteSelected();
                menuClip = -1;
                menuOnKeyframe = false;
                return;
            }
            if (DirectorUi.menuRow(screen, x, y + 18f, w, FPSMaster.i18n.get("edit.roll.reset"), false, mouseX, mouseY)) {
                DirectorCamera.resetRoll();
                if (selectedProp != null && selectedChannel == CameraChannel.ROLL) {
                    DirectorCamera.noteBeforeChange();
                    selectedProp.a = 0f;
                    save();
                }
                menuClip = -1;
                menuOnKeyframe = false;
                return;
            }
            if (DirectorUi.outside(screen, x, y, w, h)) {
                menuClip = -1;
                menuOnKeyframe = false;
            }
            return;
        }
        if (menuClip < 0 || menuClip >= project.clips.size()) {
            menuClip = -1;
            return;
        }
        EditClip clip = project.clips.get(menuClip);
        float itemH = 13f;
        int extra = 5;
        float w = 108f;
        float h = 14f + SPEED_STEPS.length * itemH + extra * itemH + 10f;
        float x = menuX;
        float y = menuY - h;
        if (y < 8f) {
            y = menuY + 6f;
        }
        UiChrome.panel(x, y, w, h, 6);
        FPSMaster.fontManager.getFont(10).drawString(FPSMaster.i18n.get("director.seg.speed"),
                x + 7f, y + 5f, ClickGuiTheme.textDisabled().getRGB());
        float iy = y + 14f;
        for (int i = 0; i < SPEED_STEPS.length; i++) {
            boolean on = !clip.hasCurve() && clip.speed == SPEED_STEPS[i];
            if (DirectorUi.menuRow(screen, x, iy, w, formatSpeed(SPEED_STEPS[i])
                    + (SPEED_STEPS[i] > 1f ? "  " + FPSMaster.i18n.get("edit.speed.up")
                    : SPEED_STEPS[i] < 1f ? "  " + FPSMaster.i18n.get("edit.speed.down") : ""), on, mouseX, mouseY)) {
                DirectorCamera.noteBeforeChange();
                project.setSpeed(menuClip, SPEED_STEPS[i]);
                selectedPoint = -1;
                save();
                menuClip = -1;
                return;
            }
            iy += itemH;
        }
        iy += 2f;
        Rects.fill(x + 6f, iy, w - 12f, 0.5f, ClickGuiTheme.stroke().getRGB());
        iy += 3f;
        if (DirectorUi.menuRow(screen, x, iy, w, FPSMaster.i18n.get("edit.remap"), clip.hasCurve(), mouseX, mouseY)) {
            DirectorCamera.noteBeforeChange();
            project.toggleCurve(menuClip);
            selectedClip = menuClip;
            selectedPoint = 0;
            save();
            menuClip = -1;
            return;
        }
        iy += itemH;
        if (clip.hasCurve() && DirectorUi.menuRow(screen, x, iy, w, FPSMaster.i18n.get("edit.remap.add"), false, mouseX, mouseY)) {
            clip.addCurvePoint(0.5f, clip.speedAt(0.5f));
            save();
            menuClip = -1;
            return;
        }
        if (clip.hasCurve()) {
            iy += itemH;
        }
        if (DirectorUi.menuRow(screen, x, iy, w, FPSMaster.i18n.get("director.cut.split"), false, mouseX, mouseY)) {
            splitAtPlayhead();
            menuClip = -1;
            return;
        }
        iy += itemH;
        if (DirectorUi.menuRow(screen, x, iy, w, FPSMaster.i18n.get("edit.duplicate"), false, mouseX, mouseY)) {
            selectedClip = menuClip;
            duplicateSelectedClip();
            menuClip = -1;
            return;
        }
        iy += itemH;
        if (DirectorUi.menuRow(screen, x, iy, w, FPSMaster.i18n.get("director.key.delete"), false, mouseX, mouseY)) {
            selectedClip = menuClip;
            deleteSelected();
            menuClip = -1;
            return;
        }
        if (DirectorUi.outside(screen, x, y, w, h)) {
            menuClip = -1;
        }
    }

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
        String[] rateLabels = {"24", "30", "60"};
        int rateIndex = exportFps == 24 ? 0 : exportFps == 30 ? 1 : 2;
        DirectorUi.enumSeg(screen, x + 11f, y + 24f, FPSMaster.i18n.get("director.export.fps"), rateLabels,
                rateIndex, mouseX, mouseY, new DirectorUi.EnumPick() {
                    @Override
                    public void pick(int index) {
                        exportFps = new int[]{24, 30, 60}[index];
                    }
                });
        String[] resLabels = new String[RESOLUTIONS.length];
        resLabels[0] = FPSMaster.i18n.get("director.export.res.window");
        for (int idx = 1; idx < RESOLUTIONS.length; idx++) {
            resLabels[idx] = RESOLUTIONS[idx][1] + "p";
        }
        DirectorUi.enumSeg(screen, x + 11f, y + 42f, FPSMaster.i18n.get("director.export.res"), resLabels,
                exportResolution, mouseX, mouseY, new DirectorUi.EnumPick() {
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
        if (DirectorUi.outside(screen, x, y, w, h)) {
            exportDialogOpen = false;
        }
    }

    private static boolean pressed(ScaledGuiScreen screen, float x, float y, float w, float h, int button) {
        return DirectorUi.click(screen, x, y, w, h, button);
    }

    private static boolean dragging(ScaledGuiScreen screen, Object token) {
        return screen != null && screen.isDragging(token);
    }

    private static boolean beginDrag(ScaledGuiScreen screen, Object token, float x, float y, float w, float h) {
        return screen != null && screen.beginDrag(token, x, y, w, h);
    }

    private static boolean beginDrag(ScaledGuiScreen screen, Object token, int button,
                                     float x, float y, float w, float h) {
        return screen != null && screen.beginDrag(token, button, x, y, w, h);
    }

    private static boolean acquireDrag(ScaledGuiScreen screen, Object token, int button) {
        return screen != null && screen.acquireDrag(token, button);
    }

    private static void releaseDrag(ScaledGuiScreen screen, Object token) {
        if (screen != null) {
            screen.releaseDrag(token);
        }
    }

    private static String formatSpeed(float speed) {
        return speed == Math.round(speed) ? Math.round(speed) + "x" : speed + "x";
    }

    private static float clamp01(float t) {
        return t < 0f ? 0f : (t > 1f ? 1f : t);
    }

    private static String easingKey(CameraKeyframe.Easing easing) {
        if (easing == null) {
            return "director.easing.linear";
        }
        switch (easing) {
            case EASE:
                return "director.easing.ease";
            case EASE_IN:
                return "director.easing.in";
            case EASE_OUT:
                return "director.easing.out";
            case EASE_IN_OUT:
                return "director.easing.inout";
            default:
                return "director.easing.linear";
        }
    }

    private static void applyFovEdit(float fov) {
        DirectorCamera.setFlyFov(fov);
        if (selectedProp != null && selectedChannel == CameraChannel.FOV) {
            selectedProp.a = DirectorCamera.flyFov();
            DirectorCamera.markDirty();
        }
    }

    private static void addKeyframe(ReplayPlayer player) {
        if (player == null || !player.isActive() || player.isPossessing()) {
            return;
        }
        CameraPose pose = DirectorCamera.capturePose();
        if (pose == null) {
            return;
        }
        int time = keyTime(player);
        DirectorCamera.noteBeforeChange();
        DirectorCamera.track().addPose(time, pose, DirectorCamera.MERGE_WINDOW_MILLIS);
        selectedChannel = CameraChannel.POSITION;
        selectedProp = DirectorCamera.track().nearest(CameraChannel.POSITION, time,
                DirectorCamera.MERGE_WINDOW_MILLIS);
        selectedClip = -1;
        save();
    }

    private static void save() {
        DirectorCamera.markDirty();
        DirectorCamera.saveIfDirty();
    }

    private static void toast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 1800L;
    }
}
