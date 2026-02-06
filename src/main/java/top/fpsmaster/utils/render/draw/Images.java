package top.fpsmaster.utils.render.draw;

import top.fpsmaster.utils.render.draw.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL14;
import top.fpsmaster.utils.render.state.Alpha;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;

public class Images {
    public static void draw(ResourceLocation res, float x, float y, float width, float height) {
        draw(res, x, y, width, height, -1, false);
    }

    public static void draw(ResourceLocation res, int x, int y, int width, int height) {
        draw(res, x, y, width, height, -1, false);
    }

    public static void draw(ResourceLocation res, float x, float y, float width, float height, Color color) {
        draw(res, x, y, width, height, color.getRGB(), false);
    }

    public static void draw(ResourceLocation res, int x, int y, int width, int height, Color color) {
        draw(res, x, y, width, height, color.getRGB(), false);
    }

    public static void draw(ResourceLocation res, float x, float y, float width, float height, int color) {
        draw(res, x, y, width, height, color, false);
    }

    public static void draw(ResourceLocation res, int x, int y, int width, int height, int color) {
        draw(res, x, y, width, height, color, false);
    }

    public static void draw(ResourceLocation res, float x, float y, float width, float height, int color, boolean rawImage) {
        if (!rawImage) {
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glDepthMask(false);
            GL14.glBlendFuncSeparate(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE, org.lwjgl.opengl.GL11.GL_ZERO);
            glColor(color);
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(res);
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        if (!rawImage) {
            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_DEPTH_TEST);
        }
    }

    public static void draw(ResourceLocation res, int x, int y, int width, int height, int color, boolean rawImage) {
        if (!rawImage) {
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glDepthMask(false);
            GL14.glBlendFuncSeparate(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE, org.lwjgl.opengl.GL11.GL_ZERO);
            glColor(color);
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(res);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        if (!rawImage) {
            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_DEPTH_TEST);
        }
    }

    public static void playerHead(AbstractClientPlayer player, float x, float y, int w, int h) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8, 8, 8, 8, w, h, 64, 64);
    }

    private static void glColor(int color) {
        Color c = Colors.toColor(Alpha.apply(color));
        org.lwjgl.opengl.GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, c.getAlpha() / 255.0F);
    }

    public static void scaleStart(float x, float y, float scale) {
        glPushMatrix();
        glTranslatef(x, y, 0);
        glScalef(scale, scale, 1);
        glTranslatef(-x, -y, 0);
    }

    public static void scaleEnd() {
        glPopMatrix();
    }

    private static void drawModalRectWithCustomSizedTexture(float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
        bufferbuilder.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 0.0D).tex(u * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).tex((u + width) * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).tex((u + width) * f, v * f1).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }
}


