package top.fpsmaster.replay.director;

/** An immutable camera pose: position, orientation (yaw / pitch / roll) and field of view. */
public final class CameraPose {
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final float fov;
    public final float roll;

    public CameraPose(double x, double y, double z, float yaw, float pitch, float fov) {
        this(x, y, z, yaw, pitch, fov, 0f);
    }

    public CameraPose(double x, double y, double z, float yaw, float pitch, float fov, float roll) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.fov = fov;
        this.roll = roll;
    }
}
