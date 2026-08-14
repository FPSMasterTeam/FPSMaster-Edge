package top.fpsmaster.replay.director;

import top.fpsmaster.utils.math.anim.BezierEasing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The camera track of a director timeline: sorted keyframes plus interpolation between them.
 *
 * <p>Position travels along either a straight line or a centripetal-flavoured Catmull-Rom spline
 * through the neighbouring keyframes; time along each segment is shaped by the segment's easing
 * curve. Yaw interpolates along the shortest arc so a pan across the ±180° seam does not whip the
 * whole way around.
 */
public final class CameraTrack {

    private static final BezierEasing EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    private static final BezierEasing EASE_IN = BezierEasing.of(0.42, 0.0, 1.0, 1.0);
    private static final BezierEasing EASE_OUT = BezierEasing.of(0.0, 0.0, 0.58, 1.0);
    private static final BezierEasing EASE_IN_OUT = BezierEasing.of(0.42, 0.0, 0.58, 1.0);

    public List<CameraKeyframe> keyframes = new ArrayList<CameraKeyframe>();

    /**
     * Cut list. Empty = the whole replay, kept, at 1x. When non-empty the segments are kept
     * sorted, non-overlapping and gap-free over [0, replayDuration] by the edit operations.
     */
    public List<TimelineSegment> segments = new ArrayList<TimelineSegment>();

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public int startMillis() {
        return keyframes.isEmpty() ? 0 : keyframes.get(0).timeMillis;
    }

    public int endMillis() {
        return keyframes.isEmpty() ? 0 : keyframes.get(keyframes.size() - 1).timeMillis;
    }

    public void sort() {
        Collections.sort(keyframes, new Comparator<CameraKeyframe>() {
            @Override
            public int compare(CameraKeyframe a, CameraKeyframe b) {
                return Integer.compare(a.timeMillis, b.timeMillis);
            }
        });
    }

    /** Adds (or replaces, when within {@code mergeWindowMillis}) a keyframe and keeps order. */
    public CameraKeyframe add(int timeMillis, CameraPose pose, int mergeWindowMillis) {
        for (CameraKeyframe existing : keyframes) {
            if (Math.abs(existing.timeMillis - timeMillis) <= mergeWindowMillis) {
                existing.x = pose.x;
                existing.y = pose.y;
                existing.z = pose.z;
                existing.yaw = pose.yaw;
                existing.pitch = pose.pitch;
                existing.fov = pose.fov;
                return existing;
            }
        }
        CameraKeyframe frame = new CameraKeyframe(timeMillis, pose);
        keyframes.add(frame);
        sort();
        return frame;
    }

    public void remove(CameraKeyframe frame) {
        keyframes.remove(frame);
    }

    /**
     * The camera pose at {@code timeMillis}. Before the first keyframe the first pose holds;
     * after the last, the last. Null only when the track is empty.
     */
    public CameraPose sample(int timeMillis) {
        if (keyframes.isEmpty()) {
            return null;
        }
        CameraKeyframe first = keyframes.get(0);
        if (timeMillis <= first.timeMillis) {
            return first.pose();
        }
        CameraKeyframe last = keyframes.get(keyframes.size() - 1);
        if (timeMillis >= last.timeMillis) {
            return last.pose();
        }
        int index = 0;
        while (index < keyframes.size() - 1 && keyframes.get(index + 1).timeMillis <= timeMillis) {
            index++;
        }
        CameraKeyframe from = keyframes.get(index);
        CameraKeyframe to = keyframes.get(index + 1);
        if (from.transition == CameraKeyframe.Transition.CUT) {
            return from.pose();
        }
        float span = to.timeMillis - from.timeMillis;
        float linearT = span <= 0f ? 1f : (timeMillis - from.timeMillis) / span;
        float t = ease(from.easing, linearT);

        double px;
        double py;
        double pz;
        if (from.transition == CameraKeyframe.Transition.SMOOTH) {
            CameraKeyframe before = index > 0 ? keyframes.get(index - 1) : from;
            CameraKeyframe after = index + 2 < keyframes.size() ? keyframes.get(index + 2) : to;
            px = catmullRom(before.x, from.x, to.x, after.x, t);
            py = catmullRom(before.y, from.y, to.y, after.y, t);
            pz = catmullRom(before.z, from.z, to.z, after.z, t);
        } else {
            px = from.x + (to.x - from.x) * t;
            py = from.y + (to.y - from.y) * t;
            pz = from.z + (to.z - from.z) * t;
        }

        float yaw = from.yaw + shortestArc(to.yaw - from.yaw) * t;
        float pitch = from.pitch + (to.pitch - from.pitch) * t;
        float fov = from.fov + (to.fov - from.fov) * t;
        return new CameraPose(px, py, pz, yaw, pitch, fov);
    }

    private static float ease(CameraKeyframe.Easing easing, float t) {
        switch (easing) {
            case EASE:
                return (float) EASE.ease(t);
            case EASE_IN:
                return (float) EASE_IN.ease(t);
            case EASE_OUT:
                return (float) EASE_OUT.ease(t);
            case EASE_IN_OUT:
                return (float) EASE_IN_OUT.ease(t);
            default:
                return t;
        }
    }

    private static double catmullRom(double p0, double p1, double p2, double p3, float t) {
        double t2 = t * (double) t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    /** Wraps a yaw delta into [-180, 180] so interpolation takes the short way round. */
    private static float shortestArc(float deltaYaw) {
        float wrapped = deltaYaw % 360f;
        if (wrapped >= 180f) {
            wrapped -= 360f;
        }
        if (wrapped < -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }

    // ------------------------------------------------------------------
    // Cut list / time remapping
    // ------------------------------------------------------------------

    /** The effective segment list: the stored cuts, or one whole-replay segment when there are none. */
    public List<TimelineSegment> effectiveSegments(int replayDuration) {
        if (!segments.isEmpty()) {
            return segments;
        }
        List<TimelineSegment> whole = new ArrayList<TimelineSegment>(1);
        whole.add(new TimelineSegment(0, Math.max(1, replayDuration)));
        return whole;
    }

    /** Ensures the cut list is materialized so edits have something to slice. */
    private void materializeSegments(int replayDuration) {
        if (segments.isEmpty()) {
            segments.add(new TimelineSegment(0, Math.max(1, replayDuration)));
        }
    }

    public void sortSegments() {
        Collections.sort(segments, new Comparator<TimelineSegment>() {
            @Override
            public int compare(TimelineSegment a, TimelineSegment b) {
                return Integer.compare(a.startMillis, b.startMillis);
            }
        });
    }

    /** Splits the segment containing {@code millis} in two; both halves keep its speed/state. */
    public void splitAt(int millis, int replayDuration) {
        materializeSegments(replayDuration);
        for (TimelineSegment segment : new ArrayList<TimelineSegment>(segments)) {
            if (millis > segment.startMillis && millis < segment.endMillis) {
                TimelineSegment tail = new TimelineSegment(millis, segment.endMillis);
                tail.speed = segment.speed;
                tail.excluded = segment.excluded;
                segment.endMillis = millis;
                segments.add(tail);
                sortSegments();
                return;
            }
        }
    }

    /** Drops everything before {@code millis} — the editor's set-in-point. */
    public void trimStart(int millis, int replayDuration) {
        materializeSegments(replayDuration);
        splitAt(millis, replayDuration);
        for (TimelineSegment segment : segments) {
            if (segment.endMillis <= millis) {
                segment.excluded = true;
            }
        }
    }

    /** Drops everything after {@code millis} — the editor's set-out-point. */
    public void trimEnd(int millis, int replayDuration) {
        materializeSegments(replayDuration);
        splitAt(millis, replayDuration);
        for (TimelineSegment segment : segments) {
            if (segment.startMillis >= millis) {
                segment.excluded = true;
            }
        }
    }

    public TimelineSegment segmentAt(int millis, int replayDuration) {
        for (TimelineSegment segment : effectiveSegments(replayDuration)) {
            if (millis >= segment.startMillis && millis < segment.endMillis) {
                return segment;
            }
        }
        return null;
    }

    /** Merges adjacent segments with identical state back together (undo for a stray split). */
    public void mergeAdjacent() {
        sortSegments();
        for (int i = segments.size() - 2; i >= 0; i--) {
            TimelineSegment a = segments.get(i);
            TimelineSegment b = segments.get(i + 1);
            if (a.endMillis == b.startMillis && a.excluded == b.excluded && a.speed == b.speed) {
                a.endMillis = b.endMillis;
                segments.remove(i + 1);
            }
        }
        // A single whole-range default segment is the same as no cut list at all.
        if (segments.size() == 1 && segments.get(0).startMillis == 0
                && !segments.get(0).excluded && segments.get(0).speed == 1f) {
            segments.clear();
        }
    }

    /** Output length of the whole edit in millis: kept segments, each stretched by speed. */
    public long outputDurationMillis(int replayDuration) {
        long total = 0;
        for (TimelineSegment segment : effectiveSegments(replayDuration)) {
            total += segment.outputLength();
        }
        return total;
    }

    /**
     * Maps a moment of the output movie back to replay time. Monotonic, so an exporter walking
     * output time forward also walks replay time forward (jumping across cuts).
     */
    public int mapOutputToSource(long outputMillis, int replayDuration) {
        long acc = 0;
        List<TimelineSegment> kept = effectiveSegments(replayDuration);
        for (TimelineSegment segment : kept) {
            long length = segment.outputLength();
            if (length <= 0) {
                continue;
            }
            if (outputMillis < acc + length) {
                float speed = segment.speed <= 0f ? 1f : segment.speed;
                return segment.startMillis + (int) ((outputMillis - acc) * speed);
            }
            acc += length;
        }
        // Past the end: the last kept moment.
        for (int i = kept.size() - 1; i >= 0; i--) {
            if (!kept.get(i).excluded) {
                return kept.get(i).endMillis;
            }
        }
        return replayDuration;
    }

    /** The first kept moment at or after {@code millis}, or -1 when nothing kept remains. */
    public int nextKeptMillis(int millis, int replayDuration) {
        for (TimelineSegment segment : effectiveSegments(replayDuration)) {
            if (segment.excluded) {
                continue;
            }
            if (millis < segment.endMillis) {
                return Math.max(millis, segment.startMillis);
            }
        }
        return -1;
    }

    public boolean hasKeptContent(int replayDuration) {
        for (TimelineSegment segment : effectiveSegments(replayDuration)) {
            if (!segment.excluded && segment.sourceLength() > 0) {
                return true;
            }
        }
        return false;
    }
}
