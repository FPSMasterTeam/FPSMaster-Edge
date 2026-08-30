package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class Scissor {
    /**
     * Intersect two axis-aligned rectangles. Width/height are {@code 0} when they
     * do not overlap. Used so nested {@code pushClip} calls compose instead of
     * replacing the current scissor (expanded ClickGUI modules must stay inside
     * the list viewport).
     */
    public static float[] intersect(
            float ax, float ay, float aw, float ah,
            float bx, float by, float bw, float bh
    ) {
        float aRight = ax + Math.max(0f, aw);
        float aBottom = ay + Math.max(0f, ah);
        float bRight = bx + Math.max(0f, bw);
        float bBottom = by + Math.max(0f, bh);
        float x = Math.max(ax, bx);
        float y = Math.max(ay, by);
        return new float[] {
                x,
                y,
                Math.max(0f, Math.min(aRight, bRight) - x),
                Math.max(0f, Math.min(aBottom, bBottom) - y)
        };
    }

    public static void apply(float x, float y, float width, float height) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        if (UiScale.isActive()) {
            applyScaled(x, y, width, height, UiScale.getLayoutScale(),
                    UiScale.getDisplayWidth(), UiScale.getDisplayHeight());
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        applyScaled(x, y, width, height, resolution.getScaleFactor(),
                mc.displayWidth, mc.displayHeight);
    }

    public static void apply(float x, float y, float width, float height, float scaleFactor) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        applyScaled(x, y, width, height, scaleFactor, mc.displayWidth, mc.displayHeight);
    }

    public static void apply(float x, float y, float width, float height, int scaleFactor) {
        apply(x, y, width, height, (float) scaleFactor);
    }

    /**
     * Convert logical GUI coordinates to an {@code glScissor} box.
     *
     * <p>OpenGL scissor is in framebuffer pixels with the origin at the bottom-left.
     * Minecraft GUI coordinates are top-down and (when a {@link ScaledResolution}
     * or {@link UiScale} is active) not 1:1 with pixels. Passing GUI units straight
     * to {@code glScissor} is the usual 1.8.9 overflow: the clip is too small, in the
     * wrong place, or invalid (negative) so the driver ignores it and settings draw
     * outside the window.
     *
     * @return {@code {fbX, fbY, fbW, fbH}} already clamped to the framebuffer
     */
    public static int[] toFramebuffer(
            float x, float y, float width, float height,
            float scale, int displayWidth, int displayHeight
    ) {
        if (scale <= 0f || displayWidth <= 0 || displayHeight <= 0) {
            return new int[] {0, 0, 0, 0};
        }
        int left = Math.round(x * scale);
        int top = Math.round(y * scale);
        int right = Math.round((x + width) * scale);
        int bottom = Math.round((y + height) * scale);
        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }
        if (right > displayWidth) {
            right = displayWidth;
        }
        if (bottom > displayHeight) {
            bottom = displayHeight;
        }
        int fbW = right - left;
        int fbH = bottom - top;
        if (fbW <= 0 || fbH <= 0) {
            return new int[] {0, 0, 0, 0};
        }
        return new int[] {left, displayHeight - bottom, fbW, fbH};
    }

    private static void applyScaled(
            float x, float y, float width, float height,
            float scaleFactor, int displayWidth, int displayHeight
    ) {
        int[] box = toFramebuffer(x, y, width, height, scaleFactor, displayWidth, displayHeight);
        GL11.glScissor(box[0], box[1], box[2], box[3]);
    }
}
