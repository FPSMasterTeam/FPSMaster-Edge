package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.ClientSettings;

public class GuiScale {
    public static int fixScale() {
        int scaleFactor = getFixedScale();
        GL11.glScaled(2.0 / scaleFactor, 2.0 / scaleFactor, 1.0);
        return scaleFactor;
    }

    public static int getFixedScale() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        if (ClientSettings.fixedScale.getValue()) {
            return sr.getScaleFactor();
        }
        return 2;
    }

    public static float[] getFixedBounds() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scaleFactor = ClientSettings.fixedScale.getValue() ? sr.getScaleFactor() : 2;
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;
        return new float[]{guiWidth, guiHeight};
    }
}
