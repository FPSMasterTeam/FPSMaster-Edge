package top.fpsmaster.modules.music.smtc;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.music.MusicManager;
import top.fpsmaster.music.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges the music player into the system media transport.
 *
 * <p>Runs a lightweight polling loop (separate daemon thread) that samples the current track and
 * forwards a snapshot to the transport facade. Control events from the system are marshalled back
 * onto the Minecraft main thread before calling {@link MusicManager} so queue/UI state stays
 * main-thread consistent. Cover art is downloaded off-thread with the established UA/referer path.
 */
public final class SmtcMusicBridge {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/123.0.0.0 Safari/537.36";

    private final MusicManager music;
    private final SystemMediaTransportControls controls;
    private final AtomicReference<byte[]> artwork = new AtomicReference<>();

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
                    // New track: kick off cover download if not already cached
                    maybeLoadArtwork(cur.getCoverUrl());
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

    /** Downloads and caches album art as PNG bytes, off-thread. */
    private void maybeLoadArtwork(final String coverUrl) {
        if (coverUrl == null || coverUrl.isEmpty()) {
            artwork.set(null);
            return;
        }
        // Only refresh when we don't already have art for the current track
        if (artwork.get() != null) {
            return;
        }
        final Thread t = new Thread(() -> {
            try {
                byte[] png = downloadPng(coverUrl);
                artwork.set(png);
                // Publish again so SMTC picks up the art
                controls.publish(snapshotFromCurrent());
            } catch (Throwable e) {
                ClientLogger.error("SMTC artwork load failed: " + e.getMessage());
            }
        }, "FPSMaster-Smtc-Art");
        t.setDaemon(true);
        t.start();
    }

    private byte[] downloadPng(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", UA);
        try (InputStream in = conn.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                return null;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        } finally {
            conn.disconnect();
        }
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
