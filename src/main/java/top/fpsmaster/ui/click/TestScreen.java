package top.fpsmaster.ui.click;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

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

        if (Hover.is(10, 10, realWidth - 20, 100, mouseX * factor / 2, mouseY * factor / 2)) {
            Rects.rounded(10, 10, realWidth - 20, 100, 10,Color.RED.getRGB());
        } else {
            Rects.rounded(10, 10, realWidth - 20, 100, 10,Color.WHITE.getRGB());
        }
    }
}




