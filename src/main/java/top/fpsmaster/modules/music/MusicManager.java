package top.fpsmaster.modules.music;

import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.music.AudioQuality;
import top.fpsmaster.music.Lyric;
import top.fpsmaster.music.MusicLog;
import top.fpsmaster.music.MusicLogger;
import top.fpsmaster.music.MusicPlaylist;
import top.fpsmaster.music.MusicService;
import top.fpsmaster.music.MusicSource;
import top.fpsmaster.music.NeteaseMusicApi;
import top.fpsmaster.music.PlaylistBrief;
import top.fpsmaster.music.QQMusicApi;
import top.fpsmaster.music.QrCode;
import top.fpsmaster.music.QrLoginState;
import top.fpsmaster.music.SongUrl;
import top.fpsmaster.music.Track;
import top.fpsmaster.music.store.MusicCredentialStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 音乐功能总控：桥接 Cadence（top.fpsmaster.music）数据库与 Edge 的 UI / 播放引擎。
 *
 * <p>所有联网操作（搜索/取直链/歌词/登录）都在 {@link FPSMaster#async} 线程池执行，结果通过
 * {@link Minecraft#addScheduledTask(Runnable)} 回到主线程，绝不阻塞渲染。
 */
public class MusicManager {

    /** 异步回调（在主线程触发）。 */
    public interface Cb<T> {
        void done(T result, Throwable error);
    }

    /** 统一的歌单条目（网易云 PlaylistBrief 与 QQ MusicPlaylist 的合并视图）。 */
    public static class PlaylistItem {
        public final MusicSource source;
        public final String id;
        public final String name;
        public final String cover;
        public final int count;

        public PlaylistItem(MusicSource source, String id, String name, String cover, int count) {
            this.source = source;
            this.id = id;
            this.name = name;
            this.cover = cover;
            this.count = count;
        }
    }

    private static MusicManager INSTANCE;

    /** 全局唯一实例（懒加载）。界面与入口按钮通过它访问音乐能力。 */
    public static MusicManager get() {
        if (INSTANCE == null) {
            INSTANCE = new MusicManager();
        }
        return INSTANCE;
    }

    private final MusicService service = new MusicService();
    private final MusicCredentialStore store = new MusicCredentialStore(resolveAuthFile());
    private final AudioEngine engine = new AudioEngine();

    private static java.nio.file.Path resolveAuthFile() {
        // 与 AuthService 同目录：%APPDATA%/FPSMaster/ 或 ~/.fpsmaster/
        String appData = System.getenv("APPDATA");
        java.nio.file.Path dir = (appData != null && !appData.isEmpty())
                ? java.nio.file.Paths.get(appData, "FPSMaster")
                : java.nio.file.Paths.get(System.getProperty("user.home"), ".fpsmaster");
        return dir.resolve("music_auth.json");
    }

    private MusicSource source = MusicSource.NETEASE;
    private volatile boolean neteaseLoggedIn = false;
    private volatile boolean qqLoggedIn = false;

    private final List<Track> queue = new ArrayList<Track>();
    private int index = -1;
    private volatile Track current = null;
    private volatile Lyric currentLyric = null;
    private volatile String status = "";
    private int volume = 70;

    private final MusicOverlay overlay = new MusicOverlay(this);
    private volatile boolean showLyricsInGame = false;

    // 系统媒体传输控件（Windows SMTC）：平台不可用时自动降级为 no-op。
    private final top.fpsmaster.modules.music.smtc.SmtcMusicBridge smtcBridge;

    // 二维码登录轮询
    private volatile Thread qrThread;
    private volatile QrCode qrCode;
    private volatile QrLoginState qrState = null;

    public MusicManager() {
        routeLogging();
        // Initialize SMTC bridge (no-op on non-Windows or when native fails)
        smtcBridge = new top.fpsmaster.modules.music.smtc.SmtcMusicBridge(
            this,
            top.fpsmaster.modules.music.smtc.SystemMediaTransportControlsFactory.create(
                new top.fpsmaster.modules.music.smtc.MediaControlListener() {
                    @Override
                    public void onPlayPause() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                togglePause();
                            }
                        });
                    }
                    @Override
                    public void onNext() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                next();
                            }
                        });
                    }
                    @Override
                    public void onPrevious() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                prev();
                            }
                        });
                    }
                    @Override
                    public void onStop() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                engine().stop();
                            }
                        });
                    }
                }
            )
        );
        smtcBridge.start();
        try {
            store.load();
            String cookie = store.getNeteaseCookie();
            if (cookie != null && !cookie.isEmpty()) {
                service.getNetease().setCookie(cookie);
                neteaseLoggedIn = true;
            }
            String qqId = store.getQqMusicId();
            String qqKey = store.getQqMusicKey();
            if (qqId != null && !qqId.isEmpty() && qqKey != null && !qqKey.isEmpty()) {
                service.getQq().setMusicid(qqId);
                service.getQq().setMusicKey(qqKey);
                qqLoggedIn = true;
            }
        } catch (Throwable e) {
            ClientLogger.error("Music credential load failed: " + e.getMessage());
        }
    }

    private void routeLogging() {
        try {
            MusicLog.INSTANCE.setLogger(new MusicLogger() {
                @Override
                public void debug(String msg) {
                    ClientLogger.debug("[Music] " + msg);
                }

                @Override
                public void info(String msg) {
                    ClientLogger.info("[Music] " + msg);
                }

                @Override
                public void warn(String msg) {
                    ClientLogger.warn("[Music] " + msg);
                }

                @Override
                public void error(String msg, Throwable t) {
                    ClientLogger.error("[Music] " + msg + (t != null ? ": " + t.getMessage() : ""));
                }
            });
        } catch (Throwable ignored) {
        }
    }

    // ---- 状态 ----

    public MusicService service() {
        return service;
    }

    public AudioEngine engine() {
        return engine;
    }

    public MusicSource getSource() {
        return source;
    }

    public void setSource(MusicSource s) {
        this.source = s;
    }

    public void toggleSource() {
        this.source = (source == MusicSource.NETEASE) ? MusicSource.QQ : MusicSource.NETEASE;
    }

    public boolean isLoggedIn() {
        return source == MusicSource.NETEASE ? neteaseLoggedIn : qqLoggedIn;
    }

    public boolean isLoggedIn(MusicSource s) {
        return s == MusicSource.NETEASE ? neteaseLoggedIn : qqLoggedIn;
    }

    public Track getCurrent() {
        return current;
    }

    public List<Track> getQueue() {
        return queue;
    }

    public Lyric getCurrentLyric() {
        return currentLyric;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String s) {
        this.status = s == null ? "" : s;
    }

    /** 是否在游戏内叠加显示当前歌词。 */
    public boolean isShowLyricsInGame() {
        return showLyricsInGame;
    }

    public void setShowLyricsInGame(boolean b) {
        if (b == showLyricsInGame) return;
        showLyricsInGame = b;
        if (b) {
            EventDispatcher.registerListener(overlay);
        } else {
            EventDispatcher.unregisterListener(overlay);
        }
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int v) {
        if (v < 0) v = 0;
        if (v > 100) v = 100;
        this.volume = v;
        engine.setVolume(v / 100f);
    }

    // ---- 搜索 / 发现 / 歌单 ----

    public void search(final String keyword, Cb<List<Track>> cb) {
        final MusicSource s = source;
        runAsync(new Callable<List<Track>>() {
            @Override
            public List<Track> call() {
                return service.search(s, keyword, 30);
            }
        }, cb);
    }

    /** 发现页（可直接播放的曲目列表）：QQ 热歌榜 / 网易云每日推荐（需登录）。 */
    public void loadDiscover(Cb<List<Track>> cb) {
        final MusicSource s = source;
        runAsync(new Callable<List<Track>>() {
            @Override
            public List<Track> call() {
                if (s == MusicSource.QQ) {
                    return service.getQq().getToplist(26, 50);
                }
                if (neteaseLoggedIn) {
                    return service.getNetease().getDailyRecommendSongs();
                }
                return new ArrayList<Track>();
            }
        }, cb);
    }

    /** 歌单网格：QQ 推荐歌单 / 网易云我的歌单（需登录）。 */
    public void loadPlaylists(Cb<List<PlaylistItem>> cb) {
        final MusicSource s = source;
        runAsync(new Callable<List<PlaylistItem>>() {
            @Override
            public List<PlaylistItem> call() {
                List<PlaylistItem> out = new ArrayList<PlaylistItem>();
                if (s == MusicSource.QQ) {
                    for (MusicPlaylist p : service.getQq().getRecommendPlaylists(12)) {
                        out.add(new PlaylistItem(MusicSource.QQ, p.getId(), p.getName(), p.getCoverUrl(), p.getTrackCount()));
                    }
                } else if (neteaseLoggedIn) {
                    Long uid = service.getNetease().getLoginUid();
                    if (uid != null) {
                        for (PlaylistBrief p : service.getNetease().getUserPlaylists(uid, 30, 0)) {
                            out.add(new PlaylistItem(MusicSource.NETEASE, p.getId(), p.getName(), p.getCoverUrl(), p.getTrackCount()));
                        }
                    }
                }
                return out;
            }
        }, cb);
    }

    /** 歌单内曲目。 */
    public void loadPlaylistTracks(final PlaylistItem item, Cb<List<Track>> cb) {
        runAsync(new Callable<List<Track>>() {
            @Override
            public List<Track> call() {
                if (item.source == MusicSource.QQ) {
                    return service.getQq().getPlaylistTracks(item.id, 100);
                }
                return service.getNetease().getPlaylistTracks(item.id, 1000);
            }
        }, cb);
    }

    // ---- 播放 ----

    public void playList(List<Track> list, int startIndex) {
        queue.clear();
        if (list != null) queue.addAll(list);
        index = startIndex;
        if (index >= 0 && index < queue.size()) {
            playInternal(queue.get(index));
        }
    }

    public void play(Track track) {
        int idx = queue.indexOf(track);
        if (idx >= 0) {
            index = idx;
        } else {
            queue.add(track);
            index = queue.size() - 1;
        }
        playInternal(track);
    }

    private void playInternal(final Track track) {
        current = track;
        currentLyric = null;
        status = "加载中…";
        final MusicSource src = track.getSource();
        // 请求 STANDARD（mp3）以保证 mp3spi 可解码
        FPSMaster.async.execute(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    final SongUrl url = service.getSongUrl(track, AudioQuality.STANDARD);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (current != track) return; // 已切歌
                            if (url.getAvailable()) {
                                String referer = src == MusicSource.QQ ? "https://y.qq.com/" : null;
                                status = "";
                                engine.setVolume(volume / 100f);
                                engine.play(url.getUrl(), referer, track.getDurationMs(), new Runnable() {
                                    @Override
                                    public void run() {
                                        next();
                                    }
                                });
                            } else {
                                String reason = url.getReason();
                                status = reason != null ? reason : "无法播放";
                            }
                        }
                    });
                    loadLyric(track);
                } catch (final Throwable e) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            status = "加载失败: " + e.getMessage();
                        }
                    });
                }
                return null;
            }
        });
    }

    private void loadLyric(final Track track) {
        FPSMaster.async.execute(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    final Lyric ly = service.getLyric(track);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (current == track) currentLyric = ly;
                        }
                    });
                } catch (Throwable e) {
                    ClientLogger.error("Music lyric load failed: " + e.getMessage());
                }
                return null;
            }
        });
    }

    public void togglePause() {
        engine.togglePause();
    }

    public void next() {
        if (queue.isEmpty()) return;
        index = (index + 1) % queue.size();
        playInternal(queue.get(index));
    }

    public void prev() {
        if (queue.isEmpty()) return;
        index = (index - 1 + queue.size()) % queue.size();
        playInternal(queue.get(index));
    }

    public void seekFraction(float f) {
        long dur = engine.getDurationMs();
        if (dur <= 0 && current != null) dur = current.getDurationMs();
        if (dur > 0) engine.seek((long) (f * dur));
    }

    /** 当前行歌词索引（按播放位置），无则 -1。 */
    public int currentLyricLine() {
        Lyric ly = currentLyric;
        if (ly == null) return -1;
        List<top.fpsmaster.music.LyricLine> lines = ly.getLines();
        if (lines == null || lines.isEmpty()) return -1;
        long pos = engine.getPositionMs();
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getStartMs() <= pos) idx = i;
            else break;
        }
        return idx;
    }

    // ---- 登录 ----

    /** 开始二维码登录：创建二维码并启动轮询线程。onUpdate 每次状态变化在主线程回调。 */
    public void startQrLogin(final Cb<QrLoginState> onUpdate) {
        stopQrLogin();
        final MusicSource s = source;
        qrState = null;
        qrCode = null;
        FPSMaster.async.execute(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    final QrCode qr = service.createQrCode(s);
                    qrCode = qr;
                    Thread poll = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pollQr(s, qr, onUpdate);
                        }
                    }, "FPSMaster-Music-QR");
                    poll.setDaemon(true);
                    qrThread = poll;
                    poll.start();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (onUpdate != null) onUpdate.done(QrLoginState.WAITING, null);
                        }
                    });
                } catch (final Throwable e) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (onUpdate != null) onUpdate.done(QrLoginState.ERROR, e);
                        }
                    });
                }
                return null;
            }
        });
    }

    private void pollQr(MusicSource s, QrCode qr, final Cb<QrLoginState> onUpdate) {
        Thread self = Thread.currentThread();
        for (int i = 0; i < 90; i++) { // 最多轮询 ~3 分钟
            if (qrThread != self) return;
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                return;
            }
            if (qrThread != self) return;
            final QrLoginState st;
            try {
                st = service.checkQrCode(s, qr);
            } catch (Throwable e) {
                continue;
            }
            qrState = st;
            if (st == QrLoginState.CONFIRMED) {
                onQrConfirmed(s);
            }
            post(new Runnable() {
                @Override
                public void run() {
                    if (onUpdate != null) onUpdate.done(st, null);
                }
            });
            if (st == QrLoginState.CONFIRMED || st == QrLoginState.EXPIRED || st == QrLoginState.ERROR) {
                return;
            }
        }
    }

    private void onQrConfirmed(MusicSource s) {
        try {
            if (s == MusicSource.NETEASE) {
                store.setNetease(service.getNetease().getCookie());
                neteaseLoggedIn = true;
            } else {
                QQMusicApi qq = service.getQq();
                store.setQq(qq.getMusicid(), qq.getMusicKey());
                qqLoggedIn = true;
            }
        } catch (Throwable e) {
            ClientLogger.error("Music login persist failed: " + e.getMessage());
        }
    }

    public QrCode getQrCode() {
        return qrCode;
    }

    public QrLoginState getQrState() {
        return qrState;
    }

    public void stopQrLogin() {
        Thread t = qrThread;
        qrThread = null;
        if (t != null) t.interrupt();
    }

    /** QQ 手动 Cookie 登录：uin(musicid) + qm_keyst(musickey)。 */
    public void qqCookieLogin(final String musicid, final String musickey, final Cb<Boolean> cb) {
        runAsync(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                QQMusicApi qq = service.getQq();
                String uin = musicid.trim();
                if (uin.startsWith("o0")) uin = uin.substring(2);
                while (uin.startsWith("0") && uin.length() > 1) uin = uin.substring(1);
                qq.setMusicid(uin);
                qq.setMusicKey(musickey.trim());
                boolean ok = qq.getLoggedIn();
                if (ok) {
                    store.setQq(qq.getMusicid(), qq.getMusicKey());
                    qqLoggedIn = true;
                }
                return ok;
            }
        }, cb);
    }

    public void logout() {
        MusicSource s = source;
        try {
            if (s == MusicSource.NETEASE) {
                service.getNetease().clearLogin();
                store.clearNetease();
                neteaseLoggedIn = false;
            } else {
                service.getQq().clearLogin();
                store.clearQq();
                qqLoggedIn = false;
            }
        } catch (Throwable e) {
            ClientLogger.error("Music logout failed: " + e.getMessage());
        }
    }

    // ---- 内部 ----

    private <T> void runAsync(final Callable<T> task, final Cb<T> cb) {
        FPSMaster.async.execute(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    final T r = task.call();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (cb != null) cb.done(r, null);
                        }
                    });
                } catch (final Throwable e) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (cb != null) cb.done(null, e);
                        }
                    });
                }
                return null;
            }
        });
    }

    private void post(Runnable r) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(r);
        } else {
            r.run();
        }
    }

    /** Releases the SMTC bridge and its native session. Safe to call multiple times. */
    public void shutdownSmtc() {
        if (smtcBridge != null) {
            smtcBridge.stop();
        }
    }
}
