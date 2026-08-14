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

    public final List<CameraKeyframe> keyframes = new ArrayList<CameraKeyframe>();

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
}
