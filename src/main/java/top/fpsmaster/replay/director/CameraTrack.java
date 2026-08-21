package top.fpsmaster.replay.director;

import top.fpsmaster.utils.math.anim.BezierEasing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Camera animation: each {@link CameraChannel} has its own keyframe list and easing, like
 * After Effects. A missing channel holds the fallback pose (the live camera while flying).
 *
 * <p>Legacy {@link #keyframes} (one pose per time) is still loaded and migrated into channels.
 */
public final class CameraTrack {

    private static final BezierEasing EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    private static final BezierEasing EASE_IN = BezierEasing.of(0.42, 0.0, 1.0, 1.0);
    private static final BezierEasing EASE_OUT = BezierEasing.of(0.0, 0.0, 0.58, 1.0);
    private static final BezierEasing EASE_IN_OUT = BezierEasing.of(0.42, 0.0, 0.58, 1.0);
    private static final Comparator<PropKeyframe> BY_TIME = new Comparator<PropKeyframe>() {
        @Override
        public int compare(PropKeyframe a, PropKeyframe b) {
            return Integer.compare(a.timeMillis, b.timeMillis);
        }
    };

    public List<PropKeyframe> position = new ArrayList<PropKeyframe>();
    public List<PropKeyframe> yaw = new ArrayList<PropKeyframe>();
    public List<PropKeyframe> pitch = new ArrayList<PropKeyframe>();
    public List<PropKeyframe> roll = new ArrayList<PropKeyframe>();
    public List<PropKeyframe> fov = new ArrayList<PropKeyframe>();

    /** @deprecated migrated into the per-channel lists on load. */
    public List<CameraKeyframe> keyframes = new ArrayList<CameraKeyframe>();

    /**
     * Cut list. Empty = the whole replay, kept, at 1x. When non-empty the segments are kept
     * sorted, non-overlapping and gap-free over [0, replayDuration] by the edit operations.
     */
    public List<TimelineSegment> segments = new ArrayList<TimelineSegment>();

    public List<PropKeyframe> channel(CameraChannel channel) {
        ensureLists();
        switch (channel) {
            case POSITION:
                return position;
            case YAW:
                return yaw;
            case PITCH:
                return pitch;
            case ROLL:
                return roll;
            case FOV:
                return fov;
            default:
                return position;
        }
    }

    public void ensureLists() {
        if (position == null) {
            position = new ArrayList<PropKeyframe>();
        }
        if (yaw == null) {
            yaw = new ArrayList<PropKeyframe>();
        }
        if (pitch == null) {
            pitch = new ArrayList<PropKeyframe>();
        }
        if (roll == null) {
            roll = new ArrayList<PropKeyframe>();
        }
        if (fov == null) {
            fov = new ArrayList<PropKeyframe>();
        }
        if (keyframes == null) {
            keyframes = new ArrayList<CameraKeyframe>();
        }
        if (segments == null) {
            segments = new ArrayList<TimelineSegment>();
        }
    }

    /** Turns a packed pose list into independent channels. Idempotent. */
    public void migratePackedKeyframes() {
        ensureLists();
        if (keyframes.isEmpty()) {
            return;
        }
        if (!position.isEmpty() || !yaw.isEmpty() || !pitch.isEmpty() || !roll.isEmpty() || !fov.isEmpty()) {
            return;
        }
        for (CameraKeyframe frame : keyframes) {
            if (frame == null) {
                continue;
            }
            PropKeyframe pos = new PropKeyframe(frame.timeMillis, (float) frame.x, (float) frame.y, (float) frame.z);
            pos.easing = frame.easing == null ? CameraKeyframe.Easing.EASE_IN_OUT : frame.easing;
            pos.path = frame.transition == null ? CameraKeyframe.Transition.SMOOTH : frame.transition;
            position.add(pos);
            yaw.add(scalar(frame.timeMillis, frame.yaw, pos.easing));
            pitch.add(scalar(frame.timeMillis, frame.pitch, pos.easing));
            roll.add(scalar(frame.timeMillis, frame.roll, pos.easing));
            fov.add(scalar(frame.timeMillis, frame.fov <= 0f ? 70f : frame.fov, pos.easing));
        }
        sort();
    }

    private static PropKeyframe scalar(int time, float value, CameraKeyframe.Easing easing) {
        PropKeyframe key = new PropKeyframe(time, value);
        key.easing = easing == null ? CameraKeyframe.Easing.EASE_IN_OUT : easing;
        return key;
    }

    public boolean isEmpty() {
        ensureLists();
        return position.isEmpty() && yaw.isEmpty() && pitch.isEmpty() && roll.isEmpty() && fov.isEmpty();
    }

    public boolean drivesLook() {
        ensureLists();
        return !yaw.isEmpty() || !pitch.isEmpty();
    }

    public boolean drivesPosition() {
        ensureLists();
        return !position.isEmpty();
    }

    public int startMillis() {
        int start = Integer.MAX_VALUE;
        for (CameraChannel channel : CameraChannel.values()) {
            List<PropKeyframe> list = channel(channel);
            if (!list.isEmpty()) {
                start = Math.min(start, list.get(0).timeMillis);
            }
        }
        return start == Integer.MAX_VALUE ? 0 : start;
    }

    public int endMillis() {
        int end = 0;
        for (CameraChannel channel : CameraChannel.values()) {
            List<PropKeyframe> list = channel(channel);
            if (!list.isEmpty()) {
                end = Math.max(end, list.get(list.size() - 1).timeMillis);
            }
        }
        return end;
    }

    public void sort() {
        ensureLists();
        for (CameraChannel channel : CameraChannel.values()) {
            Collections.sort(channel(channel), BY_TIME);
        }
        if (keyframes != null) {
            Collections.sort(keyframes, new Comparator<CameraKeyframe>() {
                @Override
                public int compare(CameraKeyframe a, CameraKeyframe b) {
                    return Integer.compare(a.timeMillis, b.timeMillis);
                }
            });
        }
    }

    public PropKeyframe add(CameraChannel channel, int timeMillis, CameraPose pose, int mergeWindowMillis) {
        if (pose == null) {
            return null;
        }
        float[] values;
        if (channel == CameraChannel.POSITION) {
            values = new float[]{(float) pose.x, (float) pose.y, (float) pose.z};
        } else if (channel == CameraChannel.YAW) {
            values = new float[]{pose.yaw};
        } else if (channel == CameraChannel.PITCH) {
            values = new float[]{pose.pitch};
        } else if (channel == CameraChannel.ROLL) {
            values = new float[]{pose.roll};
        } else {
            values = new float[]{pose.fov <= 0f ? 70f : pose.fov};
        }
        return addValues(channel, timeMillis, values, mergeWindowMillis);
    }

    public PropKeyframe addValues(CameraChannel channel, int timeMillis, float[] values, int mergeWindowMillis) {
        ensureLists();
        List<PropKeyframe> list = channel(channel);
        for (PropKeyframe existing : list) {
            if (Math.abs(existing.timeMillis - timeMillis) <= mergeWindowMillis) {
                existing.a = values[0];
                if (channel.components > 1) {
                    existing.b = values[1];
                    existing.c = values[2];
                }
                return existing;
            }
        }
        PropKeyframe key = channel.components > 1
                ? new PropKeyframe(timeMillis, values[0], values[1], values[2])
                : new PropKeyframe(timeMillis, values[0]);
        if (channel == CameraChannel.POSITION) {
            key.path = CameraKeyframe.Transition.SMOOTH;
        }
        list.add(key);
        Collections.sort(list, BY_TIME);
        return key;
    }

    /** Keys every channel at {@code timeMillis} from {@code pose}. */
    public void addPose(int timeMillis, CameraPose pose, int mergeWindowMillis) {
        if (pose == null) {
            return;
        }
        for (CameraChannel channel : CameraChannel.values()) {
            add(channel, timeMillis, pose, mergeWindowMillis);
        }
    }

    /** @deprecated use {@link #addPose}. Kept so older call sites compile during the switch. */
    public CameraKeyframe add(int timeMillis, CameraPose pose, int mergeWindowMillis) {
        addPose(timeMillis, pose, mergeWindowMillis);
        CameraKeyframe packed = new CameraKeyframe(timeMillis, pose);
        return packed;
    }

    public void remove(CameraChannel channel, PropKeyframe key) {
        if (key == null) {
            return;
        }
        channel(channel).remove(key);
    }

    public PropKeyframe nearest(CameraChannel channel, int timeMillis, int window) {
        PropKeyframe best = null;
        int bestDist = window + 1;
        for (PropKeyframe key : channel(channel)) {
            int dist = Math.abs(key.timeMillis - timeMillis);
            if (dist < bestDist) {
                best = key;
                bestDist = dist;
            }
        }
        return best;
    }

    /**
     * Pose at {@code timeMillis}. Channels without keys take {@code hold}. Null only when every
     * channel is empty <em>and</em> hold is null.
     */
    public CameraPose sample(int timeMillis, CameraPose hold) {
        ensureLists();
        migratePackedKeyframes();
        if (isEmpty()) {
            return hold;
        }
        float hx = hold == null ? 0f : (float) hold.x;
        float hy = hold == null ? 0f : (float) hold.y;
        float hz = hold == null ? 0f : (float) hold.z;
        float hyaw = hold == null ? 0f : hold.yaw;
        float hpitch = hold == null ? 0f : hold.pitch;
        float hroll = hold == null ? 0f : hold.roll;
        float hfov = hold == null || hold.fov <= 0f ? 70f : hold.fov;

        float[] pos = interpolateVec3(position, timeMillis, hx, hy, hz);
        float yawV = interpolateScalar(yaw, timeMillis, hyaw, true);
        float pitchV = interpolateScalar(pitch, timeMillis, hpitch, false);
        float rollV = interpolateScalar(roll, timeMillis, hroll, true);
        float fovV = interpolateScalar(fov, timeMillis, hfov, false);
        return new CameraPose(pos[0], pos[1], pos[2], yawV, pitchV, fovV, rollV);
    }

    public CameraPose sample(int timeMillis) {
        return sample(timeMillis, null);
    }

    private static float[] interpolateVec3(List<PropKeyframe> keys, int time,
                                           float hx, float hy, float hz) {
        if (keys == null || keys.isEmpty()) {
            return new float[]{hx, hy, hz};
        }
        PropKeyframe first = keys.get(0);
        if (time <= first.timeMillis) {
            return new float[]{first.a, first.b, first.c};
        }
        PropKeyframe last = keys.get(keys.size() - 1);
        if (time >= last.timeMillis) {
            return new float[]{last.a, last.b, last.c};
        }
        int index = 0;
        while (index < keys.size() - 1 && keys.get(index + 1).timeMillis <= time) {
            index++;
        }
        PropKeyframe from = keys.get(index);
        PropKeyframe to = keys.get(index + 1);
        if (from.path == CameraKeyframe.Transition.CUT) {
            return new float[]{from.a, from.b, from.c};
        }
        float t = easedT(from, to, time);
        if (from.path == CameraKeyframe.Transition.SMOOTH) {
            PropKeyframe before = index > 0 ? keys.get(index - 1) : from;
            PropKeyframe after = index + 2 < keys.size() ? keys.get(index + 2) : to;
            return new float[]{
                    (float) catmullRom(before.a, from.a, to.a, after.a, t),
                    (float) catmullRom(before.b, from.b, to.b, after.b, t),
                    (float) catmullRom(before.c, from.c, to.c, after.c, t)
            };
        }
        return new float[]{
                from.a + (to.a - from.a) * t,
                from.b + (to.b - from.b) * t,
                from.c + (to.c - from.c) * t
        };
    }

    private static float interpolateScalar(List<PropKeyframe> keys, int time, float hold, boolean angular) {
        if (keys == null || keys.isEmpty()) {
            return hold;
        }
        PropKeyframe first = keys.get(0);
        if (time <= first.timeMillis) {
            return first.a;
        }
        PropKeyframe last = keys.get(keys.size() - 1);
        if (time >= last.timeMillis) {
            return last.a;
        }
        int index = 0;
        while (index < keys.size() - 1 && keys.get(index + 1).timeMillis <= time) {
            index++;
        }
        PropKeyframe from = keys.get(index);
        PropKeyframe to = keys.get(index + 1);
        if (from.path == CameraKeyframe.Transition.CUT) {
            return from.a;
        }
        float t = easedT(from, to, time);
        float delta = angular ? shortestArc(to.a - from.a) : (to.a - from.a);
        return from.a + delta * t;
    }

    private static float easedT(PropKeyframe from, PropKeyframe to, int time) {
        float span = to.timeMillis - from.timeMillis;
        float linear = span <= 0f ? 1f : (time - from.timeMillis) / span;
        return ease(from.easing, linear);
    }

    static float ease(CameraKeyframe.Easing easing, float t) {
        if (easing == null) {
            return t;
        }
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

    static float shortestArc(float deltaYaw) {
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

    public List<TimelineSegment> effectiveSegments(int replayDuration) {
        if (!segments.isEmpty()) {
            return segments;
        }
        List<TimelineSegment> whole = new ArrayList<TimelineSegment>(1);
        whole.add(new TimelineSegment(0, Math.max(1, replayDuration)));
        return whole;
    }

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

    public void trimStart(int millis, int replayDuration) {
        materializeSegments(replayDuration);
        splitAt(millis, replayDuration);
        for (TimelineSegment segment : segments) {
            if (segment.endMillis <= millis) {
                segment.excluded = true;
            }
        }
    }

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
        if (segments.size() == 1 && segments.get(0).startMillis == 0
                && !segments.get(0).excluded && segments.get(0).speed == 1f) {
            segments.clear();
        }
    }

    public long outputDurationMillis(int replayDuration) {
        long total = 0;
        for (TimelineSegment segment : effectiveSegments(replayDuration)) {
            total += segment.outputLength();
        }
        return total;
    }

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
        for (int i = kept.size() - 1; i >= 0; i--) {
            if (!kept.get(i).excluded) {
                return kept.get(i).endMillis;
            }
        }
        return replayDuration;
    }

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
