package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
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

    /**
     * Shrink a widget hit box to the current clip. {@code clip == null} keeps the
     * original rectangle (no scissor is active). Empty width/height means the
     * widget is fully outside the clip and must not consume the pointer.
     */
    public static float[] constrainHit(float[] clip, float x, float y, float w, float h) {
        if (clip == null) {
            return new float[] {x, y, w, h};
        }
        return intersect(clip[0], clip[1], clip[2], clip[3], x, y, w, h);
    }

    public static boolean hasArea(float[] rect) {
        return rect != null && rect[2] > 0f && rect[3] > 0f;
    }

    public static boolean contains(float[] rect, float px, float py) {
        return hasArea(rect)
                && px >= rect[0] && px <= rect[0] + rect[2]
                && py >= rect[1] && py <= rect[1] + rect[3];
    }

    public static void apply(float x, float y, float width, float height) {
        float scale = UiScale.isActive() ? UiScale.getLayoutScale() : 1.0f;
        applyScaled(x, y, width, height, scale);
    }

    public static void apply(float x, float y, float width, float height, float scaleFactor) {
        applyScaled(x, y, width, height, scaleFactor);
    }

    public static void apply(float x, float y, float width, float height, int scaleFactor) {
        applyScaled(x, y, width, height, 2.0f);
    }

    private static void applyScaled(float x, float y, float width, float height, float scaleFactor) {
        if (Minecraft.getMinecraft().currentScreen == null) {
            return;
        }
        int displayHeight = Minecraft.getMinecraft().displayHeight;
        int sx = Math.round(x * scaleFactor);
        int sy = Math.round(y * scaleFactor);
        int sw = Math.round(width * scaleFactor);
        int sh = Math.round(height * scaleFactor);
        GL11.glScissor(
                sx,
                displayHeight - (sy + sh),
                sw,
                sh
        );
    }
}
