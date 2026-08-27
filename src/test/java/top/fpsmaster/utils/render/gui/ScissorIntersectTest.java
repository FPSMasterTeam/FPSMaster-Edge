package top.fpsmaster.utils.render.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScissorIntersectTest {

    @Test
    void nestedSettingsClipStaysInsideListViewport() {
        // SharedClickGui clips the module list (panel body), then each expanded module
        // clips its settings to the card — Performance's card is taller than the panel.
        float[] list = {140f, 40f, 330f, 270f};
        float[] settings = {146f, 70f, 318f, 950f};
        float[] clipped = Scissor.intersect(
                list[0], list[1], list[2], list[3],
                settings[0], settings[1], settings[2], settings[3]);
        assertEquals(146f, clipped[0], 0.01f);
        assertEquals(70f, clipped[1], 0.01f);
        assertEquals(318f, clipped[2], 0.01f);
        assertEquals(240f, clipped[3], 0.01f);
    }

    @Test
    void disjointRectsAreEmpty() {
        float[] clipped = Scissor.intersect(0f, 0f, 10f, 10f, 20f, 20f, 5f, 5f);
        assertEquals(0f, clipped[2], 0.01f);
        assertEquals(0f, clipped[3], 0.01f);
    }

    @Test
    void identicalRectsStayIdentical() {
        assertArrayEquals(new float[] {8f, 12f, 40f, 16f},
                Scissor.intersect(8f, 12f, 40f, 16f, 8f, 12f, 40f, 16f), 0.01f);
    }

    @Test
    void negativeExtentsDoNotExpandTheOtherRect() {
        float[] clipped = Scissor.intersect(0f, 0f, 20f, 20f, 4f, 4f, -8f, 10f);
        assertEquals(0f, clipped[2], 0.01f);
    }
}
