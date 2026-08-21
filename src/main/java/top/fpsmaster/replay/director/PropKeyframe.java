package top.fpsmaster.replay.director;

/**
 * One key on a single {@link CameraChannel}. {@link #a}/{@link #b}/{@link #c} are x/y/z for
 * position and the scalar for every other channel. {@link #easing} shapes time toward the next
 * key; {@link #path} is the spatial interpolation and only matters on {@link CameraChannel#POSITION}.
 */
public final class PropKeyframe {
    public int timeMillis;
    public float a;
    public float b;
    public float c;
    public CameraKeyframe.Easing easing = CameraKeyframe.Easing.EASE_IN_OUT;
    public CameraKeyframe.Transition path = CameraKeyframe.Transition.LINEAR;

    public PropKeyframe() {
    }

    public PropKeyframe(int timeMillis, float a) {
        this.timeMillis = timeMillis;
        this.a = a;
    }

    public PropKeyframe(int timeMillis, float x, float y, float z) {
        this.timeMillis = timeMillis;
        this.a = x;
        this.b = y;
        this.c = z;
    }

    public PropKeyframe copy() {
        PropKeyframe copy = new PropKeyframe();
        copy.timeMillis = timeMillis;
        copy.a = a;
        copy.b = b;
        copy.c = c;
        copy.easing = easing;
        copy.path = path;
        return copy;
    }
}
