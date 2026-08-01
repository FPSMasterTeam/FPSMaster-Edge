package top.fpsmaster.modules.music.smtc;

/**
 * Receives transport-control events from the system media UI (Windows SMTC buttons).
 *
 * <p>Implementations must <em>not</em> touch Minecraft or music state directly — the bridge may
 * invoke these callbacks on a native/COM thread. The caller schedules them onto the main thread.
 */
public interface MediaControlListener {
    void onPlayPause();

    void onNext();

    void onPrevious();

    void onStop();
}
