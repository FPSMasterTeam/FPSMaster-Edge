package top.fpsmaster.utils.render.draw;

import java.awt.*;

public class Colors {
    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha));
    }

    public static int clamp(int value) {
        if (value > 255) return 255;
        if (value < 0) return 0;
        return value;
    }

    public static int clamp(double value) {
        if (value > 255) return 255;
        if (value < 0) return 0;
        return (int) value;
    }

    public static Color toColor(int color) {
        return new Color(color >> 16 & 255, color >> 8 & 255, color & 255, color >> 24 & 255);
    }
}

