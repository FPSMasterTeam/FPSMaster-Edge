package top.fpsmaster.ui.screens.music;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.music.MusicManager;
import top.fpsmaster.modules.music.MusicTextures;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.LyricLine;
import top.fpsmaster.music.MusicSource;
import top.fpsmaster.music.QrCode;
import top.fpsmaster.music.QrLoginState;
import top.fpsmaster.music.Track;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 音乐界面：源切换（网易云 / QQ）、搜索、发现、歌单、扫码/Cookie 登录、播放控制、进度/音量、歌词。
 *
 * <p>数据来自开源库 Cadence（top.fpsmaster:music-api），播放走 {@link top.fpsmaster.modules.music.AudioEngine}。
 * 视觉层对应原型 {@code docs/prototypes/music.html}（1 GUI 单位 = 原型 CSS px / 2），
 * 配色走 {@link ClickGuiTheme}，通用控件走 {@link UiChrome}。
 */
public class MusicScreen extends ScaledGuiScreen {

    private enum Tab {DISCOVER, PLAYLISTS, SEARCH, LYRICS}

    /** VIP 标签金色（原型 #d9a441，与主题无关）。 */
    private static final int BADGE = 0xFFD9A441;

    /** 左列「正在播放」宽度（原型 .now 320px）。 */
    private static final float NOW_W = 160f;

    private final MusicManager m;
    private Tab tab = Tab.DISCOVER;
    private Tab previousTab = Tab.DISCOVER;

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

    /** 收藏按钮的本地视觉状态（暂无收藏后端）。 */
    private boolean liked = false;

    /** 歌单占位渐变的近似纯色（原型卡片渐变的取中值）。 */
    private static final int[] ART_PLACEHOLDER = {0xFF58A6D6, 0xFF8CC16A, 0xFF9A6FD0, 0xFFE0608A};

    public MusicScreen() {
        this.m = MusicManager.get();
    }

    @Override
    public void initGui() {
        super.initGui();
        // initGui also runs on every window resize, and rebuilding the fields there threw away whatever
        // the user had typed — including a pasted QQ musickey, which is tedious to obtain. Keep the
        // existing instances instead of recreating and re-populating them: setText() runs
        // setCursorPosition(), which derives the scroll offset from getWidth(), and width is only
        // assigned inside drawTextBox() — so on a field that has never been drawn it reads 0, pushes
        // lineScrollOffset past the end of the text, and the box renders empty from then on.
        // Surfaces (searchBox/inputBox) are drawn by UiChrome, so the fields keep a transparent bg.
        if (searchField == null) {
            searchField = new TextField(FPSMaster.fontManager.getFont(13), "搜索歌曲、歌手、专辑…",
                    0x00000000, text(), 60, new Runnable() {
                @Override
                public void run() {
                    doSearch();
                }
            });
        }
        if (qqUin == null) {
            qqUin = new TextField(FPSMaster.fontManager.getFont(12), "musicid (uin)", 0x00000000, text(), 32);
        }
        if (qqKey == null) {
            qqKey = new TextField(FPSMaster.fontManager.getFont(12), "musickey (qm_keyst)", 0x00000000, text(), 256);
        }
    }

    private int accent() {
        return ClickGuiTheme.accent().getRGB();
    }

    private static int text() {
        return ClickGuiTheme.textPrimary().getRGB();
    }

    private static int sub() {
        return ClickGuiTheme.textSecondary().getRGB();
    }

    private static int dis() {
        return ClickGuiTheme.textDisabled().getRGB();
    }

    private static UFontRenderer f(int size) {
        return FPSMaster.fontManager.getFont(size);
    }

    // ================= 渲染 =================

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        int mx = getMouseX();
        int my = getMouseY();
        // Peek, don't consume: whichever widget actually contains the press claims it inside in().
        ScaledGuiScreen.PointerEvent pe = peekAnyPress();
        boolean click = pe != null && pe.button == 0;
        int cx = click ? pe.x : -1;
        int cy = click ? pe.y : -1;

        UiChrome.veil(guiWidth, guiHeight, 1f);

        float pw = Math.min(480f, guiWidth - 24f);
        float ph = Math.min(290f, guiHeight - 32f);
        float px = (guiWidth - pw) / 2f;
        float py = (guiHeight - ph) / 2f;

        UiChrome.panel(px, py, pw, ph);

        boolean modal = loginOpen;
        int hcx = modal ? -1 : cx;
        int hcy = modal ? -1 : cy;

        // 左列底衬（原型有径向渐变，这里用轻微加深近似）+ 竖分隔线
        Rects.fill(px + 1, py + 1, NOW_W - 1, ph - 2, ClickGuiTheme.mask(40).getRGB());
        UiChrome.hairlineV(px + NOW_W, py + 1, ph - 2);

        drawNowPlaying(px, py, NOW_W, ph, mx, my, hcx, hcy);

        // 右上角关闭
        float clS = 16f;
        float clX = px + pw - 7f - clS;
        float clY = py + 7f;
        boolean clHov = Hover.is(clX, clY, clS, clS, mx, my);
        UiChrome.ghostButton(clX, clY, clS, clS, clHov);
        Icons.draw("close", clX + (clS - 8f) / 2f, clY + (clS - 8f) / 2f, 8f, clHov ? text() : sub());
        if (in(hcx, hcy, clX, clY, clS, clS)) {
            mc.displayGuiScreen(null);
            return;
        }

        float bx = px + NOW_W;
        float bw = pw - NOW_W;
        drawBrowseHead(bx, py, bw, clX, mx, my, hcx, hcy);

        float toolbarY = py + 34f;
        drawToolbar(bx, toolbarY, bw, hcx, hcy, mx, my);

        float contentX = bx + 14f;
        float contentW = bw - 24f;
        float contentY = toolbarY + UiChrome.SEARCH_H + 5f;
        float contentH = py + ph - 8f - contentY;
        drawMain(contentX, contentY, contentW, contentH, mx, my, hcx, hcy);

        if (loginOpen) {
            drawLoginModal(mx, my, cx, cy);
        }
    }

    // ================= 正在播放 =================

    private void drawNowPlaying(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        Track cur = m.getCurrent();

        // 封面 95x95，顶部 padding 20，水平居中
        float cover = 95f;
        float cvX = x + (w - cover) / 2f;
        float cvY = y + 20f;
        ResourceLocation art = cur == null ? null : MusicTextures.cover(cur.getCoverUrl());
        if (art != null) {
            Images.drawSmooth(art, cvX, cvY, cover, cover, -1);
        } else {
            Rects.rounded(cvX, cvY, cover, cover, 10, ClickGuiTheme.layerActive().getRGB(), false);
            Icons.draw("music", cvX + cover / 2f - 12f, cvY + cover / 2f - 12f, 24f, dis());
        }
        if (in(cx, cy, cvX, cvY, cover, cover)) {
            toggleLyrics();
        }

        // 标题 + 歌手行
        UFontRenderer f16 = FPSMaster.fontManager.s16;
        UFontRenderer f12 = f(12);
        String title = cur == null ? "未在播放" : cur.getName();
        String artist = cur == null ? sourceName() : (m.getStatus().isEmpty() ? cur.getArtists() : m.getStatus());
        UiChrome.boldCentered(f16, f16.trimStringToWidth(title, w - 20f), x + w / 2f, cvY + cover + 11f, text());
        f12.drawCenteredString(f12.trimStringToWidth(artist, w - 20f), x + w / 2f, cvY + cover + 21f,
                m.getStatus().isEmpty() ? sub() : BADGE);

        // 歌词预览 3 行
        drawLyricPreview(x, cvY + cover + 33f, w);

        // ---- 底部对齐区：进度条 / 时间 / 控制排 / 音量排 ----
        long dur = m.engine().getDurationMs();
        if (dur <= 0 && cur != null) dur = cur.getDurationMs();
        long pos = m.engine().getPositionMs();
        float frac = dur > 0 ? (float) pos / dur : 0;
        if (draggingProgress) frac = previewFrac;

        float barX = x + 16f;
        float barW = w - 32f;
        float barBoxY = y + h - 83f;
        drawSlider(barX, barBoxY, barW, frac, mx, my, cx, cy, true);
        UFontRenderer f10 = f(10);
        float timesY = barBoxY + 11f;
        f10.drawString(formatMs(pos), barX, timesY, dis());
        String durText = formatMs(dur);
        f10.drawString(durText, barX + barW - f10.getStringWidth(durText), timesY, dis());

        // 控制排：heart / prev / play / next / lyrics（小钮 18，主钮 24）
        float ctlCy = y + h - 45f;
        float bs = 18f;
        float ps = 24f;
        float rowW = bs * 4f + ps + 4f * 2f + 6f * 2f;
        float ix = x + (w - rowW) / 2f;

        boolean heartHov = Hover.is(ix, ctlCy - bs / 2f, bs, bs, mx, my);
        UiChrome.ghostButton(ix, ctlCy - bs / 2f, bs, bs, heartHov);
        Icons.draw("heart", ix + (bs - 8f) / 2f, ctlCy - 4f, 8f,
                liked ? ClickGuiTheme.danger().getRGB() : (heartHov ? text() : sub()));
        if (in(cx, cy, ix, ctlCy - bs / 2f, bs, bs)) liked = !liked;
        ix += bs + 4f;

        boolean prevHov = Hover.is(ix, ctlCy - bs / 2f, bs, bs, mx, my);
        UiChrome.ghostButton(ix, ctlCy - bs / 2f, bs, bs, prevHov);
        Icons.draw("prev", ix + (bs - 8f) / 2f, ctlCy - 4f, 8f, prevHov ? text() : sub());
        if (in(cx, cy, ix, ctlCy - bs / 2f, bs, bs)) m.prev();
        ix += bs + 6f;

        Rects.rounded(ix, ctlCy - ps / 2f, ps, ps, (int) (ps / 2f), accent(), false);
        Icons.draw(m.engine().isPlaying() ? "pause" : "play", ix + (ps - 10f) / 2f, ctlCy - 5f, 10f, 0xFFFFFFFF);
        if (in(cx, cy, ix, ctlCy - ps / 2f, ps, ps) && cur != null) m.togglePause();
        ix += ps + 6f;

        boolean nextHov = Hover.is(ix, ctlCy - bs / 2f, bs, bs, mx, my);
        UiChrome.ghostButton(ix, ctlCy - bs / 2f, bs, bs, nextHov);
        Icons.draw("next", ix + (bs - 8f) / 2f, ctlCy - 4f, 8f, nextHov ? text() : sub());
        if (in(cx, cy, ix, ctlCy - bs / 2f, bs, bs)) m.next();
        ix += bs + 4f;

        boolean lyOn = tab == Tab.LYRICS;
        boolean lyHov = Hover.is(ix, ctlCy - bs / 2f, bs, bs, mx, my);
        UiChrome.ghostButton(ix, ctlCy - bs / 2f, bs, bs, lyHov);
        Icons.draw("lyrics", ix + (bs - 8f) / 2f, ctlCy - 4f, 8f,
                lyOn ? accent() : (lyHov ? text() : sub()));
        if (in(cx, cy, ix, ctlCy - bs / 2f, bs, bs)) toggleLyrics();

        // 音量排
        float volIconY = y + h - 23f;
        Icons.draw("volume", x + 16f, volIconY, 7.5f, sub());
        float vsX = x + 16f + 7.5f + 5f;
        drawSlider(vsX, y + h - 25f, x + w - 16f - vsX, m.getVolume() / 100f, mx, my, cx, cy, false);
    }

    private void drawLyricPreview(float x, float y, float w) {
        UFontRenderer f13 = f(13);
        UFontRenderer f12 = f(12);
        Lyric ly = m.getCurrentLyric();
        if (ly == null || ly.getLines() == null || ly.getLines().isEmpty()) {
            f12.drawCenteredString(ly == null ? "歌词加载中…" : "暂无歌词", x + w / 2f, y + 11f, dis());
            return;
        }
        List<LyricLine> lines = ly.getLines();
        int cur = m.currentLyricLine();
        int start = Math.max(0, cur - 1);
        float lyY = y;
        for (int i = start; i < Math.min(lines.size(), start + 3); i++) {
            LyricLine line = lines.get(i);
            if (line.getText() == null || line.getText().isEmpty()) {
                continue;
            }
            boolean on = i == cur;
            if (on) {
                UiChrome.boldCentered(f13, f13.trimStringToWidth(line.getText(), w - 20f), x + w / 2f, lyY, text());
            } else {
                f12.drawCenteredString(f12.trimStringToWidth(line.getText(), w - 20f), x + w / 2f, lyY, dis());
            }
            lyY += 11f;
        }
    }

    private static String formatMs(long ms) {
        if (ms <= 0) {
            return "0:00";
        }
        long total = ms / 1000L;
        return (total / 60L) + ":" + String.format("%02d", (int) (total % 60L));
    }

    // ================= 浏览头部 / 工具栏 =================

    private void drawBrowseHead(float x, float y, float w, float closeX, int mx, int my, int cx, int cy) {
        UFontRenderer f16 = FPSMaster.fontManager.s16;
        UFontRenderer f12 = f(12);
        UFontRenderer f11 = f(11);

        UiChrome.boldString(f16, greeting(), x + 14f, y + 11f, text());
        f11.drawString(greetingSub(), x + 14f, y + 21f, sub());

        float rowH = 15f;
        float rowY = y + 11f;
        boolean qq = m.getSource() == MusicSource.QQ;

        // 账户胶囊（头像圆 11 + 文本），未登录时显示"登录"
        String accLabel = m.isLoggedIn() ? "已登录" : "登录";
        float labelW = f12.getStringWidth(accLabel);
        float pillW = 2f + 11f + 4f + labelW + 6f;
        float pillX = closeX - 6f - pillW;
        boolean accHov = Hover.is(pillX, rowY, pillW, rowH, mx, my);
        Rects.rounded(pillX - 0.5f, rowY - 0.5f, pillW + 1f, rowH + 1f, 8,
                ClickGuiTheme.stroke().getRGB(), false);
        Rects.rounded(pillX, rowY, pillW, rowH, 7,
                (accHov ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer()).getRGB(), false);
        float avaX = pillX + 2f;
        float avaY = rowY + 2f;
        Rects.rounded(avaX, avaY, 11f, 11f, 5, qq ? 0xFF2FBE77 : 0xFFE7392F, false);
        Icons.draw("user", avaX + 2.5f, avaY + 2.5f, 6f, 0xFFFFFFFF);
        f12.drawString(accLabel, avaX + 11f + 4f, rowY + rowH / 2f - 3f, text());
        if (in(cx, cy, pillX, rowY, pillW, rowH)) {
            if (m.isLoggedIn()) m.logout();
            else openLogin();
        }

        // 源分段控件（网易云 / QQ 音乐）
        String[] srcLabels = {"网易云", "QQ 音乐"};
        float segPad = 1.5f;
        float[] optW = new float[2];
        float segW = segPad * 2f;
        for (int i = 0; i < 2; i++) {
            optW[i] = f12.getStringWidth(srcLabels[i]) + 14f;
            segW += optW[i];
        }
        float segX = pillX - 6f - segW;
        UiChrome.seg(segX, rowY, segW, rowH);
        float ox = segX + segPad;
        for (int i = 0; i < 2; i++) {
            boolean selected = (i == 1) == qq;
            boolean hov = Hover.is(ox, rowY, optW[i], rowH, mx, my);
            UiChrome.segOption(ox, rowY + segPad, optW[i], rowH - segPad * 2f, srcLabels[i], selected, hov);
            if (in(cx, cy, ox, rowY, optW[i], rowH) && !selected) {
                m.setSource(i == 1 ? MusicSource.QQ : MusicSource.NETEASE);
                onSourceChanged();
            }
            ox += optW[i];
        }
    }

    private void drawToolbar(float x, float y, float w, int cx, int cy, int mx, int my) {
        float h = UiChrome.SEARCH_H;
        UFontRenderer f12 = f(12);

        // 分段控件：发现 / 我的歌单 / 搜索
        Tab[] tabs = {Tab.DISCOVER, Tab.PLAYLISTS, Tab.SEARCH};
        String[] labels = {"发现", "我的歌单", "搜索"};
        float segPad = 1.5f;
        float[] optW = new float[tabs.length];
        float segW = segPad * 2f;
        for (int i = 0; i < tabs.length; i++) {
            optW[i] = f12.getStringWidth(labels[i]) + 18f;
            segW += optW[i];
        }
        float segX = x + 14f;
        UiChrome.seg(segX, y, segW, h);
        float ox = segX + segPad;
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = tab == tabs[i];
            boolean hov = Hover.is(ox, y, optW[i], h, mx, my);
            UiChrome.segOption(ox, y + segPad, optW[i], h - segPad * 2f, labels[i], selected, hov);
            if (in(cx, cy, ox, y, optW[i], h) && tab != tabs[i]) {
                tab = tabs[i];
                scroll = 0;
                // The search box only gets a chance to lose focus while it is being clicked, so
                // without this it would keep swallowing keystrokes after navigating away.
                if (searchField != null) {
                    searchField.setFocused(false);
                }
            }
            ox += optW[i];
        }

        // 搜索框（searchField 逻辑保留，回车触发 doSearch）
        float sw = 115f;
        float sx = x + w - 14f - sw;
        UiChrome.searchBox(sx, y, sw, h, searchField.isFocused());
        Icons.draw("search", sx + 6f, y + (h - 7f) / 2f, 7f, sub());
        searchField.backGroundColor = 0x00000000;
        searchField.fontColor = text();
        searchField.drawTextBox(sx + 16f, y + 2f, sw - 22f, h - 4f);
        if (cx >= 0) {
            searchField.mouseClicked(cx, cy, 0);
            // 落在搜索框内的按压就地消费，避免穿透到其他控件
            in(cx, cy, sx, y, sw, h);
        }
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String time = hour < 6 ? "夜深了" : hour < 12 ? "早上好" : hour < 18 ? "下午好" : "晚上好";
        return time + "，" + (m.getSource() == MusicSource.QQ ? "Q音听友" : "云村居民");
    }

    private String greetingSub() {
        if (m.getSource() == MusicSource.QQ) return "热歌榜已更新";
        return m.isLoggedIn() ? "每日推荐已更新" : "登录后查看每日推荐";
    }

    private void onSourceChanged() {
        discoverLoaded = false;
        playlistsLoaded = false;
        discoverTracks = new ArrayList<Track>();
        playlists = new ArrayList<MusicManager.PlaylistItem>();
        scroll = 0;
    }

    // ================= 内容区 =================

    private void drawMain(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        // 歌词是全区沉浸视图
        if (tab == Tab.LYRICS) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            Scissor.apply(x, y, w, h);
            drawLyrics(x, y, w, h);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }

        int wheel = consumeWheelDelta(x, y, w, h);
        if (wheel != 0) scroll -= wheel / 8f;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(x, y, w, h);
        switch (tab) {
            case DISCOVER:
                ensureDiscover();
                drawTrackSection(discoverTracks,
                        m.getSource() == MusicSource.QQ ? "热歌榜" : "每日推荐", x, y, w, h, cx, cy);
                break;
            case PLAYLISTS:
                ensurePlaylists();
                drawPlaylistGrid(x, y, w, h, mx, my, cx, cy);
                break;
            case SEARCH:
                drawTrackSection(resultTracks, resultTitle.isEmpty() ? "搜索" : resultTitle, x, y, w, h, cx, cy);
                break;
            default:
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

    /** 小节标题（伪粗 13 + 数量 + 右侧"播放全部"）+ 曲目行列表。 */
    private void drawTrackSection(List<Track> list, String title, float x, float y, float w, float h, int cx, int cy) {
        UFontRenderer f13 = f(13);
        UFontRenderer f12 = f(12);
        UFontRenderer f11 = f(11);
        UFontRenderer f10 = f(10);
        UFontRenderer f9 = f(9);

        float headH = 13f;
        String head = f13.trimStringToWidth(title, w - 70f);
        UiChrome.boldString(f13, head, x, y, text());
        if (!list.isEmpty()) {
            f11.drawString(list.size() + " 首", x + f13.getStringWidth(head) + 4f, y + 1.5f, dis());
            String all = "播放全部";
            float aw = f11.getStringWidth(all);
            f11.drawString(all, x + w - aw, y + 1.5f, ClickGuiTheme.accentText().getRGB());
            if (in(cx, cy, x + w - aw - 3f, y - 1f, aw + 6f, 10f)) {
                m.playList(list, 0);
            }
        }
        float listY = y + headH;
        float listH = h - headH;

        if (list.isEmpty()) {
            String hint;
            if (tab == Tab.SEARCH) hint = "输入关键词后回车搜索";
            else if (tab == Tab.DISCOVER && m.getSource() == MusicSource.NETEASE && !m.isLoggedIn())
                hint = "登录网易云后可见每日推荐，或用搜索";
            else hint = "暂无内容";
            f12.drawString(hint, x, listY + 8f, sub());
            return;
        }

        float rowH = 16f;
        clampScroll(list.size() * rowH, listH);
        int mX = getMouseX();
        int mY = getMouseY();

        // 列布局：序号(右对齐 10) | 曲名(弹性) | 歌手(65) | 时长(右对齐 20)，行内边距 5
        float idxRight = x + 5f + 10f;
        float nameX = idxRight + 6f;
        float artW = 65f;
        float artX = x + w - 5f - 20f - 6f - artW;

        for (int i = 0; i < list.size(); i++) {
            Track t = list.get(i);
            float ry = listY + i * rowH - scroll;
            if (ry + rowH < listY || ry > listY + listH) continue;
            boolean inView = mY >= listY && mY <= listY + listH;
            boolean hov = inView && Hover.is(x, ry, w, rowH, mX, mY);
            boolean playing = t == m.getCurrent();
            if (playing) {
                Rects.rounded(x, ry + 0.5f, w, rowH - 1f, 5, ClickGuiTheme.accentSoft().getRGB(), false);
            } else if (hov) {
                Rects.rounded(x, ry + 0.5f, w, rowH - 1f, 5, ClickGuiTheme.layer().getRGB(), false);
            }

            int accText = ClickGuiTheme.accentText().getRGB();
            if (playing) {
                Icons.draw("play", idxRight - 6f, ry + (rowH - 6f) / 2f, 6f, accText);
            } else {
                String no = String.valueOf(i + 1);
                f11.drawString(no, idxRight - f11.getStringWidth(no), ry + rowH / 2f - 2.5f, dis());
            }

            String dur = fmt(t.getDurationMs());
            float durW = f11.getStringWidth(dur);
            f11.drawString(dur, x + w - 5f - durW, ry + rowH / 2f - 2.5f, dis());

            f11.drawString(f11.trimStringToWidth(t.getArtists(), artW), artX, ry + rowH / 2f - 2.5f, sub());

            float vipW = t.getVip() ? f9.getStringWidth("VIP") + 4f : 0f;
            float nameMax = artX - 6f - nameX - (t.getVip() ? vipW + 3f : 0f);
            String name = f13.trimStringToWidth(t.getName(), nameMax);
            f13.drawString(name, nameX, ry + rowH / 2f - 3.2f, playing ? accText : text());
            if (t.getVip()) {
                float tagX = nameX + f13.getStringWidth(name) + 3f;
                float tagY = ry + (rowH - 7f) / 2f;
                Rects.rounded(tagX, tagY, vipW, 7f, 2, withAlpha(BADGE, 46), false);
                f9.drawCenteredString("VIP", tagX + vipW / 2f, tagY + 1.2f, BADGE);
            }

            if (in(cx, cy, x, ry, w, rowH) && cy >= listY && cy <= listY + listH) {
                m.playList(list, i);
            }
        }
    }

    /** 歌单卡片 3 列网格（封面区 42 + 信息区）。 */
    private void drawPlaylistGrid(float x, float y, float w, float h, int mx, int my, int cx, int cy) {
        UFontRenderer f13 = f(13);
        UFontRenderer f12 = f(12);
        UFontRenderer f11 = f(11);
        UFontRenderer f10 = f(10);

        UiChrome.boldString(f13, "我的歌单", x, y, text());
        if (!playlists.isEmpty()) {
            f11.drawString(playlists.size() + " 个", x + f13.getStringWidth("我的歌单") + 4f, y + 1.5f, dis());
        }
        float top = y + 13f;
        float gridH = h - 13f;

        if (playlists.isEmpty()) {
            String hint = playlistsLoading ? "加载中…" :
                    (m.getSource() == MusicSource.NETEASE && !m.isLoggedIn() ? "登录网易云后可见我的歌单" : "暂无歌单");
            f12.drawString(hint, x, top + 8f, sub());
            return;
        }

        int cols = 3;
        float cw = (w - 20f) / 3f;
        float gap = (w - cw * cols) / (cols - 1);
        float artH = 42f;
        float chh = artH + 22f;
        float vGap = 8f;
        int rows = (playlists.size() + cols - 1) / cols;
        clampScroll(rows * (chh + vGap), gridH);

        for (int i = 0; i < playlists.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            float cardX = x + col * (cw + gap);
            float cardY = top + row * (chh + vGap) - scroll;
            if (cardY + chh < top || cardY > top + gridH) continue;
            MusicManager.PlaylistItem p = playlists.get(i);
            boolean hov = Hover.is(cardX, cardY, cw, chh, mx, my) && my >= top && my <= top + gridH;
            UiChrome.card(cardX, cardY, cw, chh, hov, false);

            ResourceLocation cover = MusicTextures.cover(p.cover);
            if (cover != null) {
                Images.drawSmooth(cover, cardX + 1f, cardY + 1f, cw - 2f, artH - 1f, -1);
            } else {
                Rects.rounded(cardX + 1f, cardY + 1f, cw - 2f, artH - 1f, 5,
                        ART_PLACEHOLDER[i % ART_PLACEHOLDER.length], false);
            }
            if (hov) {
                float fs = 14f;
                float fx = cardX + cw - 4f - fs;
                float fy = cardY + artH - 4f - fs;
                Rects.rounded(fx, fy, fs, fs, 7, 0x8C000000, false);
                Icons.draw("play", fx + 4f, fy + 4f, 6f, 0xFFFFFFFF);
            }

            f12.drawString(f12.trimStringToWidth(p.name, cw - 11f), cardX + 5.5f, cardY + artH + 4.5f, text());
            String count = p.count > 0 ? p.count + " 首" : sourceName();
            f10.drawString(f10.trimStringToWidth(count, cw - 11f), cardX + 5.5f, cardY + artH + 11.5f, dis());

            if (in(cx, cy, cardX, cardY, cw, chh) && cy >= top && cy <= top + gridH) {
                openPlaylist(p);
            }
        }
    }

    private void drawLyrics(float x, float y, float w, float h) {
        float cxm = x + w / 2f;
        Track cur = m.getCurrent();
        UFontRenderer f14 = FPSMaster.fontManager.s14;
        UFontRenderer f12 = f(12);
        UFontRenderer f11 = f(11);
        if (cur == null) {
            f12.drawCenteredString("未在播放", cxm, y + h / 2f - 3f, sub());
            return;
        }
        // 顶部歌曲信息（居中）
        UiChrome.boldCentered(f14, f14.trimStringToWidth(cur.getName(), w), cxm, y + 4f, text());
        f11.drawCenteredString(f11.trimStringToWidth(cur.getArtists(), w), cxm, y + 13f, sub());

        float lyY = y + 24f;
        float lyH = h - 24f;
        Lyric ly = m.getCurrentLyric();
        if (ly == null || ly.getLines() == null || ly.getLines().isEmpty()) {
            f12.drawCenteredString(ly == null ? "歌词加载中…" : "暂无歌词", cxm, lyY + lyH / 2f - 3f, sub());
            return;
        }
        List<LyricLine> lines = ly.getLines();
        int cur_i = m.currentLyricLine();
        float lineH = 14f;
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
            if (active) {
                UiChrome.boldCentered(f14, f14.trimStringToWidth(text, w), cxm, ly2, text());
            } else {
                int alpha = Math.max(40, 200 - dist * 42);
                f12.drawCenteredString(f12.trimStringToWidth(text, w), cxm, ly2, withAlpha(dis(), alpha));
            }
            String tr = line.getTranslation();
            if (active && tr != null && !tr.isEmpty()) {
                f11.drawCenteredString(f11.trimStringToWidth(tr, w), cxm, ly2 + 7.5f, withAlpha(sub(), 230));
            }
        }
    }

    private void drawSlider(float x, float boxY, float w, float frac, int mx, int my, int cx, int cy, boolean isProgress) {
        if (frac < 0) frac = 0;
        if (frac > 1) frac = 1;
        boolean hovBar = Hover.is(x, boxY, w, UiChrome.SLIDER_H, mx, my);
        // 悬停/拖动时才显示圆形滑块，平时保持极简
        boolean showThumb = hovBar || (isProgress && draggingProgress) || (!isProgress && draggingVolume);
        UiChrome.slider(x, boxY, w, frac, showThumb);

        // Routed through the shared drag capture rather than a private boolean, so the capture is
        // released centrally when the button goes up — including when it goes up outside the window,
        // which a locally-tracked flag never learns about and which used to leave the knob glued to
        // the cursor after an alt-tab.
        // in() establishes the hit and claims the press; acquireDrag then takes ownership without
        // asking for a second press that no longer exists. Using beginPointerCapture here instead made
        // the two consumption paths race for the same event, which is why dragging only worked
        // sometimes. Release is still handled centrally on button-up, so an alt-tab cannot strand it.
        Object captureId = isProgress ? PROGRESS_CAPTURE : VOLUME_CAPTURE;
        if (in(cx, cy, x, boxY, w, UiChrome.SLIDER_H)) {
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

    private void drawLoginModal(int mx, int my, int cx, int cy) {
        UiChrome.veil(guiWidth, guiHeight, 0.9f);
        boolean qq = m.getSource() == MusicSource.QQ;
        float mw = 190f;
        float mh = qq ? 210f : 134f;
        float mxp = (guiWidth - mw) / 2f;
        float myp = (guiHeight - mh) / 2f;
        UiChrome.panel(mxp, myp, mw, mh);

        UFontRenderer f14 = FPSMaster.fontManager.s14;
        UFontRenderer f11 = f(11);
        UiChrome.boldString(f14, (qq ? "QQ音乐" : "网易云") + " 登录", mxp + 12f, myp + 9f, text());

        // 关闭
        float clS = 14f;
        float clX = mxp + mw - clS - 6f;
        float clY = myp + 6f;
        boolean ch = Hover.is(clX, clY, clS, clS, mx, my);
        UiChrome.ghostButton(clX, clY, clS, clS, ch);
        Icons.draw("close", clX + (clS - 7f) / 2f, clY + (clS - 7f) / 2f, 7f, ch ? text() : sub());
        if (in(cx, cy, clX, clY, clS, clS)) {
            closeLogin();
            return;
        }

        // 二维码（70x70 白底圆角）
        float qs = 70f;
        float qx = mxp + (mw - qs) / 2f;
        float qy = myp + 26f;
        Rects.rounded(qx - 3f, qy - 3f, qs + 6f, qs + 6f, 6, 0xFFFFFFFF, false);
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
            f11.drawCenteredString("二维码生成中…", qx + qs / 2f, qy + qs / 2f - 3f, 0xFF555555);
        }

        f11.drawCenteredString(qrStatusText(), mxp + mw / 2f, qy + qs + 7f, ClickGuiTheme.accentText().getRGB());

        // 刷新
        float rbW = 64f, rbH = 14f;
        float rbX = mxp + (mw - rbW) / 2f;
        float rbY = qy + qs + 15f;
        if (UiChrome.buttonClicked(this, rbX, rbY, rbW, rbH, "refresh", "刷新二维码", UiChrome.Style.DEFAULT, mx, my)) {
            startQr();
        }

        // QQ Cookie 登录
        if (qq) {
            float fx = mxp + 12f;
            float fw = mw - 24f;
            float fy = rbY + rbH + 8f;
            f11.drawString("或手动 Cookie 登录（y.qq.com 复制）", fx, fy, sub());
            qqUin.backGroundColor = 0x00000000;
            qqUin.fontColor = text();
            qqKey.backGroundColor = 0x00000000;
            qqKey.fontColor = text();
            float in1Y = fy + 8f;
            UiChrome.inputBox(fx, in1Y, fw, UiChrome.INPUT_H, qqUin.isFocused());
            qqUin.drawTextBox(fx + 2f, in1Y + 1f, fw - 4f, UiChrome.INPUT_H - 2f);
            float in2Y = in1Y + UiChrome.INPUT_H + 4f;
            UiChrome.inputBox(fx, in2Y, fw, UiChrome.INPUT_H, qqKey.isFocused());
            qqKey.drawTextBox(fx + 2f, in2Y + 1f, fw - 4f, UiChrome.INPUT_H - 2f);
            if (cx >= 0) {
                qqUin.mouseClicked(cx, cy, 0);
                qqKey.mouseClicked(cx, cy, 0);
            }
            float lbY = in2Y + UiChrome.INPUT_H + 7f;
            if (UiChrome.buttonClicked(this, fx, lbY, fw, 15f, null, "Cookie 登录", UiChrome.Style.PRIMARY, mx, my)) {
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
