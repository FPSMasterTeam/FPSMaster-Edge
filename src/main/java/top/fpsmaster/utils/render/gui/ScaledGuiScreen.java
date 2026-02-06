package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.utils.render.gui.GuiScale;

import java.io.IOException;

public class ScaledGuiScreen extends GuiScreen {
    public int scaleFactor;
    public float guiWidth;
    public float guiHeight;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glPushMatrix();
        int realMouseX = mouseX * scaleFactor / 2;
        int realMouseY = mouseY * scaleFactor / 2;
        scaleFactor = GuiScale.fixScale();
        float[] bounds = GuiScale.getFixedBounds();
        guiWidth = bounds[0];
        guiHeight = bounds[1];
        render(realMouseX, realMouseY, partialTicks);
        GL11.glPopMatrix();
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
        scaleFactor = GuiScale.fixScale();
        float[] bounds = GuiScale.getFixedBounds();
        guiWidth = bounds[0];
        guiHeight = bounds[1];
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int realMouseX = mouseX * scaleFactor / 2;
        int realMouseY = mouseY * scaleFactor / 2;
        onClick(realMouseX, realMouseY, mouseButton);
    }

    public void render(int mouseX, int mouseY, float partialTicks) {

    }

    public void onClick(int mouseX, int mouseY, int mouseButton) {

    }
}



