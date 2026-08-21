package top.fpsmaster.replay.director;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectorWorldOverlayTest {

    @Test
    void yawZeroLooksSouth() {
        float[] forward = new float[3];
        float[] right = new float[3];
        float[] up = new float[3];
        DirectorWorldOverlay.lookBasis(0f, 0f, 0f, forward, right, up);
        assertEquals(0.0, forward[0], 0.02);
        assertEquals(0.0, forward[1], 0.02);
        assertEquals(1.0, forward[2], 0.02);
    }
}
