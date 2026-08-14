package top.fpsmaster.replay.director;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Sidecar persistence for a replay's director timeline: {@code <name>.edgereplay} gets a
 * {@code <name>.director.json} next to it. A sidecar rather than a new record type inside the
 * replay stream, so editing camera work never rewrites (or risks corrupting) the recording, and
 * older clients simply ignore the extra file.
 */
public final class DirectorStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String REPLAY_SUFFIX = ".edgereplay";
    private static final String SIDECAR_SUFFIX = ".director.json";

    private DirectorStore() {
    }

    public static File sidecarFor(File replayFile) {
        String name = replayFile.getName();
        if (name.endsWith(REPLAY_SUFFIX)) {
            name = name.substring(0, name.length() - REPLAY_SUFFIX.length());
        }
        return new File(replayFile.getParentFile(), name + SIDECAR_SUFFIX);
    }

    public static CameraTrack load(File replayFile) {
        File sidecar = sidecarFor(replayFile);
        if (!sidecar.isFile()) {
            return new CameraTrack();
        }
        try {
            String json = new String(Files.readAllBytes(sidecar.toPath()), StandardCharsets.UTF_8);
            CameraTrack track = GSON.fromJson(json, CameraTrack.class);
            if (track == null) {
                return new CameraTrack();
            }
            // Defensive: nulls from hand-edited files would NPE deep inside interpolation.
            track.keyframes.removeIf(frame -> frame == null);
            for (CameraKeyframe frame : track.keyframes) {
                if (frame.transition == null) {
                    frame.transition = CameraKeyframe.Transition.SMOOTH;
                }
                if (frame.easing == null) {
                    frame.easing = CameraKeyframe.Easing.EASE_IN_OUT;
                }
            }
            track.sort();
            return track;
        } catch (IOException | RuntimeException exception) {
            ClientLogger.warn("director: could not read " + sidecar.getName() + ": " + exception);
            return new CameraTrack();
        }
    }

    public static boolean save(File replayFile, CameraTrack track) {
        File sidecar = sidecarFor(replayFile);
        try {
            if (track == null || track.isEmpty()) {
                if (sidecar.isFile()) {
                    Files.delete(sidecar.toPath());
                }
                return true;
            }
            Files.write(sidecar.toPath(), GSON.toJson(track).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException exception) {
            ClientLogger.warn("director: could not save " + sidecar.getName() + ": " + exception);
            return false;
        }
    }
}
