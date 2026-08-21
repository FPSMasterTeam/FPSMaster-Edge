package top.fpsmaster.ui.screens.replay;

import net.minecraft.client.gui.GuiScreen;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.replay.ReplayFile;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.ReplayRecorder;
import top.fpsmaster.replay.director.DirectorCamera;
import top.fpsmaster.replay.director.EditProject;
import top.fpsmaster.replay.director.EditStore;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Browser for recorded sessions, after docs/prototypes/replay.html: header with the record
 * button, hover-revealed row actions (play / rename / delete), destructive confirm dialog.
 *
 * <p>Reachable from the main menu and from {@code .replay} in game, because the two things it does
 * belong to different moments — you start a recording before joining a match and pick one to watch
 * afterwards, and neither should require being in the other place.
 */
public class ReplayScreen extends ScaledGuiScreen {

    private static final float ROW_HEIGHT = 29f;
    private static final float ROW_GAP = 2.5f;

    private final GuiScreen parent;

    private enum Tab {
        SOURCE, PROJECT
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private final List<ProjectEntry> projects = new ArrayList<ProjectEntry>();
    private Tab tab = Tab.SOURCE;
    private Entry selected;
    private ProjectEntry selectedProject;
    private float scroll;
    private String status = "";
    private int statusColor = ClickGuiTheme.textSecondary().getRGB();

    private enum Dialog {
        NONE, DELETE, RENAME
    }

    private Dialog dialog = Dialog.NONE;
    private Entry dialogTarget;
    private TextField renameField;

    public ReplayScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (renameField == null) {
            renameField = new TextField(FPSMaster.fontManager.getFont(12), false,
                    FPSMaster.i18n.get("replay.rename.placeholder"), 0,
                    ClickGuiTheme.textPrimary().getRGB(), 64);
        }
        refresh();
        refreshProjects();
    }

    public ReplayScreen(GuiScreen parent, boolean showProjects) {
        this(parent);
        if (showProjects) {
            tab = Tab.PROJECT;
        }
    }

    private void refresh() {
        entries.clear();
        selected = null;
        File directory = new File(FileUtils.dir, "replays");
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".edgereplay")) {
                entries.add(new Entry(file));
            }
        }
        scanInBackground(new ArrayList<Entry>(entries));
    }

    private void refreshProjects() {
        projects.clear();
        selectedProject = null;
        for (File file : EditStore.listFiles()) {
            EditProject project = EditStore.load(file);
            if (project == null) {
                continue;
            }
            ProjectEntry row = new ProjectEntry();
            row.file = file;
            row.project = project;
            File source = new File(new File(FileUtils.dir, "replays"), project.source + ".edgereplay");
            row.missingSource = !source.isFile();
            projects.add(row);
        }
    }

    /**
     * Fills in recorder and duration off the client thread.
     *
     * <p>Neither is in a fixed-size header — duration is only known once the last record is reached —
     * so learning it means decompressing the whole file. That is fine on a worker thread and would be
     * a visible freeze on the client one.
     */
    private void scanInBackground(final List<Entry> pending) {
        Thread scanner = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Entry entry : pending) {
                    ReplayFile.Header header = null;
                    try {
                        header = ReplayFile.openForRead(entry.file);
                        entry.recorder = header.recorderName;
                        int last = 0;
                        ReplayFile.Record record;
                        while ((record = ReplayFile.read(header)) != null) {
                            last = record.millis;
                        }
                        entry.durationMillis = last;
                    } catch (Exception failure) {
                        entry.unreadable = failure.getMessage();
                    } finally {
                        if (header != null) {
                            try {
                                header.stream.close();
                            } catch (Exception closeFailure) {
                                ClientLogger.error("replay", "could not close "
                                        + entry.file.getName() + ": " + closeFailure);
                            }
                        }
                    }
                }
            }
        }, "Edge-ReplayScan");
        scanner.setDaemon(true);
        scanner.start();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);

        float panelWidth = Math.min(360f, guiWidth - 24f);
        float panelHeight = Math.min(252f, guiHeight - 32f);
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        UiChrome.panel(panelX, panelY, panelWidth, panelHeight);

        // ---- header ----
        float headH = 38f;
        float hx = panelX + 8f;
        boolean backHover = Hover.is(hx, panelY + 3f, 16f, 16f, mouseX, mouseY);
        UiChrome.ghostButton(hx, panelY + 3f, 16f, 16f, backHover);
        Icons.draw("back", hx + 4.5f, panelY + 7.5f, 7f,
                (backHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(hx, panelY + 3f, 16f, 16f, 0) != null) {
            mc.displayGuiScreen(parent);
        }

        UiChrome.boldString(FPSMaster.fontManager.s16, FPSMaster.i18n.get("replay.title"),
                hx + 21f, panelY + 6.5f, ClickGuiTheme.textPrimary().getRGB());

        // record button + folder button on the right
        ReplayRecorder recorder = ReplayRecorder.instance();
        boolean recording = recorder.isRecording();
        String recLabel = recording
                ? FPSMaster.i18n.get("replay.stop") + " · " + formatDuration(recorder.elapsedMillis())
                : FPSMaster.i18n.get("replay.record");
        float recW = FPSMaster.fontManager.s14.getStringWidth(recLabel) + 26f;
        float recX = panelX + panelWidth - 8f - recW;
        float recY = panelY + 3f;
        if (tab == Tab.SOURCE) {
            boolean recHover = Hover.is(recX, recY, recW, 16f, mouseX, mouseY);
            if (recording) {
                Rects.rounded(recX - 0.5f, recY - 0.5f, recW + 1f, 17f, UiChrome.CTL_RADIUS + 1,
                        new Color(240, 80, 110, 102).getRGB(), false);
                Rects.rounded(recX, recY, recW, 16f, UiChrome.CTL_RADIUS,
                        ClickGuiTheme.dangerSoft().getRGB(), false);
            } else {
                UiChrome.fillButton(recX, recY, recW, 16f, recHover, false);
            }
            float dotAlpha = recording ? (float) (0.35 + 0.65 * Math.abs(Math.sin(System.currentTimeMillis() / 500.0))) : 1f;
            Color dotColor = recording
                    ? new Color(240, 80, 110, (int) (255 * dotAlpha))
                    : Color.WHITE;
            Rects.rounded(recX + 7f, recY + 6f, 4f, 4f, 2, dotColor.getRGB(), false);
            FPSMaster.fontManager.s14.drawString(recLabel, recX + 15f, recY + 4.5f,
                    recording ? ClickGuiTheme.danger().getRGB() : 0xFFFFFFFF);
            if (consumePressInBounds(recX, recY, recW, 16f, 0) != null) {
                toggleRecording();
            }
        }

        float folderX = (tab == Tab.SOURCE ? recX : panelX + panelWidth - 8f) - 5f - 16f;
        boolean folderHover = Hover.is(folderX, recY, 16f, 16f, mouseX, mouseY);
        UiChrome.iconButton(folderX, recY, 16f, folderHover);
        Icons.draw("folder", folderX + 4.5f, recY + 4.5f, 7f,
                (folderHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(folderX, recY, 16f, 16f, 0) != null) {
            if (tab == Tab.PROJECT) {
                openEditsFolder();
            } else {
                openReplaysFolder();
            }
        }

        String srcTab = FPSMaster.i18n.get("edit.tab.source");
        String projTab = FPSMaster.i18n.get("edit.tab.project");
        float srcW = FPSMaster.fontManager.getFont(12).getStringWidth(srcTab) + 10f;
        float projW = FPSMaster.fontManager.getFont(12).getStringWidth(projTab) + 10f;
        float tabX = hx;
        float tabY = panelY + 21f;
        UiChrome.seg(tabX, tabY, srcW + projW + 4f, 13f);
        boolean srcOn = tab == Tab.SOURCE;
        boolean srcHover = Hover.is(tabX + 1.5f, tabY, srcW, 13f, mouseX, mouseY);
        boolean projHover = Hover.is(tabX + 1.5f + srcW + 1f, tabY, projW, 13f, mouseX, mouseY);
        UiChrome.segOption(tabX + 1.5f, tabY + 1.5f, srcW, 10f, srcTab, srcOn, srcHover);
        UiChrome.segOption(tabX + 1.5f + srcW + 1f, tabY + 1.5f, projW, 10f, projTab, !srcOn, projHover);
        if (consumePressInBounds(tabX + 1.5f, tabY, srcW, 13f, 0) != null && tab != Tab.SOURCE) {
            tab = Tab.SOURCE;
            scroll = 0f;
            status = "";
        }
        if (consumePressInBounds(tabX + 1.5f + srcW + 1f, tabY, projW, 13f, 0) != null && tab != Tab.PROJECT) {
            tab = Tab.PROJECT;
            scroll = 0f;
            status = "";
            refreshProjects();
        }
        String countText = tab == Tab.SOURCE
                ? String.format(FPSMaster.i18n.get("replay.count.size"), entries.size(), sourceBytes() / (1024 * 1024))
                : String.format(FPSMaster.i18n.get("edit.project.count"), projects.size());
        FPSMaster.fontManager.getFont(12).drawString(countText,
                tabX + srcW + projW + 10f, tabY + 3f, ClickGuiTheme.textDisabled().getRGB());

        UiChrome.hairlineH(panelX + 1f, panelY + headH, panelWidth - 2f);

        // ---- list ----
        float footH = 16f;
        float listX = panelX + 5f;
        float listY = panelY + headH + 5f;
        float listWidth = panelWidth - 10f;
        float listHeight = panelHeight - headH - 10f - footH;
        if (tab == Tab.SOURCE) {
            drawList(listX, listY, listWidth, listHeight, mouseX, mouseY);
        } else {
            drawProjectList(listX, listY, listWidth, listHeight, mouseX, mouseY);
        }

        // ---- footer ----
        float footY = panelY + panelHeight - footH;
        UiChrome.hairlineH(panelX + 1f, footY, panelWidth - 2f);
        String footText = status.isEmpty()
                ? FPSMaster.i18n.get(tab == Tab.SOURCE ? "replay.foot" : "edit.foot")
                : status;
        int footColor = status.isEmpty() ? ClickGuiTheme.textDisabled().getRGB() : statusColor;
        FPSMaster.fontManager.getFont(11).drawString(
                FPSMaster.fontManager.getFont(11).trimStringToWidth(footText, panelWidth - 16f),
                panelX + 8f, footY + 5f, footColor);

        if (dialog != Dialog.NONE) {
            drawDialog(mouseX, mouseY);
        }
    }

    private void drawList(float x, float y, float width, float height, int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            Icons.draw("film", x + width / 2f - 8f, y + height / 2f - 18f, 16f, ClickGuiTheme.textDisabled().getRGB());
            FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get("replay.empty"),
                    x + width / 2f, y + height / 2f + 2f, ClickGuiTheme.textDisabled().getRGB());
            FPSMaster.fontManager.getFont(12).drawCenteredString(FPSMaster.i18n.get("replay.empty.tip"),
                    x + width / 2f, y + height / 2f + 12f, ClickGuiTheme.textDisabled().getRGB());
            return;
        }

        float contentHeight = entries.size() * (ROW_HEIGHT + ROW_GAP);
        int wheel = dialog == Dialog.NONE ? consumeWheelDelta(x, y, width, height) : 0;
        if (wheel != 0) {
            scroll -= wheel / 120f * (ROW_HEIGHT + ROW_GAP);
        }
        scroll = Math.max(0f, Math.min(Math.max(0f, contentHeight - height), scroll));

        UFontRenderer sub = FPSMaster.fontManager.getFont(12);
        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            float rowY = y + index * (ROW_HEIGHT + ROW_GAP) - scroll;
            if (rowY + ROW_HEIGHT < y || rowY > y + height) {
                continue;
            }
            boolean unreadable = entry.unreadable != null;
            boolean hovered = dialog == Dialog.NONE && Hover.is(x, rowY, width, ROW_HEIGHT, mouseX, mouseY);
            boolean on = entry == selected;
            if (on) {
                UiChrome.selectedCard(x, rowY, width, ROW_HEIGHT);
            } else {
                UiChrome.card(x, rowY, width, ROW_HEIGHT, hovered, false);
            }

            // leading icon tile
            Rects.rounded(x + 7f, rowY + 5f, 19f, 19f, 5, ClickGuiTheme.mask(64).getRGB(), false);
            Icons.draw(unreadable ? "alert" : "film", x + 11.5f, rowY + 9.5f, 10f,
                    (unreadable ? ClickGuiTheme.textDisabled() : ClickGuiTheme.textSecondary()).getRGB());

            FPSMaster.fontManager.s14.drawString(entry.displayName(), x + 32f, rowY + 5f,
                    (unreadable ? ClickGuiTheme.textSecondary() : ClickGuiTheme.textPrimary()).getRGB());
            if (unreadable) {
                sub.drawString(FPSMaster.i18n.get("replay.unreadable") + " · " + entry.sizeText(),
                        x + 32f, rowY + 15.5f, ClickGuiTheme.danger().getRGB());
            } else {
                sub.drawString(entry.detail(), x + 32f, rowY + 15.5f, ClickGuiTheme.textSecondary().getRGB());
            }

            if (hovered) {
                // hover actions replace the duration readout
                float opY = rowY + (ROW_HEIGHT - 15f) / 2f;
                float delX = x + width - 7f - 15f;
                float renX = delX - 3f - 15f;
                String editLabel = FPSMaster.i18n.get("edit.open");
                float editW = unreadable ? 0f : FPSMaster.fontManager.getFont(12).getStringWidth(editLabel) + 12f;
                float editX = renX - (unreadable ? 0f : 3f) - editW;
                float playW = unreadable ? 0f : FPSMaster.fontManager.getFont(12).getStringWidth(FPSMaster.i18n.get("replay.play")) + 20f;
                float playX = (unreadable ? renX : editX) - (unreadable ? 3f : 5f) - playW;

                if (!unreadable) {
                    boolean playHover = Hover.is(playX, opY, playW, 15f, mouseX, mouseY);
                    UiChrome.fillButton(playX, opY, playW, 15f, playHover, false);
                    Icons.draw("play", playX + 5f, opY + 4.5f, 6f, 0xFFFFFFFF);
                    FPSMaster.fontManager.getFont(12).drawString(FPSMaster.i18n.get("replay.play"),
                            playX + 13f, opY + 4f, 0xFFFFFFFF);
                    boolean editHover = Hover.is(editX, opY, editW, 15f, mouseX, mouseY);
                    UiChrome.button(editX, opY, editW, 15f, editHover);
                    FPSMaster.fontManager.getFont(12).drawString(editLabel, editX + 6f, opY + 4f,
                            ClickGuiTheme.textPrimary().getRGB());
                    boolean renHover = Hover.is(renX, opY, 15f, 15f, mouseX, mouseY);
                    UiChrome.ghostButton(renX, opY, 15f, 15f, renHover);
                    Icons.draw("rename", renX + 4f, opY + 4f, 7f,
                            (renHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
                    if (consumePressInBounds(renX, opY, 15f, 15f, 0) != null) {
                        openRename(entry);
                    }
                    if (consumePressInBounds(editX, opY, editW, 15f, 0) != null) {
                        selected = entry;
                        editSelected();
                    }
                    if (consumePressInBounds(playX, opY, playW, 15f, 0) != null) {
                        selected = entry;
                        playSelected();
                    }
                }
                boolean delHover = Hover.is(delX, opY, 15f, 15f, mouseX, mouseY);
                UiChrome.ghostButton(delX, opY, 15f, 15f, delHover);
                Icons.draw("delete", delX + 4f, opY + 4f, 7f,
                        (delHover ? ClickGuiTheme.danger() : ClickGuiTheme.textSecondary()).getRGB());
                if (consumePressInBounds(delX, opY, 15f, 15f, 0) != null) {
                    dialog = Dialog.DELETE;
                    dialogTarget = entry;
                }
            } else {
                String dur = unreadable ? "—" : (entry.durationMillis < 0 ? "…" : formatDuration(entry.durationMillis));
                float dw = sub.getStringWidth(dur);
                sub.drawString(dur, x + width - 9f - dw, rowY + (ROW_HEIGHT - 6f) / 2f,
                        ClickGuiTheme.textSecondary().getRGB());
            }

            if (dialog == Dialog.NONE && consumeClickInBounds(x, rowY, width, ROW_HEIGHT, 0) != null) {
                selected = entry;
                status = "";
            }
        }
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    private void openRename(Entry entry) {
        dialog = Dialog.RENAME;
        dialogTarget = entry;
        renameField.setText(entry.displayName());
        renameField.setFocused(true);
    }

    private void drawDialog(int mouseX, int mouseY) {
        UiChrome.veil(guiWidth, guiHeight, 0.85f);
        float w = 170f;
        float h = dialog == Dialog.DELETE ? 74f : 88f;
        float x = (guiWidth - w) / 2f;
        float y = (guiHeight - h) / 2f;
        UiChrome.panel(x, y, w, h);

        String name = tab == Tab.PROJECT && selectedProject != null
                ? selectedProject.project.name
                : (dialogTarget == null ? "" : dialogTarget.displayName());
        if (dialog == Dialog.DELETE) {
            UiChrome.boldString(FPSMaster.fontManager.s14,
                    String.format(FPSMaster.i18n.get(tab == Tab.PROJECT
                            ? "edit.delete.title" : "replay.delete.title"), name),
                    x + 11f, y + 10f, ClickGuiTheme.textPrimary().getRGB());
            FPSMaster.fontManager.getFont(12).drawString(
                    tab == Tab.PROJECT
                            ? FPSMaster.i18n.get("edit.delete.desc")
                            : String.format(FPSMaster.i18n.get("replay.delete.desc"),
                            dialogTarget == null ? "" : dialogTarget.sizeText()),
                    x + 11f, y + 23f, ClickGuiTheme.textSecondary().getRGB());
            float by = y + h - 26f;
            if (UiChrome.buttonClicked(this, x + w - 11f - 42f, by, 42f, UiChrome.BTN_H, null,
                    FPSMaster.i18n.get("common.delete"), UiChrome.Style.DANGER_FILL, mouseX, mouseY)) {
                deleteTarget();
            }
            if (UiChrome.buttonClicked(this, x + w - 11f - 42f - 5f - 40f, by, 40f, UiChrome.BTN_H, null,
                    FPSMaster.i18n.get("common.cancel"), UiChrome.Style.GHOST, mouseX, mouseY)) {
                closeDialog();
            }
        } else {
            UiChrome.boldString(FPSMaster.fontManager.s14, FPSMaster.i18n.get("replay.rename.title"),
                    x + 11f, y + 10f, ClickGuiTheme.textPrimary().getRGB());
            UiChrome.inputBox(x + 11f, y + 24f, w - 22f, 17f, true);
            renameField.backGroundColor = 0;
            renameField.fontColor = ClickGuiTheme.textPrimary().getRGB();
            renameField.drawTextBox(x + 15f, y + 25f, w - 30f, 15f);
            ScaledGuiScreen.PointerEvent press = consumePressInBounds(x + 11f, y + 24f, w - 22f, 17f, 0);
            if (press != null) {
                renameField.setFocused(true);
                renameField.mouseClicked(press.x, press.y, 0);
            }
            float by = y + h - 26f;
            if (UiChrome.buttonClicked(this, x + w - 11f - 42f, by, 42f, UiChrome.BTN_H, null,
                    FPSMaster.i18n.get("common.confirm"), UiChrome.Style.PRIMARY, mouseX, mouseY)) {
                renameTarget();
            }
            if (UiChrome.buttonClicked(this, x + w - 11f - 42f - 5f - 40f, by, 40f, UiChrome.BTN_H, null,
                    FPSMaster.i18n.get("common.cancel"), UiChrome.Style.GHOST, mouseX, mouseY)) {
                closeDialog();
            }
        }
        if (consumePressOutside(x, y, w, h) != null) {
            closeDialog();
        }
    }

    private void deleteTarget() {
        if (tab == Tab.PROJECT && selectedProject != null) {
            File target = selectedProject.file;
            if (target.delete()) {
                setStatus(String.format(FPSMaster.i18n.get("replay.status.deleted"), target.getName()),
                        ClickGuiTheme.textSecondary().getRGB());
                refreshProjects();
            } else {
                setStatus(String.format(FPSMaster.i18n.get("replay.status.delete.failed"), target.getName()),
                        ClickGuiTheme.danger().getRGB());
            }
            closeDialog();
            return;
        }
        if (dialogTarget != null) {
            File target = dialogTarget.file;
            if (target.delete()) {
                setStatus(String.format(FPSMaster.i18n.get("replay.status.deleted"), target.getName()),
                        ClickGuiTheme.textSecondary().getRGB());
                refresh();
            } else {
                setStatus(String.format(FPSMaster.i18n.get("replay.status.delete.failed"), target.getName()),
                        ClickGuiTheme.danger().getRGB());
            }
        }
        closeDialog();
    }

    private void renameTarget() {
        if (dialogTarget != null) {
            String newName = renameField.getText().trim();
            if (!newName.isEmpty() && !newName.contains(File.separator)) {
                File dest = new File(dialogTarget.file.getParentFile(), newName + ".edgereplay");
                if (dest.exists() || !dialogTarget.file.renameTo(dest)) {
                    setStatus(FPSMaster.i18n.get("replay.status.rename.failed"), ClickGuiTheme.danger().getRGB());
                } else {
                    refresh();
                }
            }
        }
        closeDialog();
    }

    private void closeDialog() {
        dialog = Dialog.NONE;
        dialogTarget = null;
        renameField.setFocused(false);
    }

    private void openReplaysFolder() {
        File directory = new File(FileUtils.dir, "replays");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            return;
        }
        try {
            Desktop.getDesktop().open(directory);
        } catch (IOException | RuntimeException exception) {
            ClientLogger.warn("Could not open replays folder: " + exception);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (dialog == Dialog.RENAME) {
            if (keyCode == 1) {
                closeDialog();
                return;
            }
            if (keyCode == 28) {
                renameTarget();
                return;
            }
            renameField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (dialog == Dialog.DELETE && keyCode == 1) {
            closeDialog();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void toggleRecording() {
        ReplayRecorder recorder = ReplayRecorder.instance();
        if (recorder.isRecording()) {
            String name = recorder.currentFile().getName();
            recorder.stop();
            setStatus(String.format(FPSMaster.i18n.get("replay.status.saved"), name), ClickGuiTheme.ok().getRGB());
            refresh();
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            setStatus(FPSMaster.i18n.get("replay.join.first"), ClickGuiTheme.danger().getRGB());
            return;
        }
        recorder.start("replay-" + System.currentTimeMillis());
        setStatus(recorder.isRecording()
                        ? String.format(FPSMaster.i18n.get("replay.status.recording"), recorder.currentFile().getName())
                        : FPSMaster.i18n.get("replay.status.record.failed"),
                recorder.isRecording() ? ClickGuiTheme.ok().getRGB() : ClickGuiTheme.danger().getRGB());
    }

    private void playSelected() {
        if (selected == null) {
            setStatus(FPSMaster.i18n.get("replay.pick.first"), ClickGuiTheme.danger().getRGB());
            return;
        }
        if (selected.unreadable != null) {
            setStatus(FPSMaster.i18n.get("replay.unreadable"), ClickGuiTheme.danger().getRGB());
            return;
        }
        if (ReplayRecorder.instance().isRecording()) {
            setStatus(FPSMaster.i18n.get("replay.stop.first"), new Color(255, 190, 110).getRGB());
            return;
        }
        ReplayPlayer.instance().start(selected.file);
    }

    private void editSelected() {
        if (selected == null || selected.unreadable != null) {
            return;
        }
        if (ReplayRecorder.instance().isRecording()) {
            setStatus(FPSMaster.i18n.get("replay.stop.first"), new Color(255, 190, 110).getRGB());
            return;
        }
        File replay = selected.file;
        int duration = selected.durationMillis < 0 ? 1 : selected.durationMillis;
        ReplayPlayer.instance().start(replay);
        if (!ReplayPlayer.instance().isActive()) {
            setStatus(FPSMaster.i18n.get("replay.unreadable"), ClickGuiTheme.danger().getRGB());
            return;
        }
        DirectorCamera.beginForReplay(replay, duration);
        DirectorPanel.setOpen(true);
        mc.displayGuiScreen(new ReplayControlScreen());
    }

    private void openProject(ProjectEntry row) {
        if (row == null || row.missingSource) {
            setStatus(FPSMaster.i18n.get("edit.missing.source"), ClickGuiTheme.danger().getRGB());
            return;
        }
        if (ReplayRecorder.instance().isRecording()) {
            setStatus(FPSMaster.i18n.get("replay.stop.first"), new Color(255, 190, 110).getRGB());
            return;
        }
        File source = new File(new File(FileUtils.dir, "replays"), row.project.source + ".edgereplay");
        ReplayPlayer.instance().start(source);
        if (!ReplayPlayer.instance().isActive()) {
            setStatus(FPSMaster.i18n.get("replay.unreadable"), ClickGuiTheme.danger().getRGB());
            return;
        }
        DirectorCamera.openProject(row.project, row.file);
        DirectorPanel.setOpen(true);
        mc.displayGuiScreen(new ReplayControlScreen());
    }

    private void drawProjectList(float x, float y, float width, float height, int mouseX, int mouseY) {
        if (projects.isEmpty()) {
            Icons.draw("film", x + width / 2f - 8f, y + height / 2f - 18f, 16f, ClickGuiTheme.textDisabled().getRGB());
            FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get("edit.empty"),
                    x + width / 2f, y + height / 2f + 2f, ClickGuiTheme.textDisabled().getRGB());
            FPSMaster.fontManager.getFont(12).drawCenteredString(FPSMaster.i18n.get("edit.empty.tip"),
                    x + width / 2f, y + height / 2f + 12f, ClickGuiTheme.textDisabled().getRGB());
            return;
        }
        float contentHeight = projects.size() * (ROW_HEIGHT + ROW_GAP);
        int wheel = dialog == Dialog.NONE ? consumeWheelDelta(x, y, width, height) : 0;
        if (wheel != 0) {
            scroll -= wheel / 120f * (ROW_HEIGHT + ROW_GAP);
        }
        scroll = Math.max(0f, Math.min(Math.max(0f, contentHeight - height), scroll));
        UFontRenderer sub = FPSMaster.fontManager.getFont(12);
        for (int index = 0; index < projects.size(); index++) {
            ProjectEntry row = projects.get(index);
            float rowY = y + index * (ROW_HEIGHT + ROW_GAP) - scroll;
            if (rowY + ROW_HEIGHT < y || rowY > y + height) {
                continue;
            }
            boolean hovered = dialog == Dialog.NONE && Hover.is(x, rowY, width, ROW_HEIGHT, mouseX, mouseY);
            boolean on = row == selectedProject;
            if (on) {
                UiChrome.selectedCard(x, rowY, width, ROW_HEIGHT);
            } else {
                UiChrome.card(x, rowY, width, ROW_HEIGHT, hovered, false);
            }
            Rects.rounded(x + 7f, rowY + 5f, 19f, 19f, 5, ClickGuiTheme.mask(64).getRGB(), false);
            Icons.draw("sliders", x + 11.5f, rowY + 9.5f, 10f, ClickGuiTheme.textSecondary().getRGB());
            FPSMaster.fontManager.s14.drawString(row.project.name, x + 32f, rowY + 5f,
                    ClickGuiTheme.textPrimary().getRGB());
            if (row.missingSource) {
                sub.drawString(FPSMaster.i18n.get("edit.missing.source"),
                        x + 32f, rowY + 15.5f, ClickGuiTheme.danger().getRGB());
            } else {
                sub.drawString(String.format(FPSMaster.i18n.get("edit.project.detail"),
                                row.project.source, row.project.clips.size(),
                                formatWhen(row.file.lastModified())),
                        x + 32f, rowY + 15.5f, ClickGuiTheme.textSecondary().getRGB());
            }
            if (hovered) {
                float opY = rowY + (ROW_HEIGHT - 15f) / 2f;
                float delX = x + width - 7f - 15f;
                String openLabel = FPSMaster.i18n.get("edit.workbench");
                float openW = FPSMaster.fontManager.getFont(12).getStringWidth(openLabel) + 20f;
                float openX = delX - 5f - openW;
                if (!row.missingSource) {
                    boolean openHover = Hover.is(openX, opY, openW, 15f, mouseX, mouseY);
                    UiChrome.fillButton(openX, opY, openW, 15f, openHover, false);
                    FPSMaster.fontManager.getFont(12).drawString(openLabel, openX + 6f, opY + 4f, 0xFFFFFFFF);
                    if (consumePressInBounds(openX, opY, openW, 15f, 0) != null) {
                        selectedProject = row;
                        openProject(row);
                    }
                }
                boolean delHover = Hover.is(delX, opY, 15f, 15f, mouseX, mouseY);
                UiChrome.ghostButton(delX, opY, 15f, 15f, delHover);
                Icons.draw("delete", delX + 4f, opY + 4f, 7f,
                        (delHover ? ClickGuiTheme.danger() : ClickGuiTheme.textSecondary()).getRGB());
                if (consumePressInBounds(delX, opY, 15f, 15f, 0) != null) {
                    dialog = Dialog.DELETE;
                    dialogTarget = null;
                    selectedProject = row;
                }
            } else {
                String dur = ReplayScreen.formatDuration(row.project.outputDurationMillis());
                float dw = sub.getStringWidth(dur);
                sub.drawString(dur, x + width - 9f - dw, rowY + (ROW_HEIGHT - 6f) / 2f,
                        ClickGuiTheme.textSecondary().getRGB());
            }
            if (dialog == Dialog.NONE && consumeClickInBounds(x, rowY, width, ROW_HEIGHT, 0) != null) {
                selectedProject = row;
                status = "";
            }
        }
    }

    private long sourceBytes() {
        long total = 0;
        for (Entry entry : entries) {
            total += entry.file.length();
        }
        return total;
    }

    private void openEditsFolder() {
        File directory = EditStore.directory();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            return;
        }
        try {
            Desktop.getDesktop().open(directory);
        } catch (IOException | RuntimeException exception) {
            ClientLogger.warn("Could not open edits folder: " + exception);
        }
    }

    private void setStatus(String message, int color) {
        this.status = message;
        this.statusColor = color;
    }

    public static String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        return String.format("%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    /** 今天 13:52 · 昨天 21:04 · 8 月 10 日 — the prototype's relative dates. */
    static String formatWhen(long millis) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        Calendar now = Calendar.getInstance();
        SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            return FPSMaster.i18n.get("time.today") + " " + clock.format(new Date(millis));
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            return FPSMaster.i18n.get("time.yesterday") + " " + clock.format(new Date(millis));
        }
        return new SimpleDateFormat(FPSMaster.i18n.get("time.date.fmt"), Locale.getDefault()).format(new Date(millis));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class ProjectEntry {
        File file;
        EditProject project;
        boolean missingSource;
    }

    private static final class Entry {
        final File file;
        volatile String recorder;
        volatile int durationMillis = -1;
        volatile String unreadable;

        Entry(File file) {
            this.file = file;
        }

        String displayName() {
            String name = file.getName();
            return name.substring(0, name.length() - ".edgereplay".length());
        }

        String sizeText() {
            long kib = file.length() / 1024L;
            return kib >= 1024L ? (kib / 1024L) + " MB" : kib + " KB";
        }

        String detail() {
            StringBuilder detail = new StringBuilder();
            if (recorder != null) {
                detail.append(recorder).append(" · ");
            }
            detail.append(formatWhen(file.lastModified()));
            detail.append(" · ").append(sizeText());
            return detail.toString();
        }
    }
}
