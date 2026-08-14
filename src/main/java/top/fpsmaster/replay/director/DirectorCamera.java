package top.fpsmaster.replay.director;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import top.fpsmaster.replay.ReplayPlayer;

import java.io.File;

/**
 * Runtime side of the director: owns the camera track of the replay currently playing, applies it
 * to the free camera during preview and export, and exposes the FOV override for the renderer.
 *
 * <p>The free camera <em>is</em> {@code mc.thePlayer} (a spectator — see ReplayPlayer.openWorld),
 * so "moving the camera" means writing the player's position. Preview writes once per client tick
 * with the previous tick's pose left in {@code prev*}, so the renderer interpolates between track
 * samples exactly like it does for any moving entity. Export pins {@code prev*} to the same pose:
 * every rendered frame then shows the exact sampled pose regardless of partial ticks.
 */
public final class DirectorCamera {

    /** Keyframes within this window replace each other instead of stacking (one per ~tick). */
    public static final int MERGE_WINDOW_MILLIS = 40;

    private static CameraTrack track = new CameraTrack();
    private static File replayFile;
    private static boolean previewEnabled = true;
    private static boolean dirty;
    private static CameraPose lastTickPose;
    private static float fovOverride = Float.NaN;

    private DirectorCamera() {
    }

    public static CameraTrack track() {
        return track;
    }

    public static boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public static void setPreviewEnabled(boolean enabled) {
        previewEnabled = enabled;
        if (!enabled) {
            clearOverride();
        }
    }

    /** The renderer's FOV while the director drives the camera; NaN = no override. */
    public static float fovOverride() {
        return fovOverride;
    }

    public static void markDirty() {
        dirty = true;
    }

    /** Persists edits immediately; edits are rare and losing camera work to a crash is bitter. */
    public static void saveIfDirty() {
        if (dirty && replayFile != null) {
            DirectorStore.save(replayFile, track);
            dirty = false;
        }
    }

    /** Called every client tick right after the replay player has advanced. */
    public static void onClientTick() {
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            if (replayFile != null) {
                saveIfDirty();
                replayFile = null;
                track = new CameraTrack();
                clearOverride();
            }
            return;
        }
        File file = player.file();
        if (file != null && !file.equals(replayFile)) {
            saveIfDirty();
            replayFile = file;
            track = DirectorStore.load(file);
            lastTickPose = null;
        }
        if (DirectorExporter.isRunning()) {
            return; // the exporter positions the camera itself, per frame
        }
        if (!previewEnabled || track.isEmpty() || player.isPossessing()) {
            clearOverride();
            return;
        }
        CameraPose pose = track.sample(player.elapsedMillis());
        if (pose != null) {
            applySmooth(pose);
        }
    }

    /** Per-tick application: prev = last tick's pose, so frames in between interpolate. */
    private static void applySmooth(CameraPose pose) {
        EntityPlayerSP camera = Minecraft.getMinecraft().thePlayer;
        if (camera == null) {
            return;
        }
        CameraPose from = lastTickPose == null ? pose : lastTickPose;
        camera.prevPosX = from.x;
        camera.prevPosY = from.y;
        camera.prevPosZ = from.z;
        camera.lastTickPosX = from.x;
        camera.lastTickPosY = from.y;
        camera.lastTickPosZ = from.z;
        camera.prevRotationYaw = from.yaw;
        camera.prevRotationPitch = from.pitch;
        camera.setPosition(pose.x, pose.y, pose.z);
        camera.rotationYaw = pose.yaw;
        camera.rotationPitch = pose.pitch;
        camera.motionX = 0;
        camera.motionY = 0;
        camera.motionZ = 0;
        lastTickPose = pose;
        fovOverride = pose.fov > 0f ? pose.fov : Float.NaN;
    }

    /** Exact application for export: no interpolation window, the frame shows this very pose. */
    public static void applyExact(CameraPose pose) {
        EntityPlayerSP camera = Minecraft.getMinecraft().thePlayer;
        if (camera == null || pose == null) {
            return;
        }
        camera.setPosition(pose.x, pose.y, pose.z);
        camera.prevPosX = pose.x;
        camera.prevPosY = pose.y;
        camera.prevPosZ = pose.z;
        camera.lastTickPosX = pose.x;
        camera.lastTickPosY = pose.y;
        camera.lastTickPosZ = pose.z;
        camera.rotationYaw = pose.yaw;
        camera.prevRotationYaw = pose.yaw;
        camera.rotationPitch = pose.pitch;
        camera.prevRotationPitch = pose.pitch;
        camera.motionX = 0;
        camera.motionY = 0;
        camera.motionZ = 0;
        lastTickPose = pose;
        fovOverride = pose.fov > 0f ? pose.fov : Float.NaN;
    }

    public static void clearOverride() {
        fovOverride = Float.NaN;
        lastTickPose = null;
    }

    /** The free camera's current pose — what "add keyframe" records. */
    public static CameraPose capturePose() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP camera = mc.thePlayer;
        if (camera == null) {
            return null;
        }
        float fov = Float.isNaN(fovOverride) ? mc.gameSettings.fovSetting : fovOverride;
        return new CameraPose(camera.posX, camera.posY, camera.posZ,
                camera.rotationYaw, camera.rotationPitch, fov);
    }
}
