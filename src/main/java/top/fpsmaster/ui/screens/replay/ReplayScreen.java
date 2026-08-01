package top.fpsmaster.ui.screens.replay;

import net.minecraft.client.gui.GuiScreen;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.replay.ReplayFile;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.ReplayRecorder;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Browser for recorded sessions: start or stop a recording, and play one back.
 *
 * <p>Reachable from the main menu and from {@code .replay} in game, because the two things it does
 * belong to different moments — you start a recording before joining a match and pick one to watch
 * afterwards, and neither should require being in the other place.
 */
public class ReplayScreen extends ScaledGuiScreen {

    private static final int ROW_HEIGHT = 28;
    private static final Color PANEL = new Color(0, 0, 0, 150);
    private static final Color ROW = new Color(255, 255, 255, 14);
    private static final Color ROW_HOVER = new Color(255, 255, 255, 30);
    private static final Color ROW_SELECTED = new Color(113, 127, 254, 110);
    private static final Color SUBTLE = new Color(190, 190, 190);

    private final GuiScreen parent;
    private final GuiButton recordButton;
    private final GuiButton playButton;
    private final GuiButton deleteButton;
    private final GuiButton backButton;

    private final List<Entry> entries = new ArrayList<Entry>();
    private Entry selected;
    private float scroll;
    private String status = "";
    private int statusColor = SUBTLE.getRGB();

    public ReplayScreen(GuiScreen parent) {
        this.parent = parent;
        this.recordButton = new GuiButton("Record", this::toggleRecording).setText("Record", false);
        this.playButton = new GuiButton("Play", this::playSelected).setText("Play", false);
        this.deleteButton = new GuiButton("Delete", this::deleteSelected).setText("Delete", false);
        this.backButton = new GuiButton("Back", () -> mc.displayGuiScreen(parent)).setText("Back", false);
    }

    @Override
    public void initGui() {
        super.initGui();
        refresh();
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

        float panelWidth = Math.min(420f, guiWidth - 40f);
        float panelHeight = Math.min(300f, guiHeight - 40f);
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth),
                Math.round(panelHeight), 8, PANEL.getRGB());
        FPSMaster.fontManager.s24.drawString("Replays", panelX + 18f, panelY + 16f, Color.WHITE.getRGB());
        drawRecordingState(panelX + panelWidth - 18f, panelY + 20f);

        float listX = panelX + 18f;
        float listY = panelY + 48f;
        float listWidth = panelWidth - 36f;
        float listHeight = panelHeight - 48f - 44f;
        drawList(listX, listY, listWidth, listHeight, mouseX, mouseY);

        float buttonY = panelY + panelHeight - 32f;
        recordButton.setText(ReplayRecorder.instance().isRecording() ? "Stop Recording" : "Record", false);
        recordButton.renderInScreen(this, listX, buttonY, 92f, 20f, mouseX, mouseY);
        playButton.renderInScreen(this, listX + 98f, buttonY, 60f, 20f, mouseX, mouseY);
        deleteButton.renderInScreen(this, listX + 164f, buttonY, 60f, 20f, mouseX, mouseY);
        backButton.renderInScreen(this, listX + listWidth - 60f, buttonY, 60f, 20f, mouseX, mouseY);

        if (!status.isEmpty()) {
            FPSMaster.fontManager.s16.drawString(status, listX, buttonY - 14f, statusColor);
        }
    }

    private void drawRecordingState(float rightX, float y) {
        ReplayRecorder recorder = ReplayRecorder.instance();
        if (!recorder.isRecording()) {
            return;
        }
        String text = "REC " + formatDuration(recorder.elapsedMillis()) + "  "
                + recorder.bytesWritten() / 1024L + " KiB";
        float width = FPSMaster.fontManager.s16.getStringWidth(text);
        FPSMaster.fontManager.s16.drawString(text, rightX - width, y, new Color(255, 90, 90).getRGB());
    }

    private void drawList(float x, float y, float width, float height, int mouseX, int mouseY) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 4,
                new Color(0, 0, 0, 70).getRGB());

        if (entries.isEmpty()) {
            FPSMaster.fontManager.s16.drawCenteredString("No recordings yet - press Record before a match",
                    x + width / 2f, y + height / 2f - 4f, SUBTLE.getRGB());
            return;
        }

        float contentHeight = entries.size() * ROW_HEIGHT;
        int wheel = consumeWheelDelta(x, y, width, height);
        if (wheel != 0) {
            scroll -= wheel / 120f * ROW_HEIGHT;
        }
        scroll = Math.max(0f, Math.min(Math.max(0f, contentHeight - height), scroll));

        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            float rowY = y + index * ROW_HEIGHT - scroll;
            // Whole rows only: a partially visible row is skipped rather than scissored, which keeps
            // this screen free of GL state it does not otherwise need.
            if (rowY < y || rowY + ROW_HEIGHT > y + height) {
                continue;
            }

            boolean hovered = Hover.is(x, rowY, width, ROW_HEIGHT, mouseX, mouseY);
            Color background = entry == selected ? ROW_SELECTED : hovered ? ROW_HOVER : ROW;
            Rects.rounded(Math.round(x + 2), Math.round(rowY + 2), Math.round(width - 4),
                    ROW_HEIGHT - 4, 3, background.getRGB());

            FPSMaster.fontManager.s16.drawString(entry.displayName(), x + 10f, rowY + 6f,
                    Color.WHITE.getRGB());
            FPSMaster.fontManager.s16.drawString(entry.detail(), x + 10f, rowY + 16f, SUBTLE.getRGB());

            if (consumeClickInBounds(x, rowY, width, ROW_HEIGHT, 0) != null) {
                selected = entry;
                status = "";
            }
        }
    }

    private void toggleRecording() {
        ReplayRecorder recorder = ReplayRecorder.instance();
        if (recorder.isRecording()) {
            String name = recorder.currentFile().getName();
            recorder.stop();
            setStatus("Saved " + name, new Color(110, 255, 150).getRGB());
            refresh();
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            setStatus("Join a world first - a recording needs something to record",
                    new Color(255, 190, 110).getRGB());
            return;
        }
        recorder.start("replay-" + System.currentTimeMillis());
        setStatus(recorder.isRecording() ? "Recording " + recorder.currentFile().getName()
                : "Could not start recording, see the log",
                recorder.isRecording() ? new Color(110, 255, 150).getRGB()
                        : new Color(255, 120, 120).getRGB());
    }

    private void playSelected() {
        if (selected == null) {
            setStatus("Pick a recording first", new Color(255, 190, 110).getRGB());
            return;
        }
        if (selected.unreadable != null) {
            setStatus("Cannot play this recording: " + selected.unreadable,
                    new Color(255, 120, 120).getRGB());
            return;
        }
        if (ReplayRecorder.instance().isRecording()) {
            setStatus("Stop recording before playing one back", new Color(255, 190, 110).getRGB());
            return;
        }
        ReplayPlayer.instance().start(selected.file);
    }

    private void deleteSelected() {
        if (selected == null) {
            setStatus("Pick a recording first", new Color(255, 190, 110).getRGB());
            return;
        }
        File target = selected.file;
        if (target.delete()) {
            setStatus("Deleted " + target.getName(), SUBTLE.getRGB());
            refresh();
        } else {
            setStatus("Could not delete " + target.getName(), new Color(255, 120, 120).getRGB());
        }
    }

    private void setStatus(String message, int color) {
        this.status = message;
        this.statusColor = color;
    }

    static String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        return String.format("%d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
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

        String detail() {
            if (unreadable != null) {
                return "unreadable - " + unreadable;
            }
            StringBuilder detail = new StringBuilder();
            detail.append(durationMillis < 0 ? "reading..." : formatDuration(durationMillis));
            detail.append("  ").append(file.length() / 1024L).append(" KiB");
            if (recorder != null) {
                detail.append("  ").append(recorder);
            }
            return detail.toString();
        }
    }
}
