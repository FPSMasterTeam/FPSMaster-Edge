package top.fpsmaster.utils.render.state;

import top.fpsmaster.utils.render.draw.Colors;

public class Alpha {
    private static float globalAlpha = 1f;

    public static void set(float alpha) {
        globalAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    public static float get() {
        return globalAlpha;
    }

    public static int apply(int color) {
        int a = Colors.clamp((int) (Colors.toColor(color).getAlpha() * globalAlpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}

