package top.fpsmaster.ui.devspace;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.io.IOException;

import static top.fpsmaster.utils.Utility.mc;

public class AIPanel {
    private int x = 0, y = 0;
    private final int width = 320, height = 600;
    private int dragX = 0, dragY = 0;
    private boolean dragging = false;

    public void render(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);

        drawBackground(sr);

        handleDragging(mouseX, mouseY);
    }

    private void drawBackground(ScaledResolution sr) {
        Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, 10, new Color(0, 0, 0, 120).getRGB());
    }

    private void handleDragging(int mouseX, int mouseY) {
        if (!Mouse.isButtonDown(0) && dragging) {
            dragging = false;
        }

        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    }

    public void onClick(int mouseX, int mouseY, int mouseButton) {
        handleDraggingStart(mouseX, mouseY, mouseButton);
    }


    private void handleDraggingStart(int mouseX, int mouseY, int mouseButton) {
        if (Render2DUtils.isHovered(x, y, width, 15, mouseX, mouseY) && mouseButton == 0) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }
    }
}
