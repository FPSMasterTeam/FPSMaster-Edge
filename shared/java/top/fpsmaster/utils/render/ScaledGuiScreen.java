package top.fpsmaster.utils.render;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.ClientSettings;

import java.awt.*;
import java.io.IOException;

public class ScaledGuiScreen extends GuiScreen {
    public int scaleFactor;
    public float guiWidth;
    public float guiHeight;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(mc);
        if (ClientSettings.Companion.getFixedScale().getValue()) {
            scaleFactor = sr.getScaleFactor();
        } else {
            scaleFactor = 2;
        }
        guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        guiHeight = sr.getScaledHeight() / 2f * scaleFactor;
        int realMouseX = mouseX * scaleFactor / 2;
        int realMouseY = mouseY * scaleFactor / 2;

        GL11.glPushMatrix();
        GL11.glScaled((double) 1 / scaleFactor * 2.0, (double) 1 / scaleFactor * 2.0, 1.0);
        render(realMouseX, realMouseY);
        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        ScaledResolution sr = new ScaledResolution(mc);
        int realMouseX = mouseX * scaleFactor / 2;
        int realMouseY = mouseY * scaleFactor / 2;
        onClick(realMouseX, realMouseY, mouseButton);
    }

    public float getScaleFactor() {
        ScaledResolution sr = new ScaledResolution(this.mc);
        return sr.getScaleFactor();
    }

    public void render(int mouseX, int mouseY) {

    }

    public void onClick(int mouseX, int mouseY, int mouseButton) {

    }
}
