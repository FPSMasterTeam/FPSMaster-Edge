package top.fpsmaster.modules.music.smtc;

/**
 * A single immutable snapshot of the currently playing track, as exposed to the system media
 * transport (Windows SMTC). Values are read on the main thread and handed to the bridge, which
 * is free to coalesce/throttle them.
 *
 * <p>{@link #artworkPng} carries the album art as PNG bytes when available (loaded off-thread),
 * or {@code null} when no art has loaded yet.
 */
public final class MediaPlaybackSnapshot {
    public final String title;
    public final String artist;
    public final String album;
    public final long positionMs;
    public final long durationMs;
    public final boolean playing;
    public final boolean hasCurrentTrack;
    public final byte[] artworkPng;

    public MediaPlaybackSnapshot(
            String title, String artist, String album,
            long positionMs, long durationMs,
            boolean playing, boolean hasCurrentTrack,
            byte[] artworkPng) {
        this.title = title == null ? "" : title;
        this.artist = artist == null ? "" : artist;
        this.album = album == null ? "" : album;
        this.positionMs = Math.max(0, positionMs);
        this.durationMs = Math.max(0, durationMs);
        this.playing = playing;
        this.hasCurrentTrack = hasCurrentTrack;
        this.artworkPng = artworkPng;
    }
}
