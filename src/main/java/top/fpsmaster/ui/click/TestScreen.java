package top.fpsmaster.ui.click;

import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;

import java.awt.*;

public class TestScreen extends ScaledGuiScreen {

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(Utility.mc);
        int factor = sr.getScaleFactor();
        int realWidth = sr.getScaledWidth() * sr.getScaleFactor() / 2;
        int realHeight = sr.getScaledHeight() * sr.getScaleFactor() / 2;
        int realMouseX = mouseX * factor / 2;
        int realMouseY = mouseY * factor / 2;

//        GL11.glScaled((double) 2 / sr.getScaleFactor(), (double) 2 / sr.getScaleFactor(), 1.0);

        if (Render2DUtils.isHovered(10, 10, realWidth - 20, 100, mouseX * factor / 2, mouseY * factor / 2)) {
            Render2DUtils.drawOptimizedRoundedRect(10, 10, realWidth - 20, 100, 10,Color.RED.getRGB());
        } else {
            Render2DUtils.drawOptimizedRoundedRect(10, 10, realWidth - 20, 100, 10,Color.WHITE.getRGB());
        }
    }
}
