package top.fpsmaster.modules.music.smtc;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        int smtc_start(Pointer hwnd);
        void smtc_set_callback(ControlCallback cb);
        void smtc_publish(WString title, WString artist, WString album, long positionMs, long durationMs,
                          boolean playing, boolean hasCurrentTrack, byte[] artworkData, int artworkLen);
        void smtc_set_buttons(boolean playPause, boolean next, boolean prev);
        void smtc_close();
        void smtc_get_last_error(Pointer buf, int bufLen);
    }

    private interface ControlCallback extends Callback {
        void invoke(int action);
    }

    private static final AtomicReference<SmtcNative> API = new AtomicReference<>(null);
    private static boolean extractionAttempted = false;

    /**
     * Every native call goes through this single thread. WinRT apartment state is per-thread:
     * initializing it on the game thread would either fail (when something already made that
     * thread an STA) or permanently join the game thread to the MTA. Owning a thread of our own
     * also keeps the blocking {@code smtc_publish} off the caller's thread.
     */
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FPSMaster-SMTC");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean available = new AtomicBoolean(false);
    /** Latest snapshot waiting to be published; newer ones overwrite older, so nothing queues up. */
    private final AtomicReference<MediaPlaybackSnapshot> pending = new AtomicReference<>(null);
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

    /**
     * The HWND of the LWJGL game window. SMTC's desktop interop binds the session to a specific
     * window, and letting the native side guess via EnumWindows can pick the Forge splash screen,
     * which is destroyed moments later. LWJGL 2 keeps the handle private, hence the reflection;
     * failing here is not fatal — the native side falls back to its (filtered) window search.
     */
    private static Pointer resolveGameWindow() {
        try {
            Method getImpl = Class.forName("org.lwjgl.opengl.Display").getDeclaredMethod("getImplementation");
            getImpl.setAccessible(true);
            Object impl = getImpl.invoke(null);
            if (impl == null || !impl.getClass().getName().endsWith("WindowsDisplay")) {
                return null;
            }
            Field hwndField = impl.getClass().getDeclaredField("hwnd");
            hwndField.setAccessible(true);
            long hwnd = hwndField.getLong(impl);
            return hwnd == 0L ? null : new Pointer(hwnd);
        } catch (Throwable t) {
            ClientLogger.warn("SMTC could not resolve the LWJGL window handle: " + t.getMessage());
            return null;
        }
    }

    /** Reads the native failure reason; empty when the DLL has nothing to report. */
    private static String lastNativeError(SmtcNative nativeLib) {
        try {
            Memory buf = new Memory(256L * Native.WCHAR_SIZE);
            nativeLib.smtc_get_last_error(buf, 256);
            String msg = buf.getWideString(0);
            return msg == null ? "" : msg;
        } catch (Throwable t) {
            return "";
        }
    }

    private void loadAndInit() {
        // Resolve the window handle on the caller's thread: LWJGL's Display state belongs to the
        // game thread, and the native call itself is what has to move off it.
        final Pointer hwnd = resolveGameWindow();
        EXEC.execute(() -> {
            try {
                SmtcNative nativeLib = resolveNative();
                if (nativeLib == null) {
                    return;
                }
                nativeLib.smtc_set_callback(callback);
                int status = nativeLib.smtc_start(hwnd);
                if (status != 1) {
                    String err = lastNativeError(nativeLib);
                    ClientLogger.warn("SMTC native start failed, SMTC unavailable"
                            + (err.isEmpty() ? "" : ": " + err));
                    return;
                }
                available.set(true);
            } catch (Throwable t) {
                ClientLogger.error("SMTC native start failed: " + t.getMessage());
                available.set(false);
            }
        });
    }

    @Override
    public void start() {
        // loadAndInit already ran smtc_start on the SMTC thread and the native side is idempotent;
        // nothing to redo here.
    }

    @Override
    public void publish(MediaPlaybackSnapshot snapshot) {
        if (!available.get() || snapshot == null) {
            return;
        }
        // Publishing is a blocking WinRT call. Hand the newest snapshot to the SMTC thread and
        // return immediately; if one is still in flight the newer snapshot simply replaces it.
        boolean queued = pending.getAndSet(snapshot) != null;
        if (queued) {
            return;
        }
        EXEC.execute(() -> {
            MediaPlaybackSnapshot latest = pending.getAndSet(null);
            SmtcNative nativeLib = API.get();
            if (latest == null || nativeLib == null || !available.get()) {
                return;
            }
            try {
                boolean hasTrack = latest.hasCurrentTrack;
                nativeLib.smtc_set_buttons(hasTrack, hasTrack, hasTrack);
                byte[] art = latest.artworkPng;
                nativeLib.smtc_publish(
                        new WString(latest.title),
                        new WString(latest.artist),
                        new WString(latest.album),
                        latest.positionMs,
                        latest.durationMs,
                        latest.playing,
                        hasTrack,
                        art,
                        art == null ? 0 : art.length
                );
            } catch (Throwable t) {
                ClientLogger.error("SMTC publish failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void close() {
        if (!available.compareAndSet(true, false)) {
            return;
        }
        pending.set(null);
        // Wait briefly: this runs during client shutdown and the session should be released before
        // the process exits, but a hung native call must not hold the game open.
        try {
            EXEC.submit(() -> {
                try {
                    SmtcNative nativeLib = API.get();
                    if (nativeLib != null) {
                        nativeLib.smtc_close();
                    }
                } catch (Throwable t) {
                    ClientLogger.error("SMTC close failed: " + t.getMessage());
                }
            }).get(2, TimeUnit.SECONDS);
        } catch (Throwable t) {
            ClientLogger.warn("SMTC close did not finish in time: " + t.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }
}