package top.fpsmaster.utils.render.draw;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Colors;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.utils.imaging.AWTUtils;
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

    public static void rounded(float x, float y, float width, float height, int radius, Color color) {
        rounded(x, y, width, height, radius, color.getRGB());
    }

    public static void rounded(float x, float y, float width, float height, Color color) {
        rounded(x, y, width, height, 3, color.getRGB());
    }

    public static void rounded(float x, float y, float width, float height, int radius, int color) {
        rounded(x, y, width, height, radius, color, false);
    }

    public static void rounded(float x, float y, float width, float height, int color) {
        rounded(x, y, width, height, 3, color, false);
    }

    public static void roundedBorder(float x, float y, float width, float height, int radius, float lineWidth, int fill, int border) {
        rounded(x - lineWidth, y - lineWidth, width + lineWidth * 2, height + lineWidth * 2, radius + 2, border, false);
        rounded(x, y, width, height, radius, fill, false);
    }

    public static void roundedImage(float x, float y, float width, float height, int radius, Color color) {
        try {
            ResourceLocation mask = AWTUtils.generateRoundImage((int) width, (int) height, radius);
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
        ResourceLocation[] resourceLocations = AWTUtils.generateRound(radius);
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


