package top.fpsmaster.replay.director;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved edit that <em>references</em> a replay rather than rewriting it.
 *
 * <p>{@link #source} is the recording's basename ({@code foo} for {@code foo.edgereplay}).
 * {@link #clips} is the output EDL. {@link #camera} holds keyframes in source time, same as the
 * old director sidecar, so a slow clip also slows the camera move through it.
 */
public final class EditProject {
    public static final int MIN_CLIP_SOURCE = 200;

    public String name = "";
    public String source = "";
    public long updated;
    public List<EditClip> clips = new ArrayList<EditClip>();
    public CameraTrack camera = new CameraTrack();

    /** True until the user first splits, trims or reorders — the single clip may grow with duration. */
    public boolean pristine = true;

    public EditProject() {
    }

    public static EditProject create(String name, String source, int sourceDuration) {
        EditProject project = new EditProject();
        project.name = name;
        project.source = source;
        project.updated = System.currentTimeMillis();
        project.pristine = true;
        project.clips.add(new EditClip(0, Math.max(1, sourceDuration)));
        return project;
    }

    /**
     * Turns an old per-replay director sidecar into a project: kept segments become clips, camera
     * keyframes come across as-is.
     */
    public static EditProject fromTrack(String name, String source, CameraTrack track, int sourceDuration) {
        EditProject project = create(name, source, sourceDuration);
        if (track == null) {
            return project;
        }
        track.migratePackedKeyframes();
        project.camera = track;
        if (track.segments.isEmpty()) {
            return project;
        }
        project.clips.clear();
        project.pristine = false;
        int index = 1;
        for (TimelineSegment segment : track.segments) {
            if (segment.excluded || segment.sourceLength() <= 0) {
                continue;
            }
            EditClip clip = new EditClip(segment.startMillis, segment.endMillis);
            clip.speed = segment.speed <= 0f ? 1f : segment.speed;
            clip.name = "Clip " + index;
            index++;
            project.clips.add(clip);
        }
        if (project.clips.isEmpty()) {
            project.clips.add(new EditClip(0, Math.max(1, sourceDuration)));
            project.pristine = true;
        }
        return project;
    }

    public void ensureDuration(int sourceDuration) {
        if (!pristine || clips.size() != 1 || sourceDuration <= 0) {
            return;
        }
        EditClip clip = clips.get(0);
        if (clip.srcIn == 0 && clip.speed == 1f && clip.srcOut < sourceDuration) {
            clip.srcOut = sourceDuration;
        }
    }

    public long outputDurationMillis() {
        long total = 0;
        for (EditClip clip : clips) {
            total += clip.outputLength();
        }
        return total;
    }

    public long outputStartOf(int index) {
        long acc = 0;
        for (int i = 0; i < index && i < clips.size(); i++) {
            acc += clips.get(i).outputLength();
        }
        return acc;
    }

    public int clipIndexAtOutput(long outputMillis) {
        if (clips.isEmpty()) {
            return 0;
        }
        long acc = 0;
        for (int i = 0; i < clips.size(); i++) {
            long length = clips.get(i).outputLength();
            if (outputMillis < acc + length || i == clips.size() - 1) {
                return i;
            }
            acc += length;
        }
        return clips.size() - 1;
    }

    public int mapOutputToSource(long outputMillis) {
        if (clips.isEmpty()) {
            return (int) Math.max(0L, outputMillis);
        }
        long acc = 0;
        for (EditClip clip : clips) {
            long length = clip.outputLength();
            if (length <= 0) {
                continue;
            }
            if (outputMillis < acc + length) {
                return clip.srcIn + clip.sourceOffsetForOutput(outputMillis - acc);
            }
            acc += length;
        }
        return clips.get(clips.size() - 1).srcOut;
    }

    public long outputTimeFor(int clipIndex, int sourceMillis) {
        long acc = outputStartOf(clipIndex);
        if (clipIndex < 0 || clipIndex >= clips.size()) {
            return acc;
        }
        EditClip clip = clips.get(clipIndex);
        int local = sourceMillis - clip.srcIn;
        if (local < 0) {
            local = 0;
        }
        if (local > clip.sourceLength()) {
            local = clip.sourceLength();
        }
        return acc + clip.outputOffsetForSource(local);
    }

    public void splitAtOutput(long outputMillis) {
        if (clips.isEmpty()) {
            return;
        }
        int index = clipIndexAtOutput(outputMillis);
        EditClip clip = clips.get(index);
        long start = outputStartOf(index);
        long local = outputMillis - start;
        if (local < 80L || local > clip.outputLength() - 80L) {
            return;
        }
        int srcCut = clip.srcIn + clip.sourceOffsetForOutput(local);
        if (srcCut <= clip.srcIn + MIN_CLIP_SOURCE || srcCut >= clip.srcOut - MIN_CLIP_SOURCE) {
            return;
        }
        float pCut = clip.sourceLength() <= 0 ? 0.5f
                : (srcCut - clip.srcIn) / (float) clip.sourceLength();
        EditClip tail = clip.copy();
        tail.srcIn = srcCut;
        clip.srcOut = srcCut;
        clip.splitCurveAt(pCut, tail);
        clips.add(index + 1, tail);
        pristine = false;
    }

    public void removeClip(int index) {
        if (index < 0 || index >= clips.size() || clips.size() <= 1) {
            return;
        }
        clips.remove(index);
        pristine = false;
    }

    public void moveClip(int from, int to) {
        if (from < 0 || from >= clips.size() || to < 0 || to >= clips.size() || from == to) {
            return;
        }
        EditClip clip = clips.remove(from);
        clips.add(to, clip);
        pristine = false;
    }

    public void setSpeed(int index, float speed) {
        if (index < 0 || index >= clips.size()) {
            return;
        }
        EditClip clip = clips.get(index);
        clip.speed = speed <= 0f ? 1f : speed;
        clip.clearCurve();
        pristine = false;
    }

    public void toggleCurve(int index) {
        if (index < 0 || index >= clips.size()) {
            return;
        }
        EditClip clip = clips.get(index);
        if (clip.hasCurve()) {
            clip.speed = clip.speedAt(0.5f);
            clip.clearCurve();
        } else {
            clip.enableCurve();
        }
        pristine = false;
    }

    /**
     * Inserts a copy of clip {@code index} immediately after it. Same source range; playback of
     * the copy seeks back in the packet stream (a rebuild if going backwards).
     */
    public int duplicateClip(int index) {
        if (index < 0 || index >= clips.size()) {
            return -1;
        }
        EditClip copy = clips.get(index).copy();
        if (copy.name == null || copy.name.isEmpty()) {
            copy.name = "";
        }
        clips.add(index + 1, copy);
        pristine = false;
        return index + 1;
    }

    public void trimSource(int index, int newIn, int newOut) {
        if (index < 0 || index >= clips.size()) {
            return;
        }
        EditClip clip = clips.get(index);
        int in = Math.max(0, newIn);
        int out = Math.max(in + MIN_CLIP_SOURCE, newOut);
        clip.srcIn = in;
        clip.srcOut = out;
        pristine = false;
    }
}
