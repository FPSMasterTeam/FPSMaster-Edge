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
import top.fpsmaster.utils.render.draw.Icons;
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

    // 间距系统：统一用 PAD 作为面板内边距基准
    private static final float PAD = 20f;

    /** Sidebar content (≈215px) + player bar (46px) + margin. Below this the sidebar overflows. */
    private static final float MIN_PANEL_HEIGHT = 300f;

    // 配色（跟随 ClickGUI 主题，见 refreshTheme）
    private static final int DIM = 0xCC000000;
    private static final int BADGE = 0xFFD9A441;
    private int PANEL, PANEL2, CARD, CARD_HOVER, TEXT, SUB, TRACK_BG, DIVIDER;

    private void refreshTheme() {
        // ClickGUI 暗色主题的 panelBg 是透明的（真实面板用 panel.png 图片），直接用会导致面板无背景。
        // 这里用显式不透明色，随主题切换，保证面板始终可见且风格一致。
        boolean light = ClickGuiTheme.isLight();
        PANEL = light ? 0xFFF4F5F7 : 0xFF16161B;
        PANEL2 = light ? 0xFFE8EAEE : 0xFF1F1F26;
        CARD = light ? 0xFFECEEF2 : 0xFF23232B;
        CARD_HOVER = light ? 0xFFE0E3E9 : 0xFF2E2E38;
        TEXT = ClickGuiTheme.textPrimary().getRGB();
        SUB = ClickGuiTheme.textSecondary().getRGB();
        TRACK_BG = light ? 0xFFD6DAE0 : 0xFF33333D;
        DIVIDER = light ? 0x12000000 : 0x14FFFFFF;
    }

    private final MusicManager m;
    private Tab tab = Tab.DISCOVER;
    private Tab previousTab = Tab.DISCOVER;

    private UFontRenderer f14, f16, f18, f20, f24;

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

    private static final String PROGRESS_CAPTURE = "music.progress";
    private static final String VOLUME_CAPTURE = "music.volume";

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
        f24 = FPSMaster.fontManager.s24;

        // initGui also runs on every window resize, and rebuilding the fields there threw away whatever
        // the user had typed — including a pasted QQ musickey, which is tedious to obtain. Keep the
        // existing instances instead of recreating and re-populating them: setText() runs
        // setCursorPosition(), which derives the scroll offset from getWidth(), and width is only
        // assigned inside drawTextBox() — so on a field that has never been drawn it reads 0, pushes
        // lineScrollOffset past the end of the text, and the box renders empty from then on.
        if (searchField == null) {
            searchField = new TextField(f16, "搜索歌曲 / 歌手…", CARD, TEXT, 60, new Runnable() {
                @Override
                public void run() {
                    doSearch();
                }
            });
        }
        if (qqUin == null) {
            qqUin = new TextField(f14, "musicid (uin)", CARD, TEXT, 32);
        }
        if (qqKey == null) {
            qqKey = new TextField(f14, "musickey (qm_keyst)", CARD, TEXT, 256);
        }
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
        // Peek, don't consume: whichever widget actually contains the press claims it inside in().
        ScaledGuiScreen.PointerEvent pe = peekAnyPress();
        boolean click = pe != null && pe.button == 0;
        int cx = click ? pe.x : -1;
        int cy = click ? pe.y : -1;

        Rects.fill(0, 0, guiWidth, guiHeight, DIM);

        float pw = Math.min(guiWidth - 30, 500);
        // The sidebar's content height is fixed (title + 3 nav rows + source header + 2 source rows +
        // the bottom-aligned login button), so a panel shorter than this let the source rows spill past
        // contentBottom and land on top of the player bar — where a single click hit both, starting
        // playback and switching source at once. Keep the panel tall enough that it cannot happen.
        float ph = Math.max(Math.min(guiHeight - 30, 316), MIN_PANEL_HEIGHT);
        float px = (guiWidth - pw) / 2f;
        float py = (guiHeight - ph) / 2f;

        Rects.rounded((int) px, (int) py, (int) pw, (int) ph, 8, PANEL);

        boolean modal = loginOpen;
        int hcx = modal ? -1 : cx;
        int hcy = modal ? -1 : cy;

        float playerH = 46;
        float contentBottom = py + ph - playerH;
        float sidebarW = Math.max(92f, Math.min(122f, pw * 0.24f));

        // 侧边栏（导航 + 来源 + 登录）
        drawSidebar(px, py, sidebarW, contentBottom - py, mx, my, hcx, hcy);
        Rects.fill(px + sidebarW, py + 12, 1, contentBottom - py - 24, DIVIDER);

        // 关闭（右上角）
        float clS = 18;
        float clX = px + pw - clS - 10;
        float clY = py + 10;
        boolean clHov = Hover.is(clX, clY, clS, clS, mx, my);
        Rects.rounded(clX, clY, clS, clS, 5, clHov ? CARD_HOVER : CARD);
        drawCloseIcon(clX + clS / 2f, clY + clS / 2f, 4, clHov ? TEXT : SUB);
        if (in(hcx, hcy, clX, clY, clS, clS)) {
            mc.displayGuiScreen(null);
            return;
        }

        // 主内容区
        float mainX = px + sidebarW + 14;
        float mainW = pw - sidebarW - 26;
        drawMain(mainX, py + 12, mainW, contentBottom - py - 24, mx, my, hcx, hcy);

        // 播放条
        Rects.fill(px + 12, contentBottom, pw - 24, 1, DIVIDER);
        drawPlayerBar(px, contentBottom, pw, playerH, mx, my, hcx, hcy);

        if (loginOpen) {
            drawLoginModal(px, py, pw, ph, mx, my, cx, cy);
        }
    }

    // ================= 侧边栏 =================

    private void drawSidebar(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        float pad = 12;
        f18.drawString("Music", x + pad, y + 13, TEXT);

        float iy = y + 40;
        iy = sidebarNav(x, iy, w, "搜索", Tab.SEARCH, 0, mx, my, cx, cy);
        iy = sidebarNav(x, iy, w, "发现", Tab.DISCOVER, 1, mx, my, cx, cy);
        iy = sidebarNav(x, iy, w, "歌单", Tab.PLAYLISTS, 2, mx, my, cx, cy);

        iy += 12;
        f14.drawString("来源", x + pad, iy, SUB);
        iy += 18;
        iy = sidebarSource(x, iy, w, "网易云", MusicSource.NETEASE, mx, my, cx, cy);
        iy = sidebarSource(x, iy, w, "QQ音乐", MusicSource.QQ, mx, my, cx, cy);

        // 底部：登录 / 退出
        float lbH = 26;
        float lbX = x + pad;
        float lbW = w - pad * 2;
        float lbY = y + h - lbH - 10;
        String label = m.isLoggedIn() ? "退出登录" : "登录";
        boolean lbHov = Hover.is(lbX, lbY, lbW, lbH, mx, my);
        Rects.rounded(lbX, lbY, lbW, lbH, 6, lbHov ? CARD_HOVER : CARD);
        f14.drawCenteredString(label, lbX + lbW / 2f, lbY + lbH / 2f - f14.getHeight() / 2f, m.isLoggedIn() ? accent() : TEXT);
        if (in(cx, cy, lbX, lbY, lbW, lbH)) {
            if (m.isLoggedIn()) m.logout();
            else openLogin();
        }
    }

    private float sidebarNav(float x, float baseY, float w, String label, Tab t, int icon, int mx, int my, int cx, int cy) {
        float pad = 7;
        float ix = x + pad;
        float iw = w - pad * 2;
        float ih = 26;
        boolean active = tab == t;
        boolean hov = Hover.is(ix, baseY, iw, ih, mx, my);
        if (active) Rects.rounded(ix, baseY, iw, ih, 6, withAlpha(accent(), 30));
        else if (hov) Rects.rounded(ix, baseY, iw, ih, 6, CARD);
        int col = active ? accent() : (hov ? TEXT : SUB);
        float iconX = ix + 8;
        drawNavIcon(icon, iconX, baseY + ih / 2f - 5, 10, col);
        f14.drawString(label, iconX + 17, baseY + ih / 2f - f14.getHeight() / 2f, active ? TEXT : col);
        if (in(cx, cy, ix, baseY, iw, ih) && tab != t) {
            tab = t;
            scroll = 0;
            // The search box only gets a chance to lose focus while the search tab is rendering, so
            // without this it stays focused after navigating away and keeps swallowing keystrokes into
            // a field that is no longer on screen.
            if (searchField != null) {
                searchField.setFocused(false);
            }
        }
        return baseY + ih + 3;
    }

    private float sidebarSource(float x, float baseY, float w, String label, MusicSource src, int mx, int my, int cx, int cy) {
        float pad = 7;
        float ix = x + pad;
        float iw = w - pad * 2;
        float ih = 22;
        boolean active = m.getSource() == src;
        boolean hov = Hover.is(ix, baseY, iw, ih, mx, my);
        if (hov && !active) Rects.rounded(ix, baseY, iw, ih, 5, CARD);
        int dot = src == MusicSource.QQ ? 0xFF2FBE77 : 0xFFE7392F;
        Rects.rounded(ix + 8, baseY + ih / 2f - 3, 7, 7, 3, active ? dot : withAlpha(dot, 90));
        f14.drawString(label, ix + 22, baseY + ih / 2f - f14.getHeight() / 2f, active ? TEXT : SUB);
        if (in(cx, cy, ix, baseY, iw, ih) && !active) {
            m.setSource(src);
            onSourceChanged();
        }
        return baseY + ih + 2;
    }

    private static final String[] NAV_ICONS = {"search", "discover", "playlist"};

    private void drawNavIcon(int type, float x, float y, float s, int color) {
        Icons.draw(NAV_ICONS[type], x, y, s, color);
    }

    private void drawCloseIcon(float cx, float cy, float r, int color) {
        float s = r * 2.6f;
        Icons.draw("close", cx - s / 2f, cy - s / 2f, s, color);
    }

    private void onSourceChanged() {
        discoverLoaded = false;
        playlistsLoaded = false;
        discoverTracks = new ArrayList<Track>();
        playlists = new ArrayList<MusicManager.PlaylistItem>();
        scroll = 0;
    }

    private void drawMain(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        // 歌词是全区沉浸视图，不显示页面标题
        if (tab == Tab.LYRICS) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            Scissor.apply(x, y, w, h);
            drawLyrics(x, y, w, h);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }

        String title = tab == Tab.PLAYLISTS ? "歌单" : tab == Tab.SEARCH ? "搜索" : "发现";
        f18.drawString(title, x, y, TEXT);
        float contentY = y + 28;

        if (tab == Tab.SEARCH) {
            float sfW = Math.min(300, w);
            searchField.drawTextBox(x, contentY, sfW, 24);
            if (cx >= 0) searchField.mouseClicked(cx, cy, 0);
            contentY += 34;
        }

        float ch = y + h - contentY;
        int wheel = consumeWheelDelta(x, contentY, w, ch);
        if (wheel != 0) scroll -= wheel / 8f;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(x, contentY, w, ch);
        switch (tab) {
            case DISCOVER:
                ensureDiscover();
                drawTrackList(discoverTracks, discoverSubtitle(), x, contentY, w, ch, cx, cy);
                break;
            case PLAYLISTS:
                ensurePlaylists();
                drawPlaylistGrid(x, contentY, w, ch, mx, my, cx, cy);
                break;
            case SEARCH:
                drawTrackList(resultTracks, resultTitle, x, contentY, w, ch, cx, cy);
                break;
            default:
                break;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private String discoverSubtitle() {
        if (m.getSource() == MusicSource.QQ) return "QQ音乐 · 热歌榜";
        return m.isLoggedIn() ? "网易云 · 每日推荐" : "网易云 · 登录后查看每日推荐";
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
        boolean hasHead = title != null && !title.isEmpty();
        float headH = hasHead ? 18 : 0;
        if (hasHead) f14.drawString(title, x, y, SUB);
        float listY = y + headH;
        float listH = h - headH;

        if (list.isEmpty()) {
            String hint;
            if (tab == Tab.SEARCH) hint = "输入关键词后回车搜索";
            else if (tab == Tab.DISCOVER && m.getSource() == MusicSource.NETEASE && !m.isLoggedIn())
                hint = "登录网易云后可见每日推荐，或用搜索";
            else hint = "暂无内容";
            f14.drawString(hint, x, listY + 8, SUB);
            return;
        }

        clampScroll(list.size() * rowH, listH);
        int mY = getMouseY();

        for (int i = 0; i < list.size(); i++) {
            Track t = list.get(i);
            float ry = listY + i * rowH - scroll;
            if (ry + rowH < listY || ry > listY + listH) continue;
            boolean inView = mY >= listY && mY <= listY + listH;
            boolean hov = inView && Hover.is(x, ry, w, rowH, getMouseX(), mY);
            boolean playing = t == m.getCurrent();
            if (playing) {
                Rects.rounded((int) x, (int) (ry + 1), (int) w, (int) (rowH - 3), 5, withAlpha(accent(), 26));
            } else if (hov) {
                Rects.rounded((int) x, (int) (ry + 1), (int) w, (int) (rowH - 3), 5, CARD);
            }
            // 封面
            float cvS = 28;
            float cvY = ry + (rowH - cvS) / 2f;
            ResourceLocation cover = MusicTextures.cover(t.getCoverUrl());
            if (cover != null) {
                Images.draw(cover, x + 6, cvY, cvS, cvS);
            } else {
                Rects.rounded((int) (x + 6), (int) cvY, (int) cvS, (int) cvS, 4, PANEL2);
            }
            float tx = x + 6 + cvS + 10;
            String dur = fmt(t.getDurationMs());
            float durW = f14.getStringWidth(dur);
            float textMax = w - (tx - x) - durW - 16;
            String name = f16.trimStringToWidth(t.getName(), textMax - (t.getVip() ? 24 : 0));
            f16.drawString(name, tx, ry + 6, playing ? accent() : TEXT);
            if (t.getVip()) {
                float nw = f16.getStringWidth(name);
                Rects.rounded((int) (tx + nw + 5), (int) (ry + 7), 22, 11, 3, withAlpha(BADGE, 45));
                f14.drawString("VIP", tx + nw + 8, ry + 7, BADGE);
            }
            f14.drawString(f14.trimStringToWidth(t.getArtists(), textMax), tx, ry + 21, SUB);
            f14.drawString(dur, x + w - durW - 4, ry + (rowH - f14.getHeight()) / 2f, SUB);

            if (in(cx, cy, x, ry, w, rowH) && cy >= listY && cy <= listY + listH) {
                m.playList(list, i);
            }
        }
    }

    private void drawPlaylistGrid(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        f14.drawString("歌单 · " + sourceName(), x, y, SUB);
        float top = y + 18;
        float gridH = h - 18;
        if (playlists.isEmpty()) {
            String hint = playlistsLoading ? "加载中…" :
                    (m.getSource() == MusicSource.NETEASE && !m.isLoggedIn() ? "登录网易云后可见我的歌单" : "暂无歌单");
            f14.drawString(hint, x, top + 8, SUB);
            return;
        }
        int cols = 3;
        float gap = 10;
        float pad = 6;
        float cw = (w - gap * (cols - 1)) / cols;
        float imgS = cw - pad * 2;
        float chh = imgS + 22; // 图片 + 名字行
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
            Rects.rounded((int) cardX, (int) cardY, (int) cw, (int) chh, 5, hov ? CARD_HOVER : CARD);
            ResourceLocation cover = MusicTextures.cover(p.cover);
            if (cover != null) {
                Images.draw(cover, cardX + pad, cardY + pad, imgS, imgS);
            } else {
                Rects.rounded((int) (cardX + pad), (int) (cardY + pad), (int) imgS, (int) imgS, 4, PANEL2);
            }
            f14.drawString(f14.trimStringToWidth(p.name, cw - pad * 2), cardX + pad, cardY + pad + imgS + 3, TEXT);
            if (in(cx, cy, cardX, cardY, cw, chh) && cy >= top && cy <= top + gridH) {
                openPlaylist(p);
            }
        }
    }

    private void drawLyrics(float x, float y, float w, float h) {
        float cxm = x + w / 2f;
        Track cur = m.getCurrent();
        if (cur == null) {
            f16.drawCenteredString("未在播放", cxm, y + h / 2f - 8, SUB);
            return;
        }
        // 顶部歌曲信息（居中）
        f18.drawCenteredString(f18.trimStringToWidth(cur.getName(), w), cxm, y + 6, TEXT);
        f14.drawCenteredString(f14.trimStringToWidth(cur.getArtists(), w), cxm, y + 26, SUB);

        float lyY = y + 44;
        float lyH = h - 44;
        Lyric ly = m.getCurrentLyric();
        if (ly == null || ly.getLines() == null || ly.getLines().isEmpty()) {
            f14.drawCenteredString(ly == null ? "歌词加载中…" : "暂无歌词", cxm, lyY + lyH / 2f - 6, SUB);
            return;
        }
        List<LyricLine> lines = ly.getLines();
        int cur_i = m.currentLyricLine();
        float lineH = 24;
        // 自动滚动：当前行垂直居中
        float centerOffset = lyH / 2f - lineH / 2f;
        float targetScroll = cur_i < 0 ? 0f : (cur_i * lineH - centerOffset);
        scroll += (targetScroll - scroll) * 0.18f;

        for (int i = 0; i < lines.size(); i++) {
            LyricLine line = lines.get(i);
            float ly2 = lyY + i * lineH - scroll;
            if (ly2 + lineH < lyY || ly2 > lyY + lyH) continue;
            String text = line.getText();
            if (text == null || text.isEmpty()) continue;
            boolean active = i == cur_i;
            int dist = cur_i < 0 ? i : Math.abs(i - cur_i);
            UFontRenderer font = active ? f18 : f16;
            int base = active ? accent() : (line.isMetadata() ? SUB : TEXT);
            int alpha = active ? 255 : Math.max(40, 200 - dist * 42);
            font.drawCenteredString(font.trimStringToWidth(text, w), cxm, ly2, withAlpha(base, alpha));
            String tr = line.getTranslation();
            if (active && tr != null && !tr.isEmpty()) {
                f14.drawCenteredString(f14.trimStringToWidth(tr, w), cxm, ly2 + 16, withAlpha(SUB, 230));
            }
        }
    }

    private void drawPlayerBar(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        Track cur = m.getCurrent();
        float left = x + 14;
        float right = x + w - 12;

        // 顶部整条细进度条（始终可拖动，窄窗口也不丢失）
        long dur = m.engine().getDurationMs();
        if (dur <= 0 && cur != null) dur = cur.getDurationMs();
        long pos = m.engine().getPositionMs();
        float frac = dur > 0 ? (float) pos / dur : 0;
        if (draggingProgress) frac = previewFrac;
        drawSlider(left, y + 5, right - left, frac, mx, my, cx, cy, true);

        float rcy = y + 6 + (h - 6) / 2f;

        // 左：上一首 / 播放 / 下一首（紧凑）
        float pr = 10;
        float prevCx = left + 9;
        float playCx = left + 35;
        float nextCx = left + 61;
        boolean prevH = Hover.is(prevCx - 9, rcy - 9, 18, 18, mx, my);
        Icons.draw("prev", prevCx - 6, rcy - 6, 12, prevH ? TEXT : SUB);
        if (in(cx, cy, prevCx - 9, rcy - 9, 18, 18)) m.prev();
        Rects.rounded(playCx - pr, rcy - pr, pr * 2, pr * 2, (int) pr, accent());
        Icons.draw(m.engine().isPlaying() ? "pause" : "play", playCx - 6, rcy - 6, 12, 0xFFFFFFFF);
        if (in(cx, cy, playCx - pr, rcy - pr, pr * 2, pr * 2) && cur != null) m.togglePause();
        boolean nextH = Hover.is(nextCx - 9, rcy - 9, 18, 18, mx, my);
        Icons.draw("next", nextCx - 6, rcy - 6, 12, nextH ? TEXT : SUB);
        if (in(cx, cy, nextCx - 9, rcy - 9, 18, 18)) m.next();
        float transportRight = left + 72;

        // 右：歌词开关 + 音量（窄窗口只留图标）
        float rx = right;
        boolean showVolSlider = w > 420;
        if (showVolSlider) {
            float vsx = rx - 48;
            drawSlider(vsx, rcy - 1, 48, m.getVolume() / 100f, mx, my, cx, cy, false);
            rx = vsx - 7;
        }
        Icons.draw("volume", rx - 13, rcy - 6, 13, SUB);
        rx -= 13;
        float lyS = 20;
        float lyX = rx - 10 - lyS;
        float lyY = rcy - lyS / 2f;
        boolean lyOn = m.isShowLyricsInGame();
        boolean lyHov = Hover.is(lyX, lyY, lyS, lyS, mx, my);
        Rects.rounded(lyX, lyY, lyS, lyS, 5, lyOn ? withAlpha(accent(), 60) : (lyHov ? CARD_HOVER : CARD));
        Icons.draw("lyrics", lyX + 4, lyY + 4, 12, lyOn ? accent() : SUB);
        if (in(cx, cy, lyX, lyY, lyS, lyS)) m.setShowLyricsInGame(!lyOn);
        float clusterLeft = lyX;

        // 中：正在播放（封面 + 标题/歌手），空间不足时隐藏
        float centerLeft = transportRight + 14;
        float centerRight = clusterLeft - 14;
        float centerW = centerRight - centerLeft;
        if (cur != null && centerW > 130) {
            float cvS = 30;
            float cvX = centerLeft;
            float cvY = rcy - cvS / 2f;
            ResourceLocation cover = MusicTextures.cover(cur.getCoverUrl());
            if (cover != null) Images.draw(cover, cvX, cvY, cvS, cvS);
            else Rects.rounded(cvX, cvY, cvS, cvS, 5, CARD);
            if (Hover.is(cvX, cvY, cvS, cvS, mx, my)) Rects.rounded(cvX, cvY, cvS, cvS, 5, 0x55000000);
            if (in(cx, cy, cvX, cvY, cvS, cvS)) toggleLyrics();
            float tX = cvX + cvS + 10;
            float tW = centerRight - tX;
            f14.drawString(f14.trimStringToWidth(cur.getName(), tW), tX, rcy - 12, TEXT);
            String sub = m.getStatus().isEmpty() ? cur.getArtists() : m.getStatus();
            f14.drawString(f14.trimStringToWidth(sub, tW), tX, rcy + 2, m.getStatus().isEmpty() ? SUB : BADGE);
        } else if (cur == null && centerW > 60) {
            f14.drawCenteredString("未在播放", (centerLeft + centerRight) / 2f, rcy - f14.getHeight() / 2f, SUB);
        }
    }

    private void drawSlider(float x, float y, float w, float frac, int mx, int my, int cx, int cy, boolean isProgress) {
        if (frac < 0) frac = 0;
        if (frac > 1) frac = 1;
        float trackH = 3;
        float ty = y;
        boolean hovBar = Hover.is(x, y - 4, w, 11, mx, my);
        Rects.rounded(x, ty, w, trackH, 1, TRACK_BG);
        Rects.rounded(x, ty, w * frac, trackH, 1, accent());
        // 悬停/拖动时才显示圆形滑块，平时保持极简
        if (hovBar || (isProgress && draggingProgress) || (!isProgress && draggingVolume)) {
            float knobX = x + w * frac;
            Rects.rounded(knobX - 3, ty + trackH / 2f - 3, 6, 6, 3, 0xFFFFFFFF);
        }

        // Routed through the shared drag capture rather than a private boolean, so the capture is
        // released centrally when the button goes up — including when it goes up outside the window,
        // which a locally-tracked flag never learns about and which used to leave the knob glued to
        // the cursor after an alt-tab.
        // in() establishes the hit and claims the press; acquireDrag then takes ownership without
        // asking for a second press that no longer exists. Using beginPointerCapture here instead made
        // the two consumption paths race for the same event, which is why dragging only worked
        // sometimes. Release is still handled centrally on button-up, so an alt-tab cannot strand it.
        Object captureId = isProgress ? PROGRESS_CAPTURE : VOLUME_CAPTURE;
        if (in(cx, cy, x, y - 4, w, 11)) {
            acquireDrag(captureId, 0);
        }
        boolean capturing = isPointerCapturedBy(captureId, 0);

        if (isProgress) {
            if (capturing) {
                draggingProgress = true;
                previewFrac = clamp01((mx - x) / w);
            } else if (draggingProgress) {
                // Commit on release, not continuously — seeking every frame would thrash the decoder.
                m.seekFraction(previewFrac);
                draggingProgress = false;
            }
        } else {
            if (capturing) {
                draggingVolume = true;
                m.setVolume(Math.round(clamp01((mx - x) / w) * 100));
            } else {
                draggingVolume = false;
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
        Rects.fill(px, py, pw, ph, 0xC8000000);
        boolean qq = m.getSource() == MusicSource.QQ;
        float mw = 258;
        float mh = qq ? 296 : 210;
        float mxp = px + (pw - mw) / 2f;
        float myp = py + (ph - mh) / 2f;
        Rects.rounded(mxp, myp, mw, mh, 8, PANEL2);

        f16.drawString((qq ? "QQ音乐" : "网易云") + " 登录", mxp + 14, myp + 12, TEXT);

        // 关闭
        float clS = 18;
        float clX = mxp + mw - clS - 10;
        float clY = myp + 9;
        boolean ch = Hover.is(clX, clY, clS, clS, mx, my);
        Rects.rounded(clX, clY, clS, clS, 5, ch ? CARD_HOVER : CARD);
        drawCloseIcon(clX + clS / 2f, clY + clS / 2f, 4, ch ? TEXT : SUB);
        if (in(cx, cy, clX, clY, clS, clS)) {
            closeLogin();
            return;
        }

        // 二维码
        float qs = 120;
        float qx = mxp + (mw - qs) / 2f;
        float qy = myp + 36;
        Rects.rounded(qx - 4, qy - 4, qs + 8, qs + 8, 5, 0xFFFFFFFF);
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

        f14.drawCenteredString(qrStatusText(), mxp + mw / 2f, qy + qs + 8, accent());

        // 刷新
        float rbW = 78, rbH = 20;
        float rbX = mxp + (mw - rbW) / 2f;
        float rbY = qy + qs + 22;
        boolean rh = Hover.is(rbX, rbY, rbW, rbH, mx, my);
        Rects.rounded(rbX, rbY, rbW, rbH, 5, rh ? CARD_HOVER : CARD);
        f14.drawCenteredString("刷新二维码", rbX + rbW / 2f, rbY + rbH / 2f - f14.getHeight() / 2f, TEXT);
        if (in(cx, cy, rbX, rbY, rbW, rbH)) startQr();

        // QQ Cookie 登录
        if (qq) {
            float fw = mw - 28;
            float fx = mxp + 14;
            float fy = rbY + 26;
            f14.drawString("或手动 Cookie 登录（y.qq.com 复制）", fx, fy, SUB);
            qqUin.drawTextBox(fx, fy + 14, fw, 18);
            qqKey.drawTextBox(fx, fy + 36, fw, 18);
            float lbY = fy + 60;
            boolean lh = Hover.is(fx, lbY, fw, 20, mx, my);
            Rects.rounded(fx, lbY, fw, 20, 5, lh ? accent() : CARD);
            f14.drawCenteredString("Cookie 登录", fx + fw / 2f, lbY + 10 - f14.getHeight() / 2f, TEXT);
            if (cx >= 0) {
                qqUin.mouseClicked(cx, cy, 0);
                qqKey.mouseClicked(cx, cy, 0);
            }
            if (in(cx, cy, fx, lbY, fw, 20)) doQqCookieLogin();
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

    private void toggleLyrics() {
        if (tab == Tab.LYRICS) {
            tab = previousTab;
        } else {
            previousTab = tab;
            tab = Tab.LYRICS;
        }
        scroll = 0;
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

    /**
     * Hit-test that also claims the press.
     *
     * <p>{@code render} used to consume the whole screen up front and hand the raw coordinates to
     * every widget, which then re-tested them independently. Two consequences: overlapping widgets all
     * fired on one press (clicking play while the sidebar had overflowed onto it both started playback
     * and switched source), and {@code beginPointerCapture} could never acquire, because the press it
     * needs had already been eaten — which is why the progress bar could not be dragged.
     *
     * <p>Claiming here makes a press belong to exactly one widget, and leaves it available to the
     * capture-based sliders when no plain widget wants it.
     */
    private boolean in(int cx, int cy, float x, float y, float w, float h) {
        if (cx < 0 || cx < x || cx >= x + w || cy < y || cy >= y + h) {
            return false;
        }
        return consumePressInBounds(x, y, w, h, 0) != null;
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

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
