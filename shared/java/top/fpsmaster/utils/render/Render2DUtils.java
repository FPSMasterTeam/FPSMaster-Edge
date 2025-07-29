package top.fpsmaster.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.awt.AWTUtils;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.os.HttpRequest;
import top.fpsmaster.utils.os.OSUtil;
import top.fpsmaster.utils.render.shader.GLSLSandboxShader;
import top.fpsmaster.utils.render.shader.KawaseBlur;
import top.fpsmaster.utils.render.shader.RoundedUtil;
import top.fpsmaster.wrapper.renderEngine.bufferbuilder.WrapperBufferBuilder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

public class Render2DUtils extends Utility {
    // AWT
    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, Color color) {
        drawOptimizedRoundedRect(x, y, width, height, 3, color.getRGB());
    }

    public static void drawOptimizedRoundedBorderRect(float x, float y, float width, float height, float lineWidth, Color color, Color border) {
        drawOptimizedRoundedRect(x - lineWidth, y - lineWidth, width + lineWidth * 2, height + lineWidth * 2, 5, border.getRGB());
        drawOptimizedRoundedRect(x, y, width, height, 3, color.getRGB());
    }

    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, int color) {
        drawOptimizedRoundedRect(x, y, width, height, 3, color);
    }


    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, int radius, int color) {
        drawOptimizedRoundedRect(x, y, width, height, radius, color, false);
    }

    public static void drawOptimizedRoundedRect(float x, float y, float width, float height, int radius, int color, boolean rawImage) {
        radius = (int) Math.min(Math.min(height, width) / 2, radius);
        if (width < radius * 2 || radius < 1) {
            drawRect(x, y, width, height, color);
            return;
        }
        ResourceLocation[] resourceLocations = AWTUtils.generateRound(radius);
        if (resourceLocations == null || resourceLocations.length == 0) {
            return;
        }
        drawRect(x + radius, y, width - radius * 2, radius, color);
        drawRect(x + radius, y + height - radius, width - radius * 2, radius, color);
        drawRect(x, y + radius, radius, height - radius * 2, color);
        drawRect(x + width - radius, y + radius, radius, height - radius * 2, color);
        drawRect(x + radius, y + radius, width - radius * 2, height - radius * 2, color);
        drawImage(resourceLocations[0], x, y, radius, radius, color, rawImage);
        drawImage(resourceLocations[1], x + width - radius, y, radius, radius, color, rawImage);
        drawImage(resourceLocations[2], x, y + height - radius, radius, radius, color, rawImage);
        drawImage(resourceLocations[3], x + width - radius, y + height - radius, radius, radius, color, rawImage);
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
        drawRect(x, y, width, height, color.getRGB());
    }


    public static void drawImage(ResourceLocation res, float x, float y, float width, float height, Color color) {
        drawImage(res, x, y, width, height, color.getRGB(), false);
    }

    public static void drawImage(ResourceLocation res, float x, float y, float width, float height, int color) {
        drawImage(res, x, y, width, height, color, false);
    }

    public static void drawImage(ResourceLocation res, float x, float y, float width, float height, int color, boolean rawImage) {
        if (!rawImage) {
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glDepthMask(false);
            GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
            glColor(color);
        }
        mc.getTextureManager().bindTexture(res);
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        if (!rawImage) {
            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_DEPTH_TEST);
        }
    }

    public static void drawRoundedRectImage(float x, float y, float width, float height, int radius, Color color) {
        try {
            ResourceLocation res = AWTUtils.generateRoundImage((int) width, (int) height, radius);
            Render2DUtils.drawImage(res, x, y, width, height, color);
        }catch (IllegalArgumentException e){
            Render2DUtils.drawRect(x, y, width, height, color);
        }
    }

    public static void drawHue(float x, float y, int width, float height) {
        float hue = 0;
        float increment = 1.0F / height;
        for (int i = 0; i < height; i++) {
            drawRect(x, y + i, width, 1, Color.getHSBColor(hue, 1.0F, 1.0F).getRGB());
            hue += increment;
        }
    }

    public static void drawPlayerHead(EntityPlayer target, float x, float y, int w, int h) {
        mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8, 8, 8, 8, w, h, 64, 64);
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glColor(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x, y + height);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x + width, y);
        GL11.glEnd();
        glDisable(GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawModalRectWithCustomSizedTexture(float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WrapperBufferBuilder bufferbuilder = new WrapperBufferBuilder(tessellator);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 0.0D).tex(u * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).tex((u + width) * f, (v + height) * f1).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).tex((u + width) * f, v * f1).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }


    // other

    public static Color reAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), limit(alpha));
    }

    public static int limit(double i) {
        if (i > 255)
            return 255;
        if (i < 0)
            return 0;
        return (int) i;
    }


    public static Color intToColor(Integer c) {
        return new Color(c >> 16 & 255, c >> 8 & 255, c & 255, c >> 24 & 255);
    }

    private static void glColor(int color) {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int alpha = color >> 24 & 255;
        GL11.glColor4f(red / 255.0F, green / 255.0F, blue / 255.0F, alpha / 255.0F);
    }

    public static void doGlScissor(float x, float y, float width, float height, int scaleFactor) {
        if (mc.currentScreen != null) {
            width *= 1f / scaleFactor * 2;
            height *= 1f / scaleFactor * 2;
            y *= 1f / scaleFactor * 2;
            x *= 1f / scaleFactor * 2;
            GL11.glScissor((int) (x * mc.displayWidth / mc.currentScreen.width), (int) (mc.displayHeight - (y + height) * mc.displayHeight / mc.currentScreen.height), (int) (width * mc.displayWidth / mc.currentScreen.width), (int) (height * mc.displayHeight / mc.currentScreen.height));
        }
    }




    public static boolean isHovered(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static boolean isHovered(Bounding bounding, int mouseX, int mouseY) {
        return isHovered(bounding.x, bounding.y, bounding.width, bounding.height, mouseX, mouseY);
    }

    public static int fixScale() {
        int scaleFactor = getFixedScale();
        GL11.glScaled(2.0 / scaleFactor, 2.0 / scaleFactor, 1.0);
        return scaleFactor;
    }

    public static int getFixedScale() {
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor;
        if (ClientSettings.fixedScale.getValue()) {
            scaleFactor = sr.getScaleFactor();
        } else {
            scaleFactor = 2;
        }
        return scaleFactor;
    }

    public static float[] getFixedBounds() {
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor;
        if (ClientSettings.fixedScale.getValue()) {
            scaleFactor = sr.getScaleFactor();
        } else {
            scaleFactor = 2;
        }
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;
        return new float[]{guiWidth, guiHeight};
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



    public static void beginBlend() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void endBlend() {
        GlStateManager.disableBlend();
    }

    public static void drawBlurArea(int x, int y, int width, int height, int radius, Color color) {
        drawBlurArea((float) x, y, width, height, radius, color);
    }

    public static void drawBlurArea(float x, float y, float width, float height, int radius, Color color) {
        StencilUtil.initStencilToWrite();
        RoundedUtil.drawRound(x, y, width, height, radius, true, color);
        StencilUtil.readStencilBuffer(1);
        KawaseBlur.renderBlur(3, 3);
        StencilUtil.uninitStencilBuffer();
    }


    // background(shader)
    public static float animation = 0f;
    static GLSLSandboxShader shader;
    static long initTime = System.currentTimeMillis();

    static {
        try {
            shader = new GLSLSandboxShader("bg1.frag");
        } catch (Exception e) {
            OSUtil.supportShader = false;
        }
    }

    public static void drawBackground(int guiWidth, int guiHeight, int mouseX, int mouseY, float partialTicks, int zLevel) {
        ResourceLocation textureLocation;
        if (FileUtils.hasBackground) {
            textureLocation = new ResourceLocation("fpsmaster/gui/background.png");
            File file = FileUtils.background;
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ThreadDownloadImageData textureArt = new ThreadDownloadImageData(file, null, null, null);
            textureManager.loadTexture(textureLocation, textureArt);
            Render2DUtils.drawImage(textureLocation, 0f, 0f, guiWidth, guiHeight, -1);
            Render2DUtils.drawRect(0f, 0f, guiWidth, guiHeight, new Color(22, 22, 22, 50));
        } else {
            if (OSUtil.supportShader() && !FPSMaster.configManager.configure.getOrCreate("background", "new").equals("classic")) {
                if (mc.currentScreen instanceof MainMenu) {
                    animation = (float) AnimationUtils.base(animation, 1.0f, 0.05f);
                } else {
                    animation = (float) AnimationUtils.base(animation, 0.0f, 0.05f);
                }
                GlStateManager.disableCull();
                shader.useShader(guiWidth, guiHeight, mouseX, mouseY, (System.currentTimeMillis() - initTime) / 1000f, animation);
                GL11.glBegin(GL11.GL_QUADS);

                GL11.glVertex2f(-1f, -1f);
                GL11.glVertex2f(-1f, 1f);
                GL11.glVertex2f(1f, 1f);
                GL11.glVertex2f(1f, -1f);

                GL11.glEnd();

                GL20.glUseProgram(0);

                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                Render2DUtils.drawRect(0f, 0f, guiWidth, guiHeight, new Color(26, 59, 109, 60));
            } else {
                ProviderManager.mainmenuProvider.renderSkybox(mouseX, mouseY, partialTicks, guiWidth, guiHeight, zLevel);
            }
        }
    }
}
