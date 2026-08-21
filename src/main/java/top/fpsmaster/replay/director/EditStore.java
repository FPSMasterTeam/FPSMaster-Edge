package top.fpsmaster.replay.director;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Persistence for edit projects: {@code edits/<name>.edgeedit}. A project never rewrites the
 * {@code .edgereplay} it cites — losing an edit is recoverable, losing the recording is not.
 */
public final class EditStore {

    public static final String SUFFIX = ".edgeedit";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private EditStore() {
    }

    public static File directory() {
        return new File(FileUtils.dir, "edits");
    }

    public static File fileForName(String name) {
        String safe = name.trim();
        if (safe.isEmpty()) {
            safe = "untitled";
        }
        safe = safe.replace('\\', '_').replace('/', '_');
        return new File(directory(), safe + SUFFIX);
    }

    public static File uniqueFile(String name) {
        File candidate = fileForName(name);
        if (!candidate.exists()) {
            return candidate;
        }
        int n = 2;
        while (true) {
            File next = fileForName(name + " " + n);
            if (!next.exists()) {
                return next;
            }
            n++;
        }
    }

    public static List<File> listFiles() {
        File dir = directory();
        File[] files = dir.listFiles();
        List<File> result = new ArrayList<File>();
        if (files == null) {
            return result;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(SUFFIX)) {
                result.add(file);
            }
        }
        return result;
    }

    public static EditProject load(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            EditProject project = GSON.fromJson(json, EditProject.class);
            if (project == null) {
                return null;
            }
            if (project.name == null || project.name.isEmpty()) {
                String n = file.getName();
                project.name = n.endsWith(SUFFIX) ? n.substring(0, n.length() - SUFFIX.length()) : n;
            }
            if (project.source == null) {
                project.source = "";
            }
            if (project.clips == null) {
                project.clips = new ArrayList<EditClip>();
            }
            project.clips.removeIf(clip -> clip == null);
            for (EditClip clip : project.clips) {
                if (clip.curve != null) {
                    clip.curve.removeIf(point -> point == null);
                    if (clip.curve.size() < 2) {
                        clip.curve = null;
                    } else {
                        clip.sortCurve();
                    }
                }
            }
            normalize(project);
            return project;
        } catch (IOException | RuntimeException exception) {
            ClientLogger.warn("edit: could not read " + file.getName() + ": " + exception);
            return null;
        }
    }

    public static void normalize(EditProject project) {
        if (project == null) {
            return;
        }
        if (project.name == null) {
            project.name = "";
        }
        if (project.source == null) {
            project.source = "";
        }
        if (project.clips == null) {
            project.clips = new ArrayList<EditClip>();
        }
        project.clips.removeIf(clip -> clip == null);
        for (EditClip clip : project.clips) {
            if (clip.curve != null) {
                clip.curve.removeIf(point -> point == null);
                if (clip.curve.size() < 2) {
                    clip.curve = null;
                } else {
                    clip.sortCurve();
                }
            }
        }
        if (project.camera == null) {
            project.camera = new CameraTrack();
        }
        project.camera.ensureLists();
        project.camera.keyframes.removeIf(frame -> frame == null);
        for (CameraKeyframe frame : project.camera.keyframes) {
            if (frame.transition == null) {
                frame.transition = CameraKeyframe.Transition.SMOOTH;
            }
            if (frame.easing == null) {
                frame.easing = CameraKeyframe.Easing.EASE_IN_OUT;
            }
        }
        sanitizeChannel(project.camera.position);
        sanitizeChannel(project.camera.yaw);
        sanitizeChannel(project.camera.pitch);
        sanitizeChannel(project.camera.roll);
        sanitizeChannel(project.camera.fov);
        project.camera.migratePackedKeyframes();
        project.camera.sort();
    }

    private static void sanitizeChannel(java.util.List<PropKeyframe> keys) {
        if (keys == null) {
            return;
        }
        keys.removeIf(key -> key == null);
        for (PropKeyframe key : keys) {
            if (key.easing == null) {
                key.easing = CameraKeyframe.Easing.EASE_IN_OUT;
            }
            if (key.path == null) {
                key.path = CameraKeyframe.Transition.LINEAR;
            }
        }
    }

    public static boolean save(File file, EditProject project) {
        if (file == null || project == null) {
            return false;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            ClientLogger.warn("edit: could not create " + parent.getAbsolutePath());
            return false;
        }
        project.updated = System.currentTimeMillis();
        try {
            Files.write(file.toPath(), GSON.toJson(project).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException exception) {
            ClientLogger.warn("edit: could not save " + file.getName() + ": " + exception);
            return false;
        }
    }
}
