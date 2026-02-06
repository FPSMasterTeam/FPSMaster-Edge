package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.shader.GLSLSandboxShader;
import top.fpsmaster.utils.system.OSUtil;

import java.awt.*;
import java.io.File;

public class Backgrounds {
    public static float animation = 0f;
    private static GLSLSandboxShader shader;
    private static long initTime = System.currentTimeMillis();

    static {
        try {
            shader = new GLSLSandboxShader("bg1.frag");
        } catch (Exception e) {
            OSUtil.supportShader = false;
        }
    }

    public static void draw(int guiWidth, int guiHeight, int mouseX, int mouseY, float partialTicks, int zLevel) {
        ResourceLocation textureLocation;
        if (FileUtils.hasBackground) {
            textureLocation = new ResourceLocation("fpsmaster/gui/background.png");
            File file = FileUtils.background;
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ThreadDownloadImageData textureArt = new ThreadDownloadImageData(file, null, null, null);
            textureManager.loadTexture(textureLocation, textureArt);
            Images.draw(textureLocation, 0f, 0f, guiWidth, guiHeight, -1);
            Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(22, 22, 22, 50));
        } else {
            if (OSUtil.supportShader() && !FPSMaster.configManager.configure.getOrCreate("background", "new").equals("classic")) {
                if (Minecraft.getMinecraft().currentScreen instanceof MainMenu) {
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
                Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(26, 59, 109, 60));
            } else {
                Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(0, 0, 0, 255));
            }
        }
    }
}
