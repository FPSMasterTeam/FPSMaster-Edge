package top.fpsmaster.modules.music.smtc;

/**
 * No-op transport facade used on platforms where SMTC is unavailable (non-Windows), or when the
 * Windows bridge fails to load. Keeps the music system fully functional with zero side effects.
 */
final class NoopSystemMediaTransportControls implements SystemMediaTransportControls {
    @Override
    public void start() {
    }

    @Override
    public void publish(MediaPlaybackSnapshot snapshot) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
