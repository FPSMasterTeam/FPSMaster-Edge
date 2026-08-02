package top.fpsmaster.modules.music.smtc;

import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.music.MusicManager;
import top.fpsmaster.modules.music.MusicTextures;
import top.fpsmaster.music.Track;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges the music player into the system media transport.
 *
 * <p>Runs a lightweight polling loop (separate daemon thread) that samples the current track and
 * forwards a snapshot to the transport facade. Control events from the system are marshalled back
 * onto the Minecraft main thread before calling {@link MusicManager} so queue/UI state stays
 * main-thread consistent. Cover art is downloaded on the shared single-threaded
 * {@link MusicTextures} decoder queue to avoid concurrent AWT/ImageIO crashes on macOS.
 */
public final class SmtcMusicBridge {

    private final MusicManager music;
    private final SystemMediaTransportControls controls;
    private final AtomicReference<byte[]> artwork = new AtomicReference<>();
    private volatile String artworkUrl;

    private volatile boolean running;
    private Thread pollThread;
    private volatile Track lastTrack;

    public SmtcMusicBridge(MusicManager music, SystemMediaTransportControls controls) {
        this.music = music;
        this.controls = controls;
    }

    /** Starts the polling loop (idempotent). Does not block the caller. */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        controls.start();
        pollThread = new Thread(this::pollLoop, "FPSMaster-Smtc-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    public void stop() {
        running = false;
        Thread t = pollThread;
        if (t != null) {
            t.interrupt();
        }
        pollThread = null;
        controls.close();
    }

    private void pollLoop() {
        long lastPos = -1;
        boolean lastPlaying = false;
        while (running) {
            try {
                Track cur = music.getCurrent();
                if (cur == null) {
                    if (lastTrack != null || lastPos >= 0) {
                        // No current track → publish cleared state
                        controls.publish(new MediaPlaybackSnapshot("", "", "", 0, 0, false, false, null));
                        lastTrack = null;
                        lastPos = -1;
                        lastPlaying = false;
                    }
                    Thread.sleep(500);
                    continue;
                }

                if (cur != lastTrack) {
                    lastTrack = cur;
                    lastPos = -1;
                    // New track: kick off cover download (cache is keyed by URL, so a track change
                    // always refreshes the art instead of reusing the previous track's thumbnail)
                    requestArtwork(cur.getCoverUrl());
                }

                long pos = music.engine().getPositionMs();
                long dur = music.engine().getDurationMs();
                if (dur <= 0) {
                    dur = cur.getDurationMs();
                }
                boolean playing = music.engine().isPlaying();

                boolean positionChanged = Math.abs(pos - lastPos) >= 500 || lastPos == -1;
                boolean stateChanged = playing != lastPlaying;
                if (positionChanged || stateChanged) {
                    lastPos = pos;
                    lastPlaying = playing;
                    controls.publish(new MediaPlaybackSnapshot(
                            cur.getName(),
                            cur.getArtists(),
                            "",
                            pos,
                            dur,
                            playing,
                            true,
                            artwork.get()
                    ));
                }

                Thread.sleep(250);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                ClientLogger.error("SMTC poll error: " + t.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    /**
     * Requests album art for {@code coverUrl}. Empty/duplicate URLs clear or keep the cache; any
     * other URL triggers a re-download because the previous track's art no longer matches.
     */
    private void requestArtwork(final String coverUrl) {
        if (coverUrl == null || coverUrl.isEmpty()) {
            artwork.set(null);
            artworkUrl = null;
            return;
        }
        if (coverUrl.equals(artworkUrl)) {
            return;
        }
        artworkUrl = coverUrl;
        // 换曲目就立刻丢掉上一首的封面，否则新歌的标题会配着旧歌的图发布出去
        artwork.set(null);
        MusicTextures.downloadPngAsync(coverUrl, png -> {
            // Only accept the result if the track hasn't changed while we were downloading
            if (!coverUrl.equals(artworkUrl)) {
                return;
            }
            // png 为 null（404/超时）时也要落库，保持"无封面"而不是留着上一首的图
            artwork.set(png);
            controls.publish(snapshotFromCurrent());
        });
    }

    private MediaPlaybackSnapshot snapshotFromCurrent() {
        Track cur = music.getCurrent();
        if (cur == null) {
            return new MediaPlaybackSnapshot("", "", "", 0, 0, false, false, null);
        }
        long pos = music.engine().getPositionMs();
        long dur = music.engine().getDurationMs();
        if (dur <= 0) {
            dur = cur.getDurationMs();
        }
        return new MediaPlaybackSnapshot(
                cur.getName(),
                cur.getArtists(),
                "",
                pos,
                dur,
                music.engine().isPlaying(),
                true,
                artwork.get()
        );
    }
}
