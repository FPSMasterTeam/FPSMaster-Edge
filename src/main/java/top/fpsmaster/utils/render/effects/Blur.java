package top.fpsmaster.utils.render.effects;

import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.shader.KawaseBlur;
import top.fpsmaster.utils.render.shader.RoundedUtil;

import java.awt.*;

public class Blur {
    public static void area(int x, int y, int width, int height, int radius, Color color) {
        area((float) x, y, width, height, radius, color, 3, 3);
    }

    public static void area(float x, float y, float width, float height, int radius, Color color, int iterations, int offset) {
        StencilUtil.initStencilToWrite();
        RoundedUtil.drawRound(x, y, width, height, radius, true, color);
        StencilUtil.readStencilBuffer(1);
        KawaseBlur.renderBlur(iterations, offset);
        StencilUtil.uninitStencilBuffer();
    }
}
