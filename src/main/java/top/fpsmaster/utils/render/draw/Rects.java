package top.fpsmaster.utils.render.draw;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Colors;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.utils.imaging.AWTUtils;
import top.fpsmaster.utils.render.gui.UiScale;
import top.fpsmaster.utils.render.state.Alpha;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_LINE_SMOOTH;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2d;

public class Rects {
    public static void fill(float x, float y, float width, float height, Color color) {
        fill(x, y, width, height, color.getRGB());
    }

    public static void fill(float x, float y, float width, float height, int color) {
        x = UiScale.scale(x);
        y = UiScale.scale(y);
        width = UiScale.scale(width);
        height = UiScale.scale(height);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glColor(color);
        glBegin(GL11.GL_QUADS);
        glVertex2d(x, y);
        glVertex2d(x, y + height);
        glVertex2d(x + width, y + height);
        glVertex2d(x + width, y);
        glEnd();
        glDisable(GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void rounded(int x, int y, int width, int height, int radius, Color color) {
        rounded(x, y, width, height, radius, color.getRGB());
    }

    public static void rounded(float x, float y, float width, float height, int radius, int color) {
        rounded(x, y, width, height, radius, color, false);
    }

    public static void rounded(int x, int y, int width, int height, Color color) {
        rounded(x, y, width, height, 3, color.getRGB());
    }

    public static void rounded(int x, int y, int width, int height, int radius, int color) {
        rounded(x, y, width, height, radius, color, false);
    }

    public static void rounded(int x, int y, int width, int height, int color) {
        rounded(x, y, width, height, 3, color, false);
    }

    public static void roundedBorder(int x, int y, int width, int height, int radius, float lineWidth, int fill, int border) {
        rounded(Math.round(x - lineWidth), Math.round(y - lineWidth), Math.round(width + lineWidth * 2), Math.round(height + lineWidth * 2), radius + 2, border, false);
        rounded(x, y, width, height, radius, fill, false);
    }

    /**
     * Stroke only — does not fill the interior. Chrome draws the fill first, then this ring;
     * covering the inside with a hardcoded dark cutout is what kept theme panels black and
     * hid homepage background previews.
     */
    public static void roundedOutline(float x, float y, float width, float height, float radius, float stroke, int color) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        float s = Math.max(0.5f, stroke);
        float r = Math.max(0f, Math.min(radius, Math.min(width, height) * 0.5f));
        if (r < 1f) {
            fill(x, y, width, s, color);
            fill(x, y + height - s, width, s, color);
            fill(x, y + s, s, height - s * 2f, color);
            fill(x + width - s, y + s, s, height - s * 2f, color);
            return;
        }
        float inner = Math.max(0f, r - s);
        fill(x + r, y, width - r * 2f, s, color);
        fill(x + r, y + height - s, width - r * 2f, s, color);
        fill(x, y + r, s, height - r * 2f, color);
        fill(x + width - s, y + r, s, height - r * 2f, color);
        quarterRing(x + r, y + r, inner, r, color, 180.0, 270.0);
        quarterRing(x + width - r, y + r, inner, r, color, 270.0, 360.0);
        quarterRing(x + r, y + height - r, inner, r, color, 90.0, 180.0);
        quarterRing(x + width - r, y + height - r, inner, r, color, 0.0, 90.0);
    }

    /**
     * Quarter annulus in Minecraft GUI space (Y grows down): 0° is +X, 90° is +Y.
     */
    private static void quarterRing(float cx, float cy, float inner, float outer, int color, double startDeg, double endDeg) {
        if (outer <= 0f) {
            return;
        }
        cx = UiScale.scale(cx);
        cy = UiScale.scale(cy);
        inner = UiScale.scale(inner);
        outer = UiScale.scale(outer);
        int segments = Math.max(8, Math.min(48, (int) (outer * 3f)));
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glColor(color);
        glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startDeg + (endDeg - startDeg) * i / (double) segments);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            glVertex2d(cx + cos * inner, cy + sin * inner);
            glVertex2d(cx + cos * outer, cy + sin * outer);
        }
        glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void roundedImage(int x, int y, int width, int height, int radius, Color color) {
        try {
            float pixelScale = UiScale.isActive() ? UiScale.getPixelScale() : 1.0f;
            ResourceLocation mask = AWTUtils.generateRoundImage(width, height, radius, pixelScale);
            Images.draw(mask, x, y, width, height, color.getRGB(), false);
        } catch (IllegalArgumentException e) {
            fill(x, y, width, height, color);
        }
    }

    public static void rounded(float x, float y, float width, float height, int radius, int color, boolean rawImage) {
        radius = (int) Math.min(Math.min(height, width) / 2, radius);
        if (width < radius * 2 || radius < 1) {
            fill(x, y, width, height, color);
            return;
        }
        float pixelScale = UiScale.isActive() ? UiScale.getPixelScale() : 1.0f;
        ResourceLocation[] resourceLocations = AWTUtils.generateRound(radius, pixelScale);
        if (resourceLocations == null || resourceLocations.length == 0) {
            return;
        }
        fill(x + radius, y, width - radius * 2, radius, color);
        fill(x + radius, y + height - radius, width - radius * 2, radius, color);
        fill(x, y + radius, radius, height - radius * 2, color);
        fill(x + width - radius, y + radius, radius, height - radius * 2, color);
        fill(x + radius, y + radius, width - radius * 2, height - radius * 2, color);
        Images.draw(resourceLocations[0], x, y, radius, radius, color, rawImage);
        Images.draw(resourceLocations[1], x + width - radius, y, radius, radius, color, rawImage);
        Images.draw(resourceLocations[2], x, y + height - radius, radius, radius, color, rawImage);
        Images.draw(resourceLocations[3], x + width - radius, y + height - radius, radius, radius, color, rawImage);
    }

    private static void glColor(int color) {
        Color c = Colors.toColor(Alpha.apply(color));
        GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, c.getAlpha() / 255.0F);
    }
}


