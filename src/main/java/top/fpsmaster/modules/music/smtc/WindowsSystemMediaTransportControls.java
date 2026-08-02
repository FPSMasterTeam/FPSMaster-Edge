package top.fpsmaster.modules.music.smtc;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.WString;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Windows System Media Transport Controls bridge backed by the bundled {@code fpsmaster-smtc.dll}
 * native library. The DLL is shipped inside the mod jar at {@code native/windows/fpsmaster-smtc.dll}
 * and extracted to a per-run temp directory on first access.
 *
 * <p>If the OS is not Windows, the DLL is missing, or any native call fails, the instance degrades
 * to a safe no-op. Never blocks the render thread, and never crashes music or the game.
 */
final class WindowsSystemMediaTransportControls implements SystemMediaTransportControls {

    private interface SmtcNative extends Library {
        int smtc_start(long hwnd);
        void smtc_set_callback(ControlCallback cb);
        void smtc_publish(WString title, WString artist, WString album, long positionMs, long durationMs,
                          boolean playing, boolean hasCurrentTrack, byte[] artworkData, int artworkLen);
        void smtc_set_buttons(boolean playPause, boolean next, boolean prev);
        void smtc_close();
    }

    private interface ControlCallback extends Callback {
        void invoke(int action);
    }

    private static final AtomicReference<SmtcNative> API = new AtomicReference<>(null);
    private static boolean extractionAttempted = false;

    private final AtomicBoolean available = new AtomicBoolean(false);
    private final MediaControlListener listener;
    private final ControlCallback callback;

    WindowsSystemMediaTransportControls(MediaControlListener listener) {
        this.listener = listener;
        this.callback = action -> {
            try {
                if (listener != null) {
                    switch (action) {
                        case 1: listener.onPlayPause(); break;
                        case 2: listener.onNext(); break;
                        case 3: listener.onPrevious(); break;
                        case 4: listener.onStop(); break;
                        default: break;
                    }
                }
            } catch (Throwable t) {
                ClientLogger.error("SMTC control callback error: " + t.getMessage());
            }
        };
        loadAndInit();
    }

    private static synchronized SmtcNative resolveNative() {
        SmtcNative existing = API.get();
        if (existing != null) {
            return existing;
        }
        if (extractionAttempted) {
            return null;
        }
        extractionAttempted = true;

        try {
            extractAndLoad();
            SmtcNative lib = Native.load("fpsmaster-smtc", SmtcNative.class);
            API.compareAndSet(null, lib);
            return lib;
        } catch (Throwable t) {
            ClientLogger.error("SMTC native load failed: " + t.getMessage());
            return null;
        }
    }

    private static void extractAndLoad() throws Exception {
        String bitness = System.getProperty("os.arch", "").contains("64") ? "x64" : "x86";
        String resourcePath = "/native/windows/" + bitness + "/fpsmaster-smtc.dll";

        File extractDir = new File(System.getProperty("java.io.tmpdir", ""), "fpsmaster-smtc");
        extractDir.mkdirs();
        File dllFile = new File(extractDir, "fpsmaster-smtc.dll");

        // Extract from classpath
        try (InputStream in = WindowsSystemMediaTransportControls.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                ClientLogger.warn("SMTC DLL not found in classpath: " + resourcePath);
                throw new Exception("DLL resource not found: " + resourcePath);
            }
            try (FileOutputStream out = new FileOutputStream(dllFile)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        }

        // Add to JNA search path
        NativeLibrary.addSearchPath("fpsmaster-smtc", extractDir.getAbsolutePath());
    }

    private void loadAndInit() {
        try {
            SmtcNative nativeLib = resolveNative();
            if (nativeLib == null) {
                return;
            }
            nativeLib.smtc_set_callback(callback);
            int status = nativeLib.smtc_start(0L);
            if (status != 1) {
                ClientLogger.warn("SMTC native start returned failure status, SMTC unavailable");
                return;
            }
            available.set(true);
        } catch (Throwable t) {
            ClientLogger.error("SMTC native start failed: " + t.getMessage());
            available.set(false);
        }
    }

    @Override
    public void start() {
        if (!available.get()) {
            return;
        }
        try {
            SmtcNative nativeLib = API.get();
            if (nativeLib != null) {
                nativeLib.smtc_start(0L);
            }
        } catch (Throwable t) {
            ClientLogger.error("SMTC start failed: " + t.getMessage());
        }
    }

    @Override
    public void publish(MediaPlaybackSnapshot snapshot) {
        SmtcNative nativeLib = API.get();
        if (!available.get() || nativeLib == null || snapshot == null) {
            return;
        }
        try {
            boolean hasTrack = snapshot.hasCurrentTrack;
            nativeLib.smtc_set_buttons(hasTrack, hasTrack, hasTrack);
            byte[] art = snapshot.artworkPng;
            nativeLib.smtc_publish(
                    new WString(snapshot.title),
                    new WString(snapshot.artist),
                    new WString(snapshot.album),
                    snapshot.positionMs,
                    snapshot.durationMs,
                    snapshot.playing,
                    hasTrack,
                    art,
                    art == null ? 0 : art.length
            );
        } catch (Throwable t) {
            ClientLogger.error("SMTC publish failed: " + t.getMessage());
        }
    }

    @Override
    public void close() {
        if (!available.get()) {
            return;
        }
        try {
            SmtcNative nativeLib = API.get();
            if (nativeLib != null) {
                nativeLib.smtc_close();
            }
        } catch (Throwable t) {
            ClientLogger.error("SMTC close failed: " + t.getMessage());
        } finally {
            available.set(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }
}