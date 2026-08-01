package top.fpsmaster.modules.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.LyricLine;
import top.fpsmaster.utils.render.draw.Rects;

import java.util.List;

/**
 * 游戏内歌词叠加层：开启后（{@link MusicManager#setShowLyricsInGame}）在 HUD 上居中显示当前播放的歌词行。
 *
 * <p>仅在启用时注册到事件总线；音乐界面打开时游戏 HUD 不渲染（{@link EventRender2D} 不触发），
 * 故不会与界面内歌词重复。
 */
public class MusicOverlay {

    private final MusicManager m;

    public MusicOverlay(MusicManager m) {
        this.m = m;
    }

    @Subscribe
    public void onRender2D(EventRender2D e) {
        if (!m.isShowLyricsInGame() || m.getCurrent() == null) {
            return;
        }
        Lyric ly = m.getCurrentLyric();
        if (ly == null || ly.getLines() == null || ly.getLines().isEmpty()) {
            return;
        }
        int idx = m.currentLyricLine();
        if (idx < 0) {
            return;
        }
        List<LyricLine> lines = ly.getLines();
        LyricLine line = lines.get(idx);
        String text = line.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        String tr = line.getTranslation();
        boolean hasTr = tr != null && !tr.isEmpty();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        UFontRenderer font = FPSMaster.fontManager.s18;
        UFontRenderer subFont = FPSMaster.fontManager.s14;

        float cx = sw / 2f;
        float y = sh - 78f;

        float tw = Math.max(font.getStringWidth(text), hasTr ? subFont.getStringWidth(tr) : 0) + 24f;
        float th = hasTr ? 40f : 24f;
        Rects.rounded((int) (cx - tw / 2f), (int) (y - 6f), (int) tw, (int) th, 6, 0x99000000);
        font.drawCenteredString(text, cx, y, 0xFFFFFFFF);
        if (hasTr) {
            subFont.drawCenteredString(tr, cx, y + 18f, 0xFFB8BCC4);
        }
    }
}
