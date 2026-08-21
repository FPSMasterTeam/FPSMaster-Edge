package top.fpsmaster.replay.director;

/**
 * Independently keyable camera properties, After Effects / Premiere style.
 *
 * <p>Position is one property (three channels keyed together). Yaw, pitch, roll and FOV each have
 * their own keyframe list and easing.
 */
public enum CameraChannel {
    POSITION("edit.cam.position", 3),
    YAW("edit.cam.yaw", 1),
    PITCH("edit.cam.pitch", 1),
    ROLL("edit.cam.roll", 1),
    FOV("edit.cam.fov", 1);

    public final String i18n;
    public final int components;

    CameraChannel(String i18n, int components) {
        this.i18n = i18n;
        this.components = components;
    }
}
