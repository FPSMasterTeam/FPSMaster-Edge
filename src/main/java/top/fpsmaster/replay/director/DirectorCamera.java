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
    private static EditProject project;
    private static File projectFile;
    private static int previewClipIndex;
    private static boolean previewEnabled = false;
    private static boolean dirty;
    private static CameraPose lastTickPose;
    private static float fovOverride = Float.NaN;
    private static float appliedRoll;
    private static float prevAppliedRoll;
    private static float flyRoll;
    private static float flyFov = 70f;
    private static boolean rollKeyWasDown;
    private static boolean resetRollWasDown;
    private static final EditHistory history = new EditHistory();

    private DirectorCamera() {
    }

    public static CameraTrack track() {
        return track;
    }

    public static EditProject project() {
        return project;
    }

    public static File projectFile() {
        return projectFile;
    }

    public static int previewClipIndex() {
        return previewClipIndex;
    }

    public static void openProject(EditProject opened, File file) {
        saveIfDirty();
        project = opened;
        projectFile = file;
        if (opened.camera == null) {
            opened.camera = new CameraTrack();
        }
        opened.camera.migratePackedKeyframes();
        track = opened.camera;
        previewClipIndex = 0;
        dirty = false;
        lastTickPose = null;
        history.clear();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            flyFov = mc.gameSettings.fovSetting;
        }
    }

    /** New project for the replay now playing (or about to), importing any director sidecar. */
    public static void beginForReplay(File replay, int sourceDuration) {
        if (replay == null) {
            return;
        }
        String source = basename(replay);
        CameraTrack sidecar = DirectorStore.load(replay);
        EditProject created = EditProject.fromTrack(source, source, sidecar, sourceDuration);
        openProject(created, EditStore.uniqueFile(created.name));
        markDirty();
    }

    public static void seekToOutput(long outputMillis) {
        ReplayPlayer player = ReplayPlayer.instance();
        if (project == null || project.clips.isEmpty()) {
            player.seek((int) Math.max(0L, outputMillis));
            return;
        }
        previewClipIndex = project.clipIndexAtOutput(outputMillis);
        player.seek(project.mapOutputToSource(outputMillis));
    }

    public static long currentOutputTime() {
        ReplayPlayer player = ReplayPlayer.instance();
        if (project == null || project.clips.isEmpty()) {
            return player.elapsedMillis();
        }
        if (previewClipIndex < 0 || previewClipIndex >= project.clips.size()) {
            previewClipIndex = 0;
        }
        return project.outputTimeFor(previewClipIndex, player.elapsedMillis());
    }

    public static long editOutputDuration(int replayDuration) {
        if (project != null) {
            project.ensureDuration(replayDuration);
            if (!project.clips.isEmpty()) {
                return project.outputDurationMillis();
            }
        }
        if (!track.segments.isEmpty()) {
            return track.hasKeptContent(replayDuration) ? track.outputDurationMillis(replayDuration) : 0L;
        }
        return Math.max(0L, track.endMillis() - track.startMillis());
    }

    public static int mapOutputToSource(long outputMillis, int replayDuration) {
        if (project != null && !project.clips.isEmpty()) {
            return project.mapOutputToSource(outputMillis);
        }
        if (!track.segments.isEmpty()) {
            return track.mapOutputToSource(outputMillis, replayDuration);
        }
        return track.isEmpty() ? (int) outputMillis : track.startMillis() + (int) outputMillis;
    }

    private static String basename(File replay) {
        String name = replay.getName();
        if (name.endsWith(".edgereplay")) {
            return name.substring(0, name.length() - ".edgereplay".length());
        }
        return name;
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

    /** Dutch angle in degrees, interpolated like entity yaw. 0 = upright. */
    public static float roll(float partialTicks) {
        if (!ReplayPlayer.instance().isActive() || ReplayPlayer.instance().isPossessing()) {
            return 0f;
        }
        return prevAppliedRoll + (appliedRoll - prevAppliedRoll) * partialTicks;
    }

    public static float roll() {
        return appliedRoll;
    }

    public static void nudgeRoll(float degrees) {
        flyRoll = wrapRoll(flyRoll + degrees);
    }

    public static void resetRoll() {
        flyRoll = 0f;
    }

    /**
     * Q / E / R are sampled once per client tick. Updating roll every render frame and then
     * interpolating it with tick {@code partialTicks} fights vanilla camera setup (view bobbing
     * especially) and reads as the shot flickering back and forth.
     */
    public static boolean pollRollKeys() {
        if (!ReplayPlayer.instance().isActive() || ReplayPlayer.instance().isPossessing()) {
            rollKeyWasDown = false;
            return false;
        }
        boolean q = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_Q);
        boolean e = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_E);
        if (q || e) {
            float step = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                    || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT) ? 1.0f : 5.0f;
            flyRoll = wrapRoll(flyRoll + (e ? step : 0f) - (q ? step : 0f));
            rollKeyWasDown = true;
        } else {
            rollKeyWasDown = false;
        }
        boolean r = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_R);
        if (r && !resetRollWasDown) {
            flyRoll = 0f;
        }
        resetRollWasDown = r;
        return rollKeyWasDown || r;
    }

    static float wrapRoll(float roll) {
        float wrapped = roll % 360f;
        if (wrapped > 180f) {
            wrapped -= 360f;
        }
        if (wrapped < -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }

    /**
     * True while the track (or the exporter) owns the camera — mouse look must not fight it.
     * The per-tick snap would win anyway, but the tug-of-war reads as jitter.
     */
    public static boolean isDrivingCamera() {
        if (DirectorExporter.isRunning()) {
            return true;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        return previewEnabled && player.isActive() && track.drivesLook() && !player.isPossessing();
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void noteBeforeChange() {
        if (project != null) {
            project.camera = track;
            history.checkpoint(project);
        }
    }

    public static boolean undo() {
        if (project == null) {
            return false;
        }
        project.camera = track;
        EditProject restored = history.undo(project);
        return applyRestored(restored);
    }

    public static boolean redo() {
        if (project == null) {
            return false;
        }
        project.camera = track;
        EditProject restored = history.redo(project);
        return applyRestored(restored);
    }

    public static boolean canUndo() {
        return history.canUndo();
    }

    public static boolean canRedo() {
        return history.canRedo();
    }

    private static boolean applyRestored(EditProject restored) {
        if (restored == null) {
            return false;
        }
        EditStore.normalize(restored);
        project = restored;
        track = restored.camera;
        lastTickPose = null;
        dirty = true;
        saveIfDirty();
        return true;
    }

    public static void nudgeFov(float degrees) {
        flyFov = clampFov(flyFov + degrees);
        fovOverride = flyFov;
    }

    public static float flyFov() {
        return flyFov;
    }

    public static void setFlyFov(float fov) {
        flyFov = clampFov(fov);
        fovOverride = flyFov;
    }

    static float clampFov(float fov) {
        return fov < 30f ? 30f : (fov > 110f ? 110f : fov);
    }

    /** Persists edits immediately; edits are rare and losing camera work to a crash is bitter. */
    public static void saveIfDirty() {
        if (!dirty) {
            return;
        }
        if (project != null && projectFile != null) {
            project.camera = track;
            if (EditStore.save(projectFile, project)) {
                dirty = false;
            }
            return;
        }
        if (replayFile != null) {
            DirectorStore.save(replayFile, track);
            dirty = false;
        }
    }

    /** Called every client tick right after the replay player has advanced. */
    public static void onClientTick() {
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            if (replayFile != null || project != null) {
                saveIfDirty();
                replayFile = null;
                project = null;
                projectFile = null;
                previewClipIndex = 0;
                track = new CameraTrack();
                restorePreviewSpeed(player);
                clearOverride();
            }
            return;
        }
        File file = player.file();
        if (file != null && !file.equals(replayFile)) {
            if (project == null) {
                saveIfDirty();
                track = DirectorStore.load(file);
            }
            replayFile = file;
            lastTickPose = null;
        }
        if (DirectorExporter.isRunning()) {
            return; // the exporter positions the camera itself, per frame
        }
        if (!previewEnabled) {
            restorePreviewSpeed(player);
            clearOverride();
            return;
        }

        int duration = Math.max(player.durationMillis(), player.elapsedMillis());
        if (project != null) {
            project.ensureDuration(duration);
            applyProjectPreview(player);
        } else if (!track.segments.isEmpty() && !player.isSeeking()) {
            // Legacy sidecar cut list, source-ordered.
            TimelineSegment segment = track.segmentAt(player.elapsedMillis(), duration);
            if (segment != null && segment.excluded) {
                int next = track.nextKeptMillis(player.elapsedMillis(), duration);
                if (next > player.elapsedMillis()) {
                    player.seek(next);
                } else if (next < 0 && !player.isPaused()) {
                    player.togglePause();
                }
            } else if (segment != null) {
                float want = segment.speed <= 0f ? 1f : segment.speed;
                if (previewSpeed != want) {
                    player.setSpeed(want);
                    previewSpeed = want;
                }
            }
        }

        boolean rolling = false;
        if (!player.isPossessing()) {
            rolling = pollRollKeys();
            pollFovWheel();
        }
        if (player.isPossessing()) {
            // Possession is a preview of the recorder's eyes, not a camera take.
            clearOverride();
            commitRoll();
            return;
        }
        if (track.isEmpty()) {
            commitRoll();
            if (Float.isNaN(fovOverride)) {
                fovOverride = flyFov;
            }
            return;
        }
        CameraPose hold = capturePose();
        CameraPose pose = track.sample(player.elapsedMillis(), hold);
        if (pose != null) {
            applySmooth(pose, rolling);
        }
        commitRoll();
    }

    /**
     * Pins the camera to the track on every rendered frame. Tick-only application is overwritten
     * by vanilla entity updates ({@code prev = pos}) and reads as 20 fps stutter.
     */
    public static void onRenderFrame(float partialTicks) {
        if (DirectorExporter.isRunning()) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        if (!previewEnabled || !player.isActive() || player.isPossessing()) {
            return;
        }
        if (track.isEmpty()) {
            if (Float.isNaN(fovOverride)) {
                fovOverride = flyFov;
            }
            return;
        }
        CameraPose hold = capturePose();
        CameraPose pose = track.sample(player.visualElapsedMillis(), hold);
        if (pose == null) {
            return;
        }
        if (track.drivesPosition() || track.drivesLook()) {
            applyExact(pose);
        } else if (pose.fov > 0f) {
            flyFov = clampFov(pose.fov);
            fovOverride = flyFov;
        }
    }

    private static void pollFovWheel() {
        if (Minecraft.getMinecraft().currentScreen != null) {
            return;
        }
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel != 0) {
            nudgeFov(wheel > 0 ? 2f : -2f);
        }
    }

    private static void commitRoll() {
        prevAppliedRoll = appliedRoll;
        appliedRoll = flyRoll;
    }

    /** Speed the preview last forced, NaN when it has not touched the player's speed. */
    private static float previewSpeed = Float.NaN;

    private static void applyProjectPreview(ReplayPlayer player) {
        if (player.isSeeking() || project.clips.isEmpty()) {
            return;
        }
        if (previewClipIndex < 0 || previewClipIndex >= project.clips.size()) {
            previewClipIndex = 0;
        }
        EditClip clip = project.clips.get(previewClipIndex);
        int elapsed = player.elapsedMillis();
        if (elapsed >= clip.srcIn && elapsed < clip.srcOut) {
            applyClipSpeed(player, clip, elapsed);
            return;
        }
        if (elapsed >= clip.srcOut) {
            int next = previewClipIndex + 1;
            if (next >= project.clips.size()) {
                if (!player.isPaused()) {
                    player.togglePause();
                }
                return;
            }
            EditClip nextClip = project.clips.get(next);
            previewClipIndex = next;
            // A razor cut is two adjacent source ranges. The clock overshoots srcOut by up to a
            // tick, so seek(next.srcIn) would be a few milliseconds backward — and any backward
            // seek rebuilds the world from the start of the packet stream.
            if (inSourceRange(nextClip, elapsed)) {
                applyClipSpeed(player, nextClip, elapsed);
                return;
            }
            player.seek(nextClip.srcIn);
            return;
        }
        player.seek(clip.srcIn);
    }

    static boolean inSourceRange(EditClip clip, int elapsed) {
        return clip != null && elapsed >= clip.srcIn && elapsed < clip.srcOut;
    }

    private static void applyClipSpeed(ReplayPlayer player, EditClip clip, int elapsed) {
        float want = clip.speedAtSource(elapsed);
        if (Math.abs(previewSpeed - want) > 0.02f) {
            player.setSpeed(want);
            previewSpeed = want;
        }
    }

    private static void restorePreviewSpeed(ReplayPlayer player) {
        if (!Float.isNaN(previewSpeed)) {
            player.setSpeed(1f);
            previewSpeed = Float.NaN;
        }
    }

    /** Per-tick application: prev = last tick's pose, so frames in between interpolate. */
    private static void applySmooth(CameraPose pose, boolean rolling) {
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
        if (pose.fov > 0f) {
            flyFov = clampFov(pose.fov);
            fovOverride = flyFov;
        }
        if (!rolling) {
            flyRoll = pose.roll;
        }
    }

    /** Exact application for export: no interpolation window, the frame shows this very pose. */
    public static void applyExact(CameraPose pose) {
        EntityPlayerSP camera = Minecraft.getMinecraft().thePlayer;
        if (camera == null || pose == null) {
            return;
        }
        if (Double.isNaN(pose.x) || Double.isNaN(pose.y) || Double.isNaN(pose.z)
                || Float.isNaN(pose.yaw) || Float.isNaN(pose.pitch)) {
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
        if (pose.fov > 0f) {
            flyFov = clampFov(pose.fov);
            fovOverride = flyFov;
        }
        prevAppliedRoll = pose.roll;
        appliedRoll = pose.roll;
        flyRoll = pose.roll;
    }

    public static void clearOverride() {
        fovOverride = Float.NaN;
        lastTickPose = null;
        if (!ReplayPlayer.instance().isActive()) {
            appliedRoll = 0f;
            prevAppliedRoll = 0f;
            flyRoll = 0f;
        }
    }

    /** The free camera's current pose — what "add keyframe" records. */
    public static CameraPose capturePose() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP camera = mc.thePlayer;
        if (camera == null) {
            return null;
        }
        float fov = flyFov > 0f ? flyFov
                : (Float.isNaN(fovOverride) ? mc.gameSettings.fovSetting : fovOverride);
        return new CameraPose(camera.posX, camera.posY, camera.posZ,
                camera.rotationYaw, camera.rotationPitch, clampFov(fov), flyRoll);
    }
}
