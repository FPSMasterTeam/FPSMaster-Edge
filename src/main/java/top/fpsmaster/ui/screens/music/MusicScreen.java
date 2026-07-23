package top.fpsmaster.ui.screens.music;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.music.MusicManager;
import top.fpsmaster.modules.music.MusicTextures;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.LyricLine;
import top.fpsmaster.music.MusicSource;
import top.fpsmaster.music.QrCode;
import top.fpsmaster.music.QrLoginState;
import top.fpsmaster.music.Track;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;

import java.util.ArrayList;
import java.util.List;

/**
 * 音乐界面：源切换（网易云 / QQ）、搜索、发现、歌单、扫码/Cookie 登录、播放控制、进度/音量、歌词。
 *
 * <p>数据来自开源库 Cadence（top.fpsmaster:music-api），播放走 {@link top.fpsmaster.modules.music.AudioEngine}。
 * UI 遵循 Edge 既有框架（{@link ScaledGuiScreen} + Rects/Images/字体 + 手动滚动/裁剪）。
 */
public class MusicScreen extends ScaledGuiScreen {

    private enum Tab {DISCOVER, PLAYLISTS, SEARCH, LYRICS}

    // 配色（跟随 ClickGUI 主题，见 refreshTheme）
    private static final int DIM = 0xB0000000;
    private static final int BADGE = 0xFFD9A441;
    private int PANEL, PANEL2, CARD, CARD_HOVER, TEXT, SUB, TRACK_BG;

    private void refreshTheme() {
        PANEL = ClickGuiTheme.panelBg().getRGB();
        PANEL2 = ClickGuiTheme.moduleHeaderBg().getRGB();
        CARD = ClickGuiTheme.cardBg().getRGB();
        CARD_HOVER = ClickGuiTheme.cardHoverBg().getRGB();
        TEXT = ClickGuiTheme.textPrimary().getRGB();
        SUB = ClickGuiTheme.textSecondary().getRGB();
        TRACK_BG = ClickGuiTheme.divider().getRGB();
    }

    private final MusicManager m;
    private Tab tab = Tab.DISCOVER;

    private UFontRenderer f14, f16, f18, f20;

    private TextField searchField;
    private TextField qqUin, qqKey;

    private List<Track> discoverTracks = new ArrayList<Track>();
    private List<Track> resultTracks = new ArrayList<Track>();
    private List<MusicManager.PlaylistItem> playlists = new ArrayList<MusicManager.PlaylistItem>();
    private String resultTitle = "";

    private boolean discoverLoaded = false;
    private boolean discoverLoading = false;
    private boolean playlistsLoaded = false;
    private boolean playlistsLoading = false;

    private float scroll = 0;

    private boolean loginOpen = false;
    private QrLoginState lastQrState = null;

    private boolean draggingProgress = false;
    private float previewFrac = 0;
    private boolean draggingVolume = false;

    public MusicScreen() {
        this.m = MusicManager.get();
    }

    @Override
    public void initGui() {
        super.initGui();
        f14 = FPSMaster.fontManager.s14;
        f16 = FPSMaster.fontManager.s16;
        f18 = FPSMaster.fontManager.s18;
        f20 = FPSMaster.fontManager.s20;
        searchField = new TextField(f16, "搜索歌曲 / 歌手…", CARD, TEXT, 60, new Runnable() {
            @Override
            public void run() {
                doSearch();
            }
        });
        qqUin = new TextField(f14, "musicid (uin)", CARD, TEXT, 32);
        qqKey = new TextField(f14, "musickey (qm_keyst)", CARD, TEXT, 256);
    }

    private int accent() {
        return ClickGuiTheme.accent().getRGB();
    }

    // ================= 渲染 =================

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        refreshTheme();
        int mx = getMouseX();
        int my = getMouseY();
        ScaledGuiScreen.PointerEvent pe = consumePressInBounds(0, 0, guiWidth, guiHeight, 0);
        boolean click = pe != null;
        int cx = click ? pe.x : -1;
        int cy = click ? pe.y : -1;

        Rects.fill(0, 0, guiWidth, guiHeight, DIM);

        float pw = Math.min(guiWidth - 30, 660);
        float ph = Math.min(guiHeight - 30, 440);
        float px = (guiWidth - pw) / 2f;
        float py = (guiHeight - ph) / 2f;

        Rects.rounded((int) px, (int) py, (int) pw, (int) ph, 8, PANEL);

        float headerH = 34;
        float playerH = 52;
        float bodyY = py + headerH;
        float bodyH = ph - headerH - playerH;

        // 若登录弹窗打开，主体点击不生效（modal 优先）
        boolean modal = loginOpen;

        drawHeader(px, py, pw, headerH, mx, my, modal ? -1 : cx, modal ? -1 : cy);
        float tabsH = 26;
        drawTabs(px, bodyY, pw, tabsH, mx, my, modal ? -1 : cx, modal ? -1 : cy);

        float contentY = bodyY + tabsH;
        float contentH = bodyH - tabsH;
        drawContent(px + 10, contentY, pw - 20, contentH, mx, my, modal ? -1 : cx, modal ? -1 : cy);

        drawPlayerBar(px, py + ph - playerH, pw, playerH, mx, my, modal ? -1 : cx, modal ? -1 : cy);

        if (loginOpen) {
            drawLoginModal(px, py, pw, ph, mx, my, cx, cy);
        }
    }

    private void drawHeader(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        f18.drawString("Music", x + 12, y + h / 2f - f18.getHeight() / 2f, TEXT);

        // 源切换
        float bx = x + 70;
        bx = sourcePill(bx, y + 7, "网易云", MusicSource.NETEASE, mx, my, cx, cy);
        bx = sourcePill(bx + 6, y + 7, "QQ音乐", MusicSource.QQ, mx, my, cx, cy);

        // 关闭
        float closeS = 20;
        float closeX = x + w - closeS - 10;
        float closeY = y + (h - closeS) / 2f;
        boolean ch = Hover.is(closeX, closeY, closeS, closeS, mx, my);
        Rects.rounded((int) closeX, (int) closeY, (int) closeS, (int) closeS, 5, ch ? CARD_HOVER : CARD);
        f16.drawString("x", closeX + 7, closeY + 3, ch ? TEXT : SUB);
        if (in(cx, cy, closeX, closeY, closeS, closeS)) {
            mc.displayGuiScreen(null);
            return;
        }

        // 登录 / 用户
        String label = m.isLoggedIn() ? "已登录 · 退出" : "登录";
        float lw = f14.getStringWidth(label) + 16;
        float lx = closeX - lw - 8;
        float ly = y + (h - 20) / 2f;
        boolean lh = Hover.is(lx, ly, lw, 20, mx, my);
        Rects.rounded((int) lx, (int) ly, (int) lw, 20, 5, lh ? CARD_HOVER : CARD);
        f14.drawString(label, lx + 8, ly + 6, m.isLoggedIn() ? accent() : TEXT);
        if (in(cx, cy, lx, ly, lw, 20)) {
            if (m.isLoggedIn()) {
                m.logout();
            } else {
                openLogin();
            }
        }

        // 搜索框
        float sfW = Math.min(220, lx - bx - 16);
        if (sfW > 80) {
            float sfX = bx + 10;
            float sfY = y + 7;
            searchField.drawTextBox(sfX, sfY, sfW, 20);
            if (cx >= 0) searchField.mouseClicked(cx, cy, 0);
        }
    }

    private float sourcePill(float x, float y, String label, MusicSource src, int mx, int my, int cx, int cy) {
        float w = f14.getStringWidth(label) + 16;
        boolean active = m.getSource() == src;
        boolean hov = Hover.is(x, y, w, 20, mx, my);
        int col = active ? (src == MusicSource.QQ ? 0xFF2FBE77 : 0xFFE7392F) : (hov ? CARD_HOVER : CARD);
        Rects.rounded((int) x, (int) y, (int) w, 20, 5, col);
        f14.drawString(label, x + 8, y + 6, active ? 0xFFFFFFFF : SUB);
        if (in(cx, cy, x, y, w, 20) && !active) {
            m.setSource(src);
            onSourceChanged();
        }
        return x + w;
    }

    private void onSourceChanged() {
        discoverLoaded = false;
        playlistsLoaded = false;
        discoverTracks = new ArrayList<Track>();
        playlists = new ArrayList<MusicManager.PlaylistItem>();
        scroll = 0;
    }

    private void drawTabs(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        String[] names = {"发现", "歌单", "搜索", "歌词"};
        Tab[] tabs = {Tab.DISCOVER, Tab.PLAYLISTS, Tab.SEARCH, Tab.LYRICS};
        float tx = x + 10;
        for (int i = 0; i < names.length; i++) {
            float tw = f16.getStringWidth(names[i]) + 4;
            boolean active = tab == tabs[i];
            f16.drawString(names[i], tx, y + h / 2f - f16.getHeight() / 2f, active ? TEXT : SUB);
            if (active) {
                Rects.fill(tx, y + h - 3, tw, 2, accent());
            }
            if (in(cx, cy, tx - 4, y, tw + 8, h)) {
                if (tab != tabs[i]) {
                    tab = tabs[i];
                    scroll = 0;
                }
            }
            tx += tw + 18;
        }
    }

    private void drawContent(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        int wheel = consumeWheelDelta(x, y, w, h);
        if (wheel != 0) {
            scroll -= wheel / 8f;
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(x, y, w, h);
        switch (tab) {
            case DISCOVER:
                ensureDiscover();
                drawTrackList(discoverTracks, "发现 · " + sourceName(), x, y, w, h, cx, cy);
                break;
            case PLAYLISTS:
                ensurePlaylists();
                drawPlaylistGrid(x, y, w, h, mx, my, cx, cy);
                break;
            case SEARCH:
                drawTrackList(resultTracks, resultTitle.isEmpty() ? "搜索结果" : resultTitle, x, y, w, h, cx, cy);
                break;
            case LYRICS:
                drawLyrics(x, y, w, h);
                break;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private String sourceName() {
        return m.getSource() == MusicSource.QQ ? "QQ音乐" : "网易云";
    }

    private void ensureDiscover() {
        if (discoverLoaded || discoverLoading) return;
        discoverLoading = true;
        m.loadDiscover(new MusicManager.Cb<List<Track>>() {
            @Override
            public void done(List<Track> result, Throwable error) {
                discoverLoading = false;
                discoverLoaded = true;
                if (result != null) discoverTracks = result;
            }
        });
    }

    private void ensurePlaylists() {
        if (playlistsLoaded || playlistsLoading) return;
        playlistsLoading = true;
        m.loadPlaylists(new MusicManager.Cb<List<MusicManager.PlaylistItem>>() {
            @Override
            public void done(List<MusicManager.PlaylistItem> result, Throwable error) {
                playlistsLoading = false;
                playlistsLoaded = true;
                if (result != null) playlists = result;
            }
        });
    }

    private void drawTrackList(List<Track> list, String title, float x, float y, float w, float h, int cx, int cy) {
        float rowH = 40;
        float headH = 22;
        f14.drawString(title, x, y + 4, SUB);
        float listY = y + headH;
        float listH = h - headH;

        if (list.isEmpty()) {
            String hint = m.getSource() == MusicSource.NETEASE && tab == Tab.DISCOVER && !m.isLoggedIn()
                    ? "登录网易云后可见每日推荐，或直接搜索" : "暂无内容";
            f14.drawString(hint, x, listY + 8, SUB);
            return;
        }

        float total = list.size() * rowH;
        clampScroll(total, listH);

        for (int i = 0; i < list.size(); i++) {
            Track t = list.get(i);
            float ry = listY + i * rowH - scroll;
            if (ry + rowH < listY || ry > listY + listH) continue;
            boolean hov = Hover.is(x, Math.max(ry, listY), w, rowH, getMouseX(), getMouseY())
                    && getMouseY() >= listY && getMouseY() <= listY + listH;
            boolean playing = t == m.getCurrent();
            if (hov || playing) {
                Rects.rounded((int) x, (int) ry, (int) w, (int) rowH - 4, 5, playing ? withAlpha(accent(), 40) : CARD);
            }
            // 封面
            float cs = rowH - 12;
            ResourceLocation cover = MusicTextures.cover(t.getCoverUrl());
            if (cover != null) {
                Images.draw(cover, x + 4, ry + 4, cs, cs);
            } else {
                Rects.rounded((int) (x + 4), (int) (ry + 4), (int) cs, (int) cs, 4, PANEL2);
            }
            float tx = x + cs + 12;
            String name = f16.trimStringToWidth(t.getName(), w - cs - 120);
            f16.drawString(name, tx, ry + 6, playing ? accent() : TEXT);
            if (t.getVip()) {
                float nw = f16.getStringWidth(name);
                Rects.rounded((int) (tx + nw + 4), (int) (ry + 6), 20, 11, 3, withAlpha(BADGE, 60));
                f14.drawString("VIP", tx + nw + 6, ry + 6, BADGE);
            }
            f14.drawString(f14.trimStringToWidth(t.getArtists(), w - cs - 120), tx, ry + 21, SUB);
            // 时长
            String dur = fmt(t.getDurationMs());
            f14.drawString(dur, x + w - f14.getStringWidth(dur) - 8, ry + 13, SUB);

            if (in(cx, cy, x, ry, w, rowH) && cy >= listY && cy <= listY + listH) {
                m.playList(list, i);
                tab = Tab.LYRICS;
                scroll = 0;
            }
        }
    }

    private void drawPlaylistGrid(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        f14.drawString("歌单 · " + sourceName(), x, y + 4, SUB);
        float top = y + 22;
        float gridH = h - 22;
        if (playlists.isEmpty()) {
            String hint = playlistsLoading ? "加载中…" :
                    (m.getSource() == MusicSource.NETEASE && !m.isLoggedIn() ? "登录网易云后可见我的歌单" : "暂无歌单");
            f14.drawString(hint, x, top + 8, SUB);
            return;
        }
        int cols = 3;
        float gap = 10;
        float cw = (w - gap * (cols - 1)) / cols;
        float chh = cw * 0.78f + 20;
        int rows = (playlists.size() + cols - 1) / cols;
        clampScroll(rows * (chh + gap), gridH);

        for (int i = 0; i < playlists.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            float cardX = x + col * (cw + gap);
            float cardY = top + row * (chh + gap) - scroll;
            if (cardY + chh < top || cardY > top + gridH) continue;
            MusicManager.PlaylistItem p = playlists.get(i);
            boolean hov = Hover.is(cardX, cardY, cw, chh, mx, my) && my >= top && my <= top + gridH;
            Rects.rounded((int) cardX, (int) cardY, (int) cw, (int) chh, 6, hov ? CARD_HOVER : CARD);
            ResourceLocation cover = MusicTextures.cover(p.cover);
            float imgS = cw - 12;
            if (cover != null) {
                Images.draw(cover, cardX + 6, cardY + 6, imgS, imgS);
            } else {
                Rects.rounded((int) (cardX + 6), (int) (cardY + 6), (int) imgS, (int) imgS, 5, PANEL2);
            }
            f14.drawString(f14.trimStringToWidth(p.name, cw - 12), cardX + 6, cardY + imgS + 9, TEXT);
            if (in(cx, cy, cardX, cardY, cw, chh) && cy >= top && cy <= top + gridH) {
                openPlaylist(p);
            }
        }
    }

    private void drawLyrics(float x, float y, float w, float h) {
        Track cur = m.getCurrent();
        if (cur == null) {
            f16.drawString("未在播放", x, y + 10, SUB);
            return;
        }
        // 顶部歌曲信息
        f18.drawString(f18.trimStringToWidth(cur.getName(), w), x, y + 4, TEXT);
        f14.drawString(f14.trimStringToWidth(cur.getArtists(), w), x, y + 24, SUB);

        float lyY = y + 44;
        float lyH = h - 44;
        Lyric ly = m.getCurrentLyric();
        if (ly == null || ly.getLines() == null || ly.getLines().isEmpty()) {
            f14.drawString(ly == null ? "歌词加载中…" : "暂无歌词", x, lyY + 10, SUB);
            return;
        }
        List<LyricLine> lines = ly.getLines();
        int cur_i = m.currentLyricLine();
        float lineH = 20;
        // 自动滚动使当前行居中
        float centerOffset = lyH / 2f - lineH / 2f;
        float targetScroll = (cur_i < 0 ? 0 : cur_i * lineH) - centerOffset;
        scroll += (targetScroll - scroll) * 0.2f;
        if (scroll < 0) scroll = Math.max(scroll, 0) == scroll ? scroll : 0;

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            float ly2 = lyY + i * lineH - scroll + centerOffset;
            if (ly2 + lineH < lyY || ly2 > lyY + lyH) continue;
            boolean active = i == cur_i;
            String text = line.getText();
            if (text == null || text.isEmpty()) continue;
            UFontRenderer font = active ? f18 : f16;
            int color = active ? accent() : (line.isMetadata() ? SUB : TEXT);
            font.drawCenteredString(font.trimStringToWidth(text, w), x + w / 2f, ly2, color);
            String tr = line.getTranslation();
            if (active && tr != null && !tr.isEmpty()) {
                f14.drawCenteredString(f14.trimStringToWidth(tr, w), x + w / 2f, ly2 + 16, SUB);
            }
        }
    }

    private void drawPlayerBar(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        Rects.fill(x, y, w, 1, 0xFF2A2A32);
        Track cur = m.getCurrent();

        // 封面 + 标题
        float cs = 40;
        float cImgX = x + 10;
        float cImgY = y + (h - cs) / 2f;
        if (cur != null) {
            ResourceLocation cover = MusicTextures.cover(cur.getCoverUrl());
            if (cover != null) Images.draw(cover, cImgX, cImgY, cs, cs);
            else Rects.rounded((int) cImgX, (int) cImgY, (int) cs, (int) cs, 5, CARD);
            f16.drawString(f16.trimStringToWidth(cur.getName(), 160), cImgX + cs + 8, cImgY + 4, TEXT);
            f14.drawString(f14.trimStringToWidth(cur.getArtists(), 160), cImgX + cs + 8, cImgY + 20, SUB);
        } else {
            Rects.rounded((int) cImgX, (int) cImgY, (int) cs, (int) cs, 5, CARD);
            f14.drawString("未在播放", cImgX + cs + 8, cImgY + 14, SUB);
        }
        if (!m.getStatus().isEmpty()) {
            f14.drawString(f14.trimStringToWidth(m.getStatus(), 200), cImgX + cs + 8, cImgY + 34 - 8, BADGE);
        }

        // 控制按钮（居中）
        float btnY = y + 8;
        float cxc = x + w / 2f;
        float bs = 18;
        // 上一首
        boolean prevH = Hover.is(cxc - 60, btnY, 24, 24, mx, my);
        drawPrev(cxc - 60 + 5, btnY + 6, 12, prevH ? TEXT : SUB);
        if (in(cx, cy, cxc - 60, btnY, 24, 24)) m.prev();
        // 播放/暂停
        boolean pauseState = m.engine().isPlaying();
        Rects.rounded((int) (cxc - 16), (int) (btnY - 2), 32, 28, 14, accent());
        if (pauseState) {
            Rects.fill(cxc - 6, btnY + 3, 4, 16, 0xFFFFFFFF);
            Rects.fill(cxc + 2, btnY + 3, 4, 16, 0xFFFFFFFF);
        } else {
            triangle(cxc - 5, btnY + 3, cxc - 5, btnY + 19, cxc + 8, btnY + 11, 0xFFFFFFFF);
        }
        if (in(cx, cy, cxc - 16, btnY - 2, 32, 28)) {
            if (m.getCurrent() != null) m.togglePause();
        }
        // 下一首
        boolean nextH = Hover.is(cxc + 36, btnY, 24, 24, mx, my);
        drawNext(cxc + 36 + 5, btnY + 6, 12, nextH ? TEXT : SUB);
        if (in(cx, cy, cxc + 36, btnY, 24, 24)) m.next();

        // 进度条
        float barY = y + h - 16;
        float barX = cImgX + cs + 180;
        float barW = x + w - barX - 190;
        if (barW < 60) {
            barX = x + 20;
            barW = w - 220;
        }
        long dur = m.engine().getDurationMs();
        if (dur <= 0 && cur != null) dur = cur.getDurationMs();
        long pos = m.engine().getPositionMs();
        float frac = dur > 0 ? (float) pos / dur : 0;
        if (draggingProgress) frac = previewFrac;
        drawSlider(barX, barY, barW, frac, mx, my, cx, cy, true);
        f14.drawString(fmt((long) (frac * dur)), barX - 36, barY - 4, SUB);
        f14.drawString(fmt(dur), barX + barW + 6, barY - 4, SUB);

        // 音量
        float volW = 70;
        float volX = x + w - volW - 12;
        float volY = y + 12;
        f14.drawString("音量", volX - 30, volY - 4, SUB);
        drawSlider(volX, volY, volW, m.getVolume() / 100f, mx, my, cx, cy, false);
    }

    private void drawSlider(float x, float y, float w, float frac, int mx, int my, int cx, int cy, boolean isProgress) {
        if (frac < 0) frac = 0;
        if (frac > 1) frac = 1;
        float trackH = 4;
        float ty = y;
        Rects.rounded((int) x, (int) ty, (int) w, (int) trackH, 2, TRACK_BG);
        Rects.rounded((int) x, (int) ty, (int) (w * frac), (int) trackH, 2, accent());
        float knobX = x + w * frac;
        Rects.rounded((int) (knobX - 3), (int) (ty - 2), 6, 8, 3, 0xFFFFFFFF);

        boolean hovBar = Hover.is(x, y - 4, w, 12, mx, my);
        boolean down = isMouseDown(0);
        boolean startHere = in(cx, cy, x, y - 4, w, 12);

        if (isProgress) {
            if (startHere) {
                draggingProgress = true;
                previewFrac = clamp01((mx - x) / w);
            }
            if (draggingProgress) {
                if (down) {
                    previewFrac = clamp01((mx - x) / w);
                } else {
                    m.seekFraction(previewFrac);
                    draggingProgress = false;
                }
            }
        } else {
            if (startHere) draggingVolume = true;
            if (draggingVolume) {
                if (down) {
                    int v = Math.round(clamp01((mx - x) / w) * 100);
                    m.setVolume(v);
                } else {
                    draggingVolume = false;
                }
            }
        }
    }

    // ================= 登录弹窗 =================

    private void openLogin() {
        loginOpen = true;
        lastQrState = null;
        startQr();
    }

    private void closeLogin() {
        loginOpen = false;
        m.stopQrLogin();
    }

    private void startQr() {
        m.startQrLogin(new MusicManager.Cb<QrLoginState>() {
            @Override
            public void done(QrLoginState result, Throwable error) {
                lastQrState = result;
                if (result == QrLoginState.CONFIRMED) {
                    loginOpen = false;
                    m.stopQrLogin();
                }
            }
        });
    }

    private void drawLoginModal(float px, float py, float pw, float ph, int mx, int my, int cx, int cy) {
        Rects.fill(px, py, pw, ph, 0xC0000000);
        float mw = 320;
        float mh = m.getSource() == MusicSource.QQ ? 380 : 320;
        float mxp = px + (pw - mw) / 2f;
        float myp = py + (ph - mh) / 2f;
        Rects.rounded((int) mxp, (int) myp, (int) mw, (int) mh, 8, PANEL2);

        f18.drawString((m.getSource() == MusicSource.QQ ? "QQ音乐" : "网易云") + " 登录", mxp + 16, myp + 14, TEXT);

        // 关闭
        boolean ch = Hover.is(mxp + mw - 26, myp + 12, 18, 18, mx, my);
        f16.drawString("x", mxp + mw - 22, myp + 13, ch ? TEXT : SUB);
        if (in(cx, cy, mxp + mw - 28, myp + 10, 24, 24)) {
            closeLogin();
            return;
        }

        // 二维码
        float qs = 180;
        float qx = mxp + (mw - qs) / 2f;
        float qy = myp + 44;
        Rects.rounded((int) qx - 4, (int) qy - 4, (int) qs + 8, (int) qs + 8, 6, 0xFFFFFFFF);
        QrCode qr = m.getQrCode();
        ResourceLocation qrTex = null;
        if (qr != null) {
            String content = qr.getQrContent();
            qrTex = content != null && content.startsWith("data:")
                    ? MusicTextures.base64Image(content)
                    : MusicTextures.qr(content);
        }
        if (qrTex != null) {
            Images.draw(qrTex, qx, qy, qs, qs);
        } else {
            f14.drawCenteredString("二维码生成中…", qx + qs / 2f, qy + qs / 2f - 4, SUB);
        }

        String status = qrStatusText();
        f14.drawCenteredString(status, mxp + mw / 2f, qy + qs + 10, accent());

        // 刷新
        float rbX = mxp + mw / 2f - 40;
        float rbY = qy + qs + 26;
        boolean rh = Hover.is(rbX, rbY, 80, 20, mx, my);
        Rects.rounded((int) rbX, (int) rbY, 80, 20, 5, rh ? CARD_HOVER : CARD);
        f14.drawCenteredString("刷新二维码", rbX + 40, rbY + 6, TEXT);
        if (in(cx, cy, rbX, rbY, 80, 20)) {
            startQr();
        }

        // QQ Cookie 登录
        if (m.getSource() == MusicSource.QQ) {
            float fy = rbY + 30;
            f14.drawString("或手动 Cookie 登录（浏览器 y.qq.com 里复制）", mxp + 16, fy, SUB);
            qqUin.drawTextBox(mxp + 16, fy + 14, mw - 32, 18);
            qqKey.drawTextBox(mxp + 16, fy + 38, mw - 32, 18);
            float lbX = mxp + 16;
            float lbY = fy + 62;
            boolean lh = Hover.is(lbX, lbY, mw - 32, 20, mx, my);
            Rects.rounded((int) lbX, (int) lbY, (int) (mw - 32), 20, 5, lh ? accent() : CARD);
            f14.drawCenteredString("Cookie 登录", lbX + (mw - 32) / 2f, lbY + 6, TEXT);
            if (cx >= 0) {
                qqUin.mouseClicked(cx, cy, 0);
                qqKey.mouseClicked(cx, cy, 0);
            }
            if (in(cx, cy, lbX, lbY, mw - 32, 20)) {
                doQqCookieLogin();
            }
        }
    }

    private void doQqCookieLogin() {
        m.qqCookieLogin(qqUin.getText(), qqKey.getText(), new MusicManager.Cb<Boolean>() {
            @Override
            public void done(Boolean result, Throwable error) {
                if (result != null && result) {
                    loginOpen = false;
                    m.stopQrLogin();
                } else {
                    lastQrState = QrLoginState.ERROR;
                }
            }
        });
    }

    private String qrStatusText() {
        if (lastQrState == null) return "请使用手机 App 扫码";
        switch (lastQrState) {
            case WAITING:
                return "等待扫码…";
            case SCANNED:
                return "已扫码，请在手机确认";
            case CONFIRMED:
                return "登录成功";
            case EXPIRED:
                return "二维码已过期，请刷新";
            default:
                return "登录失败，请刷新重试";
        }
    }

    // ================= 输入 =================

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (loginOpen) {
                closeLogin();
            } else {
                mc.displayGuiScreen(null);
            }
            return;
        }
        if (loginOpen) {
            if (m.getSource() == MusicSource.QQ) {
                if (qqUin.isFocused()) qqUin.textboxKeyTyped(typedChar, keyCode);
                else if (qqKey.isFocused()) qqKey.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        if (searchField != null && searchField.isFocused()) {
            searchField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    private void doSearch() {
        String kw = searchField.getText().trim();
        if (kw.isEmpty()) return;
        tab = Tab.SEARCH;
        scroll = 0;
        resultTitle = "搜索: " + kw;
        resultTracks = new ArrayList<Track>();
        m.search(kw, new MusicManager.Cb<List<Track>>() {
            @Override
            public void done(List<Track> result, Throwable error) {
                if (result != null) resultTracks = result;
                else resultTitle = "搜索失败";
            }
        });
    }

    private void openPlaylist(MusicManager.PlaylistItem p) {
        tab = Tab.SEARCH;
        scroll = 0;
        resultTitle = p.name;
        resultTracks = new ArrayList<Track>();
        m.loadPlaylistTracks(p, new MusicManager.Cb<List<Track>>() {
            @Override
            public void done(List<Track> result, Throwable error) {
                if (result != null) resultTracks = result;
            }
        });
    }

    // ================= 工具 =================

    private void clampScroll(float total, float viewH) {
        float max = Math.max(0, total - viewH);
        if (scroll > max) scroll = max;
        if (scroll < 0) scroll = 0;
    }

    private static boolean in(int cx, int cy, float x, float y, float w, float h) {
        return cx >= 0 && cx >= x && cx < x + w && cy >= y && cy < y + h;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static String fmt(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x3, y3);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }

    private void drawPrev(float x, float y, float s, int color) {
        triangle(x + s * 0.7f, y, x + s * 0.7f, y + s, x, y + s / 2f, color);
        Rects.fill(x, y, 2, s, color);
    }

    private void drawNext(float x, float y, float s, int color) {
        triangle(x + s * 0.3f, y, x + s * 0.3f, y + s, x + s, y + s / 2f, color);
        Rects.fill(x + s - 2, y, 2, s, color);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
