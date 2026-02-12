package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.glu.Project;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.shader.GLSLSandboxShader;
import top.fpsmaster.utils.system.OSUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import static top.fpsmaster.utils.core.Utility.mc;

public class Backgrounds {
    public static float animation = 0f;
    private static final ResourceLocation CUSTOM_BG_LOCATION = new ResourceLocation("fpsmaster/gui/background.png");
    private static GLSLSandboxShader shader;
    private static long initTime = System.currentTimeMillis();
    private static long customBgLastModified = -1L;
    private static boolean customTextureLoaded = false;
    private static float customImageWidth = 1f;
    private static float customImageHeight = 1f;
    private static final int PANORAMA_FACE_COUNT = 6;
    private static final int PANORAMA_VIEWPORT_SIZE = 256;
    private static final String DEFAULT_PANORAMA_STYLE = "panorama_1";
    private static String activePanoramaStyle = DEFAULT_PANORAMA_STYLE;
    private static ResourceLocation[] titlePanoramaPaths = createPanoramaPaths(DEFAULT_PANORAMA_STYLE);
    private static int panoramaTimer = 0;
    private static long panoramaTimerLastUpdate = System.currentTimeMillis();

    static {
        try {
            shader = new GLSLSandboxShader("bg1.frag");
        } catch (Exception e) {
            OSUtil.supportShader = false;
        }
    }

    public static void draw(int guiWidth, int guiHeight, int mouseX, int mouseY, float partialTicks, int zLevel) {
        float uiScale = UiScale.isActive() ? UiScale.getScale() : 1.0f;
        int scaledGuiWidth = Math.round(guiWidth / uiScale);
        int scaledGuiHeight = Math.round(guiHeight / uiScale);
        int scaledMouseX = Math.round(mouseX * uiScale);
        int scaledMouseY = Math.round(mouseY * uiScale);

        String backgroundMode = FPSMaster.configManager.configure.background;
        if ("custom".equals(backgroundMode)) {
            drawCustom(scaledGuiWidth, scaledGuiHeight);
        } else if ("classic".equals(backgroundMode)) {
            CustomColor base = new CustomColor(
                    FPSMaster.configManager.configure.classicBackgroundHue,
                    FPSMaster.configManager.configure.classicBackgroundBrightness,
                    FPSMaster.configManager.configure.classicBackgroundSaturation,
                    FPSMaster.configManager.configure.classicBackgroundAlpha
            );
            ColorSetting.ColorType mode = ColorSetting.ColorType.STATIC;
            try {
                mode = ColorSetting.ColorType.valueOf(FPSMaster.configManager.configure.classicBackgroundMode);
            } catch (IllegalArgumentException ignored) {
            }
            Rects.fill(0f, 0f, scaledGuiWidth, scaledGuiHeight, ColorSetting.resolveColor(base, mode, 0f).getRGB());
        } else if (isPanoramaMode(backgroundMode)) {
            renderSkybox(guiWidth, guiHeight, normalizePanoramaStyle(backgroundMode), partialTicks, zLevel);
            Rects.fill(0f, 0f, scaledGuiWidth, scaledGuiHeight, new Color(22, 22, 22, 30));
        } else if ("shader".equals(backgroundMode)) {
            if (OSUtil.supportShader()) {
                if (Minecraft.getMinecraft().currentScreen instanceof MainMenu) {
                    animation = (float) AnimMath.base(animation, 1.0f, 0.05f);
                } else {
                    animation = (float) AnimMath.base(animation, 0.0f, 0.05f);
                }
                GlStateManager.disableCull();
                shader.useShader(guiWidth, guiHeight, scaledMouseX, scaledMouseY, (System.currentTimeMillis() - initTime) / 1000f, animation);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(-1f, -1f);
                GL11.glVertex2f(-1f, 1f);
                GL11.glVertex2f(1f, 1f);
                GL11.glVertex2f(1f, -1f);
                GL11.glEnd();
                GL20.glUseProgram(0);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                Rects.fill(0f, 0f, scaledGuiWidth, scaledGuiHeight, new Color(26, 59, 109, 60));
            } else {
                Rects.fill(0f, 0f, scaledGuiWidth, scaledGuiHeight, new Color(0, 0, 0, 255));
            }
        } else {
            Rects.fill(0f, 0f, scaledGuiWidth, scaledGuiHeight, new Color(108, 108, 108, 255));
        }
    }

    private static void drawCustom(int guiWidth, int guiHeight) {
        File file = FileUtils.background;
        if (file == null || !file.exists()) {
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(0, 0, 0, 255));
            return;
        }

        long modified = file.lastModified();
        if (!customTextureLoaded || customBgLastModified != modified) {
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ThreadDownloadImageData textureArt = new ThreadDownloadImageData(file, null, null, null);
            textureManager.loadTexture(CUSTOM_BG_LOCATION, textureArt);
            customTextureLoaded = true;
            customBgLastModified = modified;

            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
                    customImageWidth = image.getWidth();
                    customImageHeight = image.getHeight();
                }
            } catch (IOException ignored) {
            }
        }

        float scale = Math.min(guiWidth / customImageWidth, guiHeight / customImageHeight);
        float drawWidth = customImageWidth * scale;
        float drawHeight = customImageHeight * scale;
        float drawX = (guiWidth - drawWidth) * 0.5f;
        float drawY = (guiHeight - drawHeight) * 0.5f;

        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(0, 0, 0, 255));
        Images.draw(CUSTOM_BG_LOCATION, drawX, drawY, drawWidth, drawHeight, -1);
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(22, 22, 22, 40));
    }

    private static boolean isPanoramaMode(String backgroundMode) {
        return "panorama".equals(backgroundMode) || (backgroundMode != null && backgroundMode.startsWith("panorama_"));
    }

    private static String normalizePanoramaStyle(String backgroundMode) {
        if (backgroundMode == null || "panorama".equals(backgroundMode)) {
            return DEFAULT_PANORAMA_STYLE;
        }
        return backgroundMode;
    }


    private static void drawPanorama(float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int tileCount = 8;

        for (int j = 0; j < tileCount * tileCount; ++j) {
            GlStateManager.pushMatrix();
            float offsetX = ((float) (j % tileCount) / (float) tileCount - 0.5F) / 64.0F;
            float offsetY = ((float) (j / tileCount) / (float) tileCount - 0.5F) / 64.0F;
            GlStateManager.translate(offsetX, offsetY, 0.0F);
            GlStateManager.rotate(MathHelper.sin(((float) panoramaTimer + partialTicks) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-((float) panoramaTimer + partialTicks) * 0.1F, 0.0F, 1.0F, 0.0F);

            for (int k = 0; k < PANORAMA_FACE_COUNT; ++k) {
                GlStateManager.pushMatrix();
                if (k == 1) {
                    GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (k == 2) {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                if (k == 3) {
                    GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (k == 4) {
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (k == 5) {
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                mc.getTextureManager().bindTexture(titlePanoramaPaths[k]);
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int l = 255 / (j + 1);
                worldrenderer.pos((double)-1.0F, (double)-1.0F, (double)1.0F).tex((double)0.0F, (double)0.0F).color(255, 255, 255, l).endVertex();
                worldrenderer.pos((double)1.0F, (double)-1.0F, (double)1.0F).tex((double)1.0F, (double)0.0F).color(255, 255, 255, l).endVertex();
                worldrenderer.pos((double)1.0F, (double)1.0F, (double)1.0F).tex((double)1.0F, (double)1.0F).color(255, 255, 255, l).endVertex();
                worldrenderer.pos((double)-1.0F, (double)1.0F, (double)1.0F).tex((double)0.0F, (double)1.0F).color(255, 255, 255, l).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }

        worldrenderer.setTranslation((double)0.0F, (double)0.0F, (double)0.0F);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }


    private static DynamicTexture viewportTexture;
    private static ResourceLocation backgroundTexture;

    public static void initGui(){
        if (viewportTexture == null || backgroundTexture == null) {
            viewportTexture = new DynamicTexture(PANORAMA_VIEWPORT_SIZE, PANORAMA_VIEWPORT_SIZE);
            backgroundTexture = mc.getTextureManager().getDynamicTextureLocation("background", viewportTexture);
        }
    }

    private static void rotateAndBlurSkybox(int width, int height, int zLevel) {
        mc.getTextureManager().bindTexture(backgroundTexture);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, PANORAMA_VIEWPORT_SIZE, PANORAMA_VIEWPORT_SIZE);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        int i = 3;

        for(int j = 0; j < i; ++j) {
            float f = 1.0F / (float)(j + 1);
            float f1 = (float)(j - i / 2) / (float) PANORAMA_VIEWPORT_SIZE;
            worldrenderer.pos((double)width, (double)height, (double)zLevel).tex((double)f1, (double)1.0F).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos((double)width, (double)0.0F, (double)zLevel).tex((double)(1.0F + f1), (double)1.0F).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos((double)0.0F, (double)0.0F, (double)zLevel).tex((double)(1.0F + f1), (double)0.0F).color(1.0F, 1.0F, 1.0F, f).endVertex();
            worldrenderer.pos((double)0.0F, (double)height, (double)zLevel).tex((double)f1, (double)0.0F).color(1.0F, 1.0F, 1.0F, f).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    private static void renderSkybox(int width, int height, String style, float partialTicks, int zLevel) {
        ensurePanoramaStyle(style);
        updatePanoramaTimer();
        mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, PANORAMA_VIEWPORT_SIZE, PANORAMA_VIEWPORT_SIZE);
        drawPanorama(partialTicks);
        rotateAndBlurSkybox(width, height, zLevel);
        rotateAndBlurSkybox(width, height, zLevel);
        rotateAndBlurSkybox(width, height, zLevel);
        rotateAndBlurSkybox(width, height, zLevel);
        rotateAndBlurSkybox(width, height, zLevel);
        rotateAndBlurSkybox(width, height, zLevel);
        rotateAndBlurSkybox(width, height, zLevel);
        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        float f = width > height ? 120.0F / (float)width : 120.0F / (float)height;
        float f1 = (float)height * f / PANORAMA_VIEWPORT_SIZE;
        float f2 = (float)width * f / PANORAMA_VIEWPORT_SIZE;
        int i = width;
        int j = height;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos((double)0.0F, (double)j, (double)zLevel).tex((double)(0.5F - f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)i, (double)j, (double)zLevel).tex((double)(0.5F - f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)i, (double)0.0F, (double)zLevel).tex((double)(0.5F + f1), (double)(0.5F - f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos((double)0.0F, (double)0.0F, (double)zLevel).tex((double)(0.5F + f1), (double)(0.5F + f2)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }


    private static void updatePanoramaTimer() {
        long now = System.currentTimeMillis();
        long elapsed = now - panoramaTimerLastUpdate;
        if (elapsed <= 0L) {
            return;
        }
        int ticks = (int) (elapsed / 50L);
        if (ticks > 0) {
            panoramaTimer += ticks;
            panoramaTimerLastUpdate += ticks * 50L;
        }
    }

    private static void ensurePanoramaStyle(String panoramaStyle) {
        if (panoramaStyle.equals(activePanoramaStyle)) {
            return;
        }
        activePanoramaStyle = panoramaStyle;
        titlePanoramaPaths = createPanoramaPaths(panoramaStyle);
    }

    private static ResourceLocation[] createPanoramaPaths(String panoramaStyle) {
        ResourceLocation[] paths = new ResourceLocation[PANORAMA_FACE_COUNT];
        for (int i = 0; i < PANORAMA_FACE_COUNT; i++) {
            paths[i] = new ResourceLocation("client/background/" + panoramaStyle + "/panorama_" + i + ".png");
        }
        return paths;
    }
}
