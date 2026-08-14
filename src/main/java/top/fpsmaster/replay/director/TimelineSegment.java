package top.fpsmaster.replay.director;

/**
 * One slice of the replay on the director timeline: kept or cut, and played at a speed.
 *
 * <p>Segments partition the recording. The exported movie is the kept segments back to back, each
 * stretched by its speed (0.5 = slow motion at twice the length). Camera keyframes stay addressed
 * in <em>replay</em> time, so slowing a segment slows the camera move through it equally — the
 * shot stays glued to the moment it frames.
 */
public final class TimelineSegment {
    public int startMillis;
    public int endMillis;
    public float speed = 1f;
    public boolean excluded;

    public TimelineSegment() {
    }

    public TimelineSegment(int startMillis, int endMillis) {
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public int sourceLength() {
        return Math.max(0, endMillis - startMillis);
    }

    /** Length this segment occupies in the output, after the speed stretch. */
    public long outputLength() {
        if (excluded) {
            return 0L;
        }
        float clamped = speed <= 0f ? 1f : speed;
        return (long) (sourceLength() / clamped);
    }
}
