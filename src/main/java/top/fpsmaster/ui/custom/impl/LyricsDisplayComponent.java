package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.features.impl.interfaces.LyricsDisplay;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.LyricLine;
import top.fpsmaster.modules.music.MusicManager;
import top.fpsmaster.prism.screen.MusicBridge;
import top.fpsmaster.prism.screen.SharedLyrics;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.ui.hud.HudEditorScreen;

import java.util.ArrayList;
import java.util.List;

public class LyricsDisplayComponent extends Component {
    private final SharedLyrics lyrics = new SharedLyrics();
    private Lyric cachedLyric;
    private List<MusicBridge.LyricRow> cachedRows = new ArrayList<MusicBridge.LyricRow>();
    private long lastNanos;

    public LyricsDisplayComponent() {
        super(LyricsDisplay.class);
        x = 0.25f;
        y = 0.75f;
        allowScale = true;
    }

    @Override
    public void measure() {
        width = 260f;
        height = SharedLyrics.hudHeight(style());
    }

    @Override
    public boolean hasBackground() {
        return false;
    }

    @Override
    public void draw(float x, float y) {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.016f : Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        MusicManager music = MusicManager.get();
        boolean ownsFrame = !EdgeUi.hasFrame();
        if (ownsFrame) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            EdgeUi.beginOverlay(sr.getScaledWidth() * sr.getScaleFactor() / 2f,
                    sr.getScaledHeight() * sr.getScaleFactor() / 2f);
        }
        try {
            boolean preview = Minecraft.getMinecraft().currentScreen instanceof HudEditorScreen;
            List<MusicBridge.LyricRow> rows = preview ? previewRows() : rows(music.getCurrentLyric());
            int current = preview ? 0 : music.currentLyricLine();
            lyrics.drawHud(EdgeUi.frame(), rows, current,
                    x, y, width * scale, style(scale), dt);
        } finally {
            if (ownsFrame) EdgeUi.end();
        }
    }

    @Override
    public boolean isVisibleForAlignment() {
        return super.isVisibleForAlignment() && MusicManager.get().getCurrentLyric() != null;
    }

    private List<MusicBridge.LyricRow> rows(Lyric lyric) {
        if (lyric == cachedLyric) return cachedRows;
        cachedLyric = lyric;
        cachedRows = new ArrayList<MusicBridge.LyricRow>();
        if (lyric != null && lyric.getLines() != null) {
            for (LyricLine line : lyric.getLines()) {
                cachedRows.add(new MusicBridge.LyricRow(line.getText(), line.getTranslation()));
            }
        }
        return cachedRows;
    }

    private static List<MusicBridge.LyricRow> previewRows() {
        List<MusicBridge.LyricRow> rows = new ArrayList<MusicBridge.LyricRow>();
        rows.add(new MusicBridge.LyricRow("We're singing through the night", "我们在夜色中歌唱"));
        rows.add(new MusicBridge.LyricRow("This moment stays with us", "这一刻与我们同在"));
        return rows;
    }

    private static SharedLyrics.HudStyle style(float scale) {
        return new SharedLyrics.HudStyle(Math.round(LyricsDisplay.fontSize.getValue().floatValue() * scale),
                LyricsDisplay.lines.getValue().intValue(), LyricsDisplay.translation.getValue(),
                LyricsDisplay.scroll.getValue(), LyricsDisplay.background.getValue(),
                LyricsDisplay.backgroundColor.getRGB(), LyricsDisplay.textColor.getRGB());
    }

    private static SharedLyrics.HudStyle style() {
        return style(1f);
    }
}
