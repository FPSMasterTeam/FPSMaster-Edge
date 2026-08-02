package top.fpsmaster.modules.music.smtc;

/**
 * Platform-neutral facade for the system media transport controls (SMTC on Windows).
 *
 * <p>On non-Windows platforms the returned no-op implementation is used, so the music system and
 * the rest of the client behave exactly as before. The concrete bridge only activates after an
 * explicit {@code os.name} Windows check and a successful native-library load; every public method
 * degrades gracefully (no-op) if the bridge is unavailable.
 */
public interface SystemMediaTransportControls {

    /** Initializes the transport. Safe to call multiple times; idempotent. */
    void start();

    /** Publishes the current playback snapshot. Safe/no-op when unavailable. */
    void publish(MediaPlaybackSnapshot snapshot);

    /** Releases the transport and its session. Safe/no-op when never started. */
    void close();

    /** True when this bridge actually backed the transport (Windows + load success). */
    boolean isAvailable();
}
