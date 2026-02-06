package top.fpsmaster.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.io.HttpRequest;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Gradients;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.effects.Blur;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.GuiScale;
import top.fpsmaster.utils.render.gui.Scissor;
import top.fpsmaster.utils.render.state.Alpha;
import top.fpsmaster.utils.render.state.Blend;
import top.fpsmaster.utils.render.types.Bounding;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

public class Render2DUtils extends Utility {
    public static void setGlobalAlpha(float alpha) {
        Alpha.set(alpha);
    }

    public static int applyGlobalAlpha(int color) {
        return Alpha.apply(color);
    }
    // AWT
    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, Color color) {
        Rects.rounded(x, y, width, height, 3, color.getRGB());
    }

    public static void drawOptimizedRoundedBorderRect(float x, float y, float width, float height, float lineWidth, Color color, Color border) {
        Rects.roundedBorder(x, y, width, height, 3, lineWidth, color.getRGB(), border.getRGB());
    }

    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, int color) {
        Rects.rounded(x, y, width, height, 3, color);
    }


    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, int radius, int color) {
        Rects.rounded(x, y, width, height, radius, color);
    }

    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, int radius, int color, boolean rawImage) {
        Rects.rounded(x, y, width, height, radius, color, rawImage);
    }

    static ArrayList<String> downloadingImages = new ArrayList<>();
    static ArrayList<String> downloadedImages = new ArrayList<>();


    public static void drawWebImage(String url, float x, float y, int width, int height) {
        if (downloadingImages.contains(url)){
            drawRoundedRectImage(x, y, width, height, 5, new Color(194, 194, 194, 255));
        } else if (downloadedImages.contains(url)) {
            drawImage(new ResourceLocation(url), x, y, width, height, -1);
        }else{
            downloadingImages.add(url);
            FPSMaster.async.runnable(()->{
                ResourceLocation textureLocation = new ResourceLocation(url);
                ThreadDownloadImageData downloadImageData = new ThreadDownloadImageData(null, null, textureLocation, null);
                try {
                    downloadImageData.setBufferedImage(HttpRequest.downloadImage(url));
                    mc.getTextureManager().loadTexture(textureLocation, downloadImageData);
                } catch (IOException ignored) {
                }
                downloadedImages.add(url);
                downloadingImages.remove(url);
            });
        }
    }

    // vanilla
    public static void drawRect(float x, float y, float width, float height, Color color) {
        Rects.fill(x, y, width, height, color);
    }


    public static void drawImage(ResourceLocation res, float x, float y, float width, float height, Color color) {
        Images.draw(res, x, y, width, height, color);
    }

    public static void drawImage(ResourceLocation res, float x, float y, float width, float height, int color) {
        Images.draw(res, x, y, width, height, color);
    }

    public static void drawImage(ResourceLocation res, float x, float y, float width, float height, int color, boolean rawImage) {
        Images.draw(res, x, y, width, height, color, rawImage);
    }

    public static void drawRoundedRectImage(float x, float y, float width, float height, int radius, Color color) {
        Rects.roundedImage(x, y, width, height, radius, color);
    }

    public static void drawHue(float x, float y, int width, float height) {
        Gradients.hue(x, y, width, height);
    }

    public static void drawPlayerHead(EntityPlayer target, float x, float y, int w, int h) {
        Images.playerHead((AbstractClientPlayer) target, x, y, w, h);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        Rects.fill(x, y, width, height, color);
    }

    public static void drawModalRectWithCustomSizedTexture(float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 0.0D).tex(u * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).tex((u + width) * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).tex((u + width) * f, v * f1).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }


    // other

    public static Color reAlpha(Color color, int alpha) {
        return Colors.alpha(color, alpha);
    }

    public static int limit(double i) {
        return Colors.clamp(i);
    }


    public static Color intToColor(Integer c) {
        return Colors.toColor(c);
    }

    private static void glColor(int color) {
        Color c = Colors.toColor(Alpha.apply(color));
        GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, c.getAlpha() / 255.0F);
    }

    public static void doGlScissor(float x, float y, float width, float height, int scaleFactor) {
        Scissor.apply(x, y, width, height, scaleFactor);
    }




    public static boolean isHovered(float x, float y, float width, float height, int mouseX, int mouseY) {
        return Hover.is(x, y, width, height, mouseX, mouseY);
    }

    public static boolean isHovered(Bounding bounding, int mouseX, int mouseY) {
        return Hover.is(bounding, mouseX, mouseY);
    }

    public static int fixScale() {
        return GuiScale.fixScale();
    }

    public static int getFixedScale() {
        return GuiScale.getFixedScale();
    }

    public static float[] getFixedBounds() {
        return GuiScale.getFixedBounds();
    }

    public static void scaleStart(float x, float y, float scale) {
        Images.scaleStart(x, y, scale);
    }

    public static void scaleEnd() {
        Images.scaleEnd();
    }



    public static void beginBlend() {
        Blend.begin();
    }

    public static void endBlend() {
        Blend.end();
    }

    public static void drawBlurArea(int x, int y, int width, int height, int radius, Color color) {
        Blur.area(x, y, width, height, radius, color);
    }

    public static void drawBlurArea(float x, float y, float width, float height, int radius, Color color, int iterations, int offset) {
        Blur.area(x, y, width, height, radius, color, iterations, offset);
    }


    // background(shader)
    public static void drawBackground(int guiWidth, int guiHeight, int mouseX, int mouseY, float partialTicks, int zLevel) {
        Backgrounds.draw(guiWidth, guiHeight, mouseX, mouseY, partialTicks, zLevel);
    }
}


