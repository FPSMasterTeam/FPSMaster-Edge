package top.fpsmaster.utils.render.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.utils.Utility;

import java.awt.*;

public class GradientUtils extends Utility {

    private static final ShaderUtil gradientMaskShader = new ShaderUtil(Shaders.gradientMask);
    private static final ShaderUtil gradientShader = new ShaderUtil(Shaders.gradient);

    public static void drawGradient(float x, float y, float width, float height, float alpha, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        applyGradient(x, y, width, height, alpha, bottomLeft, topLeft, bottomRight, topRight,sr.getScaleFactor(), () -> ShaderUtil.drawQuads(x, y, width, height));
    }


    public static void applyGradient(float x, float y, float width, float height, float alpha, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight, int scale, Runnable content) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        gradientMaskShader.init();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

        int factor = sr.getScaleFactor();
        if (ClientSettings.Companion.getFixedScale().getValue()) {
            factor = 2;
        }
        gradientMaskShader.setUniformf("location", x * factor, (Minecraft.getMinecraft().displayHeight - (height * factor)) - (y * factor));
        gradientMaskShader.setUniformf("rectSize", width * factor, height * factor);
        gradientMaskShader.setUniformf("alpha", alpha);
        gradientMaskShader.setUniformi("tex", 0);
        // Bottom Left
        gradientMaskShader.setUniformf("color1", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f);
        //Top left
        gradientMaskShader.setUniformf("color2", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f);
        //Bottom Right
        gradientMaskShader.setUniformf("color3", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f);
        //Top Right
        gradientMaskShader.setUniformf("color4", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f);

        //Apply the gradient to whatever is put here
        content.run();

        gradientMaskShader.unload();
        GlStateManager.disableBlend();
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float) interpolationValue).intValue();
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static void drawGradientVertical(float x, float y, float width, float height, Color c1, Color c2) {
        GL11.glDisable(2929);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glDepthMask(true);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
        GL11.glShadeModel(7425);
        GL11.glBegin(7);
        glColor(c1);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x, y);
        glColor(c2);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glEnd();

        GL11.glShadeModel(7424);
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDisable(2848);
        GL11.glHint(3154, 4352);
        GL11.glHint(3155, 4352);
    }

    public static void glColor(Color c) {
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
    }
}
