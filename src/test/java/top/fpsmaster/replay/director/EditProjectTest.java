package top.fpsmaster.replay.director;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditProjectTest {

    @Test
    void emptyClipsFallBackToIdentity() {
        EditProject project = new EditProject();
        assertEquals(0L, project.outputDurationMillis());
        assertEquals(1500, project.mapOutputToSource(1500));
    }

    @Test
    void speedStretchesOutput() {
        EditProject project = EditProject.create("t", "src", 4000);
        project.setSpeed(0, 0.5f);
        assertEquals(8000L, project.outputDurationMillis());
        assertEquals(0, project.mapOutputToSource(0));
        assertEquals(2000, project.mapOutputToSource(4000));
        assertEquals(4000, project.mapOutputToSource(8000));
    }

    @Test
    void splitThenReorderMapsBackToSource() {
        EditProject project = EditProject.create("t", "src", 10_000);
        project.splitAtOutput(4000);
        assertEquals(2, project.clips.size());
        assertEquals(0, project.clips.get(0).srcIn);
        assertEquals(4000, project.clips.get(0).srcOut);
        assertEquals(4000, project.clips.get(1).srcIn);

        project.moveClip(1, 0);
        assertEquals(4000, project.clips.get(0).srcIn);
        assertEquals(0, project.clips.get(1).srcIn);
        assertEquals(4000, project.mapOutputToSource(0));
        assertEquals(0, project.mapOutputToSource(6000));
    }

    @Test
    void fromTrackDropsExcludedSegments() {
        CameraTrack track = new CameraTrack();
        track.trimStart(1000, 5000);
        track.trimEnd(4000, 5000);
        EditProject project = EditProject.fromTrack("cut", "src", track, 5000);
        assertEquals(1, project.clips.size());
        assertEquals(1000, project.clips.get(0).srcIn);
        assertEquals(4000, project.clips.get(0).srcOut);
        assertFalse(project.pristine);
    }

    @Test
    void ensureDurationGrowsPristineClipOnly() {
        EditProject project = EditProject.create("t", "src", 1);
        project.ensureDuration(8000);
        assertEquals(8000, project.clips.get(0).srcOut);
        project.splitAtOutput(2000);
        project.ensureDuration(20_000);
        assertTrue(project.clips.get(0).srcOut <= 8000);
    }

    @Test
    void uniformCurveMatchesConstantSpeed() {
        EditClip clip = new EditClip(0, 4000);
        clip.speed = 2f;
        long flat = clip.outputLength();
        clip.enableCurve();
        clip.curve.get(0).s = 2f;
        clip.curve.get(1).s = 2f;
        assertEquals((double) flat, (double) clip.outputLength(), 80.0);
        assertEquals(2000.0, clip.sourceOffsetForOutput(1000), 80.0);
    }

    @Test
    void slowCurveStretchesOutput() {
        EditClip clip = new EditClip(0, 4000);
        clip.enableCurve();
        clip.curve.get(0).s = 0.5f;
        clip.curve.get(1).s = 0.5f;
        assertEquals(8000.0, clip.outputLength(), 120.0);
    }

    @Test
    void duplicateClipRepeatsSourceRange() {
        EditProject project = EditProject.create("t", "src", 4000);
        int copy = project.duplicateClip(0);
        assertEquals(1, copy);
        assertEquals(2, project.clips.size());
        assertEquals(project.clips.get(0).srcIn, project.clips.get(1).srcIn);
        assertEquals(project.clips.get(0).srcOut, project.clips.get(1).srcOut);
        assertEquals(8000L, project.outputDurationMillis());
        assertEquals(0, project.mapOutputToSource(0));
        assertEquals(0, project.mapOutputToSource(4000));
    }

    @Test
    void undoRestoresClipCount() {
        EditProject project = EditProject.create("t", "src", 8000);
        EditHistory history = new EditHistory();
        history.checkpoint(project);
        project.splitAtOutput(4000);
        assertEquals(2, project.clips.size());
        EditProject restored = history.undo(project);
        EditStore.normalize(restored);
        assertEquals(1, restored.clips.size());
    }

    @Test
    void lookBasisIsOrthonormal() {
        float[] forward = new float[3];
        float[] right = new float[3];
        float[] up = new float[3];
        DirectorWorldOverlay.lookBasis(90f, 0f, 0f, forward, right, up);
        float fl = forward[0] * forward[0] + forward[1] * forward[1] + forward[2] * forward[2];
        float rl = right[0] * right[0] + right[1] * right[1] + right[2] * right[2];
        assertEquals(1.0, fl, 0.02);
        assertEquals(1.0, rl, 0.02);
        assertEquals(0.0, forward[0] * right[0] + forward[1] * right[1] + forward[2] * right[2], 0.02);
    }

    @Test
    void splitKeepsMonotonicSource() {
        EditProject project = EditProject.create("t", "src", 10_000);
        project.splitAtOutput(4000);
        assertEquals(2, project.clips.size());
        int prev = -1;
        for (long out = 0; out <= project.outputDurationMillis(); out += 50) {
            int src = project.mapOutputToSource(out);
            assertTrue(src >= prev, "source went backward at output " + out + ": " + prev + " -> " + src);
            prev = src;
        }
    }

    @Test
    void razorOvershootDoesNotNeedSeek() {
        EditProject project = EditProject.create("t", "src", 10_000);
        project.splitAtOutput(4000);
        EditClip left = project.clips.get(0);
        EditClip right = project.clips.get(1);
        assertEquals(left.srcOut, right.srcIn);
        assertTrue(DirectorCamera.inSourceRange(right, right.srcIn));
        assertTrue(DirectorCamera.inSourceRange(right, right.srcIn + 40));
        assertFalse(DirectorCamera.inSourceRange(left, right.srcIn + 40));
    }

    @Test
    void splitPreservesCurveHalves() {
        EditProject project = EditProject.create("t", "src", 8000);
        project.toggleCurve(0);
        project.splitAtOutput(project.outputDurationMillis() / 2);
        assertEquals(2, project.clips.size());
        assertTrue(project.clips.get(0).hasCurve());
        assertTrue(project.clips.get(1).hasCurve());
    }
}
