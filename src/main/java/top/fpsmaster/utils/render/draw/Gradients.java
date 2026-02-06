package top.fpsmaster.utils.render.draw;

import java.awt.*;

public class Gradients {
    public static void hue(float x, float y, int width, float height) {
        float hue = 0;
        float increment = 1.0F / height;
        for (int i = 0; i < height; i++) {
            Rects.fill(x, y + i, width, 1, Color.getHSBColor(hue, 1.0F, 1.0F).getRGB());
            hue += increment;
        }
    }
}
