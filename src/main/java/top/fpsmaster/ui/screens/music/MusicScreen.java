package top.fpsmaster.ui.screens.music;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.LyricsDisplay;
import top.fpsmaster.modules.music.MusicManager;
import top.fpsmaster.modules.music.MusicTextures;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.LyricLine;
import top.fpsmaster.music.MusicSource;
import top.fpsmaster.music.QrCode;
import top.fpsmaster.music.QrLoginState;
import top.fpsmaster.music.Track;
import top.fpsmaster.prism.screen.MusicBridge;
import top.fpsmaster.prism.screen.SharedMusic;
import top.fpsmaster.prism.widget.UiFrame;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Edge host for the shared Prism music screen. */
public class MusicScreen extends ScaledGuiScreen {
    private final GuiScreen parent;
    private final MusicManager music = MusicManager.get();
    private final SharedMusic gui = new SharedMusic();
    private final MusicBridge bridge = new EdgeMusicBridge();
    private volatile List<Track> tracks = new ArrayList<Track>();
    private volatile List<MusicManager.PlaylistItem> playlists = new ArrayList<MusicManager.PlaylistItem>();
    private volatile String listTitle = "";
    private volatile String loginStatus = "";

    public MusicScreen() {
        this(null);
    }

    public MusicScreen(GuiScreen parent) {
        this.parent = parent;
        music.setVolume(FPSMaster.configManager.configure.musicVolume);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (gui.draw(EdgeUi.frame(), bridge)) mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (gui.cancelOverlay()) music.stopQrLogin();
            else mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        music.stopQrLogin();
        FPSMaster.configManager.saveConfigQuietly(ConfigProfileUtils.getActiveProfileName());
        super.onGuiClosed();
    }

    private final class EdgeMusicBridge implements MusicBridge {
        @Override public String i18n(String key) { return FPSMaster.i18n.get(key); }
        @Override public boolean qq() { return music.getSource() == MusicSource.QQ; }
        @Override public void setQq(boolean qq) { music.setSource(qq ? MusicSource.QQ : MusicSource.NETEASE); }
        @Override public boolean loggedIn() { return music.isLoggedIn(); }
        @Override public String status() { return music.getStatus(); }
        @Override public String nowTitle() { Track track = music.getCurrent(); return track == null ? "未在播放" : track.getName(); }
        @Override public String nowArtist() { Track track = music.getCurrent(); return track == null ? (qq() ? "QQ 音乐" : "网易云音乐") : track.getArtists(); }
        @Override public boolean playing() { return music.engine().isPlaying(); }
        @Override public boolean paused() { return music.engine().isPaused(); }
        @Override public long positionMs() { return music.engine().getPositionMs(); }
        @Override public long durationMs() { long duration = music.engine().getDurationMs(); Track track = music.getCurrent(); return duration > 0 || track == null ? duration : track.getDurationMs(); }
        @Override public float progress() { long duration = durationMs(); return duration <= 0 ? 0f : Math.min(1f, positionMs() / (float) duration); }
        @Override public float volume() { return music.getVolume() / 100f; }
        @Override public void setVolume(float value) {
            int volume = Math.round(value * 100f);
            music.setVolume(volume);
            FPSMaster.configManager.configure.musicVolume = volume;
        }
        @Override public void seek(float value) { music.seekFraction(value); }
        @Override public void togglePause() { music.togglePause(); }
        @Override public void next() { music.next(); }
        @Override public void prev() { music.prev(); }

        @Override
        public void play(int index) {
            List<Track> snapshot = tracks;
            if (index >= 0 && index < snapshot.size()) music.playList(snapshot, index);
        }

        @Override
        public List<TrackRow> tracks() {
            List<TrackRow> rows = new ArrayList<TrackRow>();
            for (Track track : tracks) {
                rows.add(new TrackRow(track.getName(), track.getArtists(), format(track.getDurationMs()), track.getVip()));
            }
            return rows;
        }

        @Override public String listTitle() { return listTitle; }
        @Override public void search(final String query) {
            listTitle = "搜索: " + query;
            music.search(query, (result, error) -> tracks = result == null ? new ArrayList<Track>() : result);
        }

        @Override public void loadDiscover() {
            listTitle = qq() ? "QQ 热歌榜" : "每日推荐";
            music.loadDiscover((result, error) -> tracks = result == null ? new ArrayList<Track>() : result);
        }

        @Override public void loadPlaylists() {
            music.loadPlaylists((result, error) -> playlists = result == null
                    ? new ArrayList<MusicManager.PlaylistItem>() : result);
        }

        @Override public boolean playlists() { return !playlists.isEmpty(); }

        @Override
        public void openPlaylist(int index) {
            List<MusicManager.PlaylistItem> snapshot = playlists;
            if (index < 0 || index >= snapshot.size()) return;
            MusicManager.PlaylistItem playlist = snapshot.get(index);
            listTitle = playlist.name;
            music.loadPlaylistTracks(playlist, (result, error) -> tracks = result == null ? new ArrayList<Track>() : result);
        }

        @Override
        public List<PlaylistRow> playlistRows() {
            List<PlaylistRow> rows = new ArrayList<PlaylistRow>();
            for (MusicManager.PlaylistItem playlist : playlists) rows.add(new PlaylistRow(playlist.name, String.valueOf(playlist.count)));
            return rows;
        }

        @Override public boolean supportsLogin() { return true; }

        @Override
        public void startLogin() {
            loginStatus = "二维码生成中…";
            music.startQrLogin(new MusicManager.Cb<QrLoginState>() {
                @Override
                public void done(QrLoginState state, Throwable error) {
                    if (error != null || state == QrLoginState.ERROR) loginStatus = "登录失败，请刷新重试";
                    else if (state == QrLoginState.WAITING) loginStatus = "等待扫码…";
                    else if (state == QrLoginState.SCANNED) loginStatus = "已扫码，请在手机确认";
                    else if (state == QrLoginState.CONFIRMED) loginStatus = "登录成功";
                    else if (state == QrLoginState.EXPIRED) loginStatus = "二维码已过期，请刷新";
                }
            });
        }

        @Override public void stopLogin() { music.stopQrLogin(); }
        @Override public void logout() { music.logout(); }
        @Override public String loginStatus() { return loginStatus; }

        @Override
        public void paintLoginQr(UiFrame ui, float x, float y, float size) {
            QrCode qr = music.getQrCode();
            if (qr == null) return;
            String content = qr.getQrContent();
            ResourceLocation texture = content != null && content.startsWith("data:")
                    ? MusicTextures.base64Image(content) : MusicTextures.qr(content);
            if (texture != null) Images.draw(texture, x, y, size, size);
        }

        @Override
        public void submitQqCookie(String musicId, String musicKey) {
            music.qqCookieLogin(musicId, musicKey, (result, error) ->
                    loginStatus = Boolean.TRUE.equals(result) ? "登录成功" : "Cookie 登录失败");
        }

        @Override
        public void paintCover(UiFrame ui, float x, float y, float size) {
            Track track = music.getCurrent();
            if (track == null) return;
            ResourceLocation texture = MusicTextures.cover(track.getCoverUrl());
            if (texture != null) Images.draw(texture, x, y, size, size);
        }

        @Override public boolean hasLyrics() { return true; }
        @Override public boolean lyricsHudEnabled() { return FPSMaster.moduleManager.getModule(LyricsDisplay.class).isEnabled(); }
        @Override public void setLyricsHudEnabled(boolean enabled) { FPSMaster.moduleManager.getModule(LyricsDisplay.class).set(enabled); }
        @Override public float lyricFontSize() { return LyricsDisplay.fontSize.getValue().floatValue(); }
        @Override public void setLyricFontSize(float size) { LyricsDisplay.fontSize.setValue(size); }
        @Override public int lyricLines() { return LyricsDisplay.lines.getValue().intValue(); }
        @Override public void setLyricLines(int lines) { LyricsDisplay.lines.setValue(lines); }
        @Override public boolean lyricTranslation() { return LyricsDisplay.translation.getValue(); }
        @Override public void setLyricTranslation(boolean enabled) { LyricsDisplay.translation.setValue(enabled); }
        @Override public boolean lyricScroll() { return LyricsDisplay.scroll.getValue(); }
        @Override public void setLyricScroll(boolean enabled) { LyricsDisplay.scroll.setValue(enabled); }
        @Override public boolean lyricBackground() { return LyricsDisplay.background.getValue(); }
        @Override public void setLyricBackground(boolean enabled) { LyricsDisplay.background.setValue(enabled); }
        @Override public int currentLyricIndex() { return music.currentLyricLine(); }

        @Override
        public List<LyricRow> lyricRows() {
            List<LyricRow> rows = new ArrayList<LyricRow>();
            Lyric lyric = music.getCurrentLyric();
            if (lyric == null || lyric.getLines() == null) return rows;
            for (LyricLine line : lyric.getLines()) rows.add(new LyricRow(line.getText(), line.getTranslation()));
            return rows;
        }
    }

    private static String format(long millis) {
        long seconds = Math.max(0L, millis) / 1000L;
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }
}
