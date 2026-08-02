package top.fpsmaster.modules.music.smtc;

import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Factory that picks the real Windows SMTC bridge when the OS is Windows and the native library
 * loads, and a no-op otherwise. Never throws from the factory path — the caller (music) must never
 * crash because of media integration.
 */
public final class SystemMediaTransportControlsFactory {

    private SystemMediaTransportControlsFactory() {
    }

    /**
     * Creates the platform-appropriate transport.
     *
     * @param listener control-event listener; may be invoked on a native thread, schedule it to the
     *                 main thread before touching Minecraft/music state.
     */
    public static SystemMediaTransportControls create(MediaControlListener listener) {
        try {
            String os = System.getProperty("os.name", "");
            if (os.toLowerCase(java.util.Locale.ROOT).contains("win")) {
                try {
                    return new WindowsSystemMediaTransportControls(listener);
                } catch (Throwable t) {
                    ClientLogger.error("Windows SMTC bridge unavailable, using no-op: " + t.getMessage());
                    return new NoopSystemMediaTransportControls();
                }
            }
            return new NoopSystemMediaTransportControls();
        } catch (Throwable t) {
            ClientLogger.error("SMTC factory error, using no-op: " + t.getMessage());
            return new NoopSystemMediaTransportControls();
        }
    }
}
