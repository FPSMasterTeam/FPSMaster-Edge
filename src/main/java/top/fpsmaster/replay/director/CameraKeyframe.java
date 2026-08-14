package top.fpsmaster.replay.director;

/**
 * One camera keyframe on the director timeline.
 *
 * <p>{@link #transition} and {@link #easing} describe how the camera travels from <em>this</em>
 * keyframe to the next one: transition picks the spatial path, easing the speed profile along it.
 * Both are ignored on the last keyframe.
 */
public final class CameraKeyframe {

    /** Spatial path towards the next keyframe. */
    public enum Transition {
        /** Straight line. */
        LINEAR,
        /** Catmull-Rom spline through the neighbouring keyframes. */
        SMOOTH,
        /** Hold this pose until the next keyframe (hard cut). */
        CUT
    }

    /** Speed profile along the path towards the next keyframe. */
    public enum Easing {
        LINEAR,
        /** CSS ease: cubic-bezier(0.25, 0.1, 0.25, 1). */
        EASE,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT
    }

    public int timeMillis;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public float fov;
    public Transition transition = Transition.SMOOTH;
    public Easing easing = Easing.EASE_IN_OUT;

    public CameraKeyframe() {
    }

    public CameraKeyframe(int timeMillis, CameraPose pose) {
        this.timeMillis = timeMillis;
        this.x = pose.x;
        this.y = pose.y;
        this.z = pose.z;
        this.yaw = pose.yaw;
        this.pitch = pose.pitch;
        this.fov = pose.fov;
    }

    public CameraPose pose() {
        return new CameraPose(x, y, z, yaw, pitch, fov);
    }
}
