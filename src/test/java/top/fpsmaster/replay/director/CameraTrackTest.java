package top.fpsmaster.replay.director;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraTrackTest {

    @Test
    void migratesPackedKeyframesIntoChannels() {
        CameraTrack track = new CameraTrack();
        CameraPose a = new CameraPose(1, 2, 3, 10f, 20f, 70f, 5f);
        CameraPose b = new CameraPose(4, 5, 6, 40f, 0f, 90f, 0f);
        CameraKeyframe ka = new CameraKeyframe(0, a);
        CameraKeyframe kb = new CameraKeyframe(1000, b);
        track.keyframes.add(ka);
        track.keyframes.add(kb);
        track.migratePackedKeyframes();
        assertEquals(2, track.position.size());
        assertEquals(2, track.fov.size());
        CameraPose hold = new CameraPose(0, 0, 0, 0f, 0f, 70f, 0f);
        CameraPose mid = track.sample(500, hold);
        assertEquals(2.5, mid.x, 0.15);
        assertEquals(80.0, mid.fov, 1.5);
    }

    @Test
    void unkeyedChannelHoldsLivePose() {
        CameraTrack track = new CameraTrack();
        track.addValues(CameraChannel.FOV, 0, new float[]{70f}, 0);
        track.addValues(CameraChannel.FOV, 1000, new float[]{90f}, 0);
        CameraPose hold = new CameraPose(8, 9, 10, 45f, -10f, 70f, 3f);
        CameraPose sampled = track.sample(500, hold);
        assertEquals(8, sampled.x, 0.01);
        assertEquals(45f, sampled.yaw, 0.01);
        assertEquals(80.0, sampled.fov, 1.5);
        assertFalse(track.drivesLook());
        assertFalse(track.drivesPosition());
    }

    @Test
    void independentEasingOnFov() {
        CameraTrack track = new CameraTrack();
        PropKeyframe a = track.addValues(CameraChannel.FOV, 0, new float[]{30f}, 0);
        track.addValues(CameraChannel.FOV, 1000, new float[]{110f}, 0);
        a.easing = CameraKeyframe.Easing.LINEAR;
        CameraPose hold = new CameraPose(0, 0, 0, 0f, 0f, 70f, 0f);
        assertEquals(70.0, track.sample(500, hold).fov, 0.5);
    }

    @Test
    void positionSampleDoesNotJumpPerMillisecond() {
        CameraTrack track = new CameraTrack();
        track.addValues(CameraChannel.POSITION, 0, new float[]{0f, 0f, 0f}, 0);
        track.addValues(CameraChannel.POSITION, 1000, new float[]{10f, 0f, 0f}, 0);
        CameraPose hold = new CameraPose(0, 0, 0, 0f, 0f, 70f, 0f);
        double prev = track.sample(0, hold).x;
        for (int t = 1; t <= 1000; t++) {
            double x = track.sample(t, hold).x;
            assertTrue(Math.abs(x - prev) < 0.05, "jump at t=" + t + ": " + prev + " -> " + x);
            prev = x;
        }
    }
}
