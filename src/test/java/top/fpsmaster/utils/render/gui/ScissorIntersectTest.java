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

    @Test
    void framebufferScissorUsesScaleAndFlipsY() {
        // 1920x1080, GUI scale 2: logical (100, 50, 200, 100) → fb (200, 780, 400, 200)
        int[] box = Scissor.toFramebuffer(100f, 50f, 200f, 100f, 2f, 1920, 1080);
        assertArrayEquals(new int[] {200, 780, 400, 200}, box);
    }

    @Test
    void framebufferScissorClampsExpandedSettingsToTheWindow() {
        // Performance expanded: settings extra is taller than the display.
        int[] box = Scissor.toFramebuffer(146f, 70f, 318f, 950f, 2f, 1920, 1080);
        assertEquals(292, box[0]);
        assertEquals(0, box[1]);
        assertEquals(636, box[2]);
        assertEquals(940, box[3]);
    }

    @Test
    void framebufferScissorRejectsEmptyOrInvalidInput() {
        assertArrayEquals(new int[] {0, 0, 0, 0},
                Scissor.toFramebuffer(0f, 0f, 10f, 10f, 0f, 1920, 1080));
        assertArrayEquals(new int[] {0, 0, 0, 0},
                Scissor.toFramebuffer(0f, 0f, -4f, 10f, 2f, 1920, 1080));
    }
}
