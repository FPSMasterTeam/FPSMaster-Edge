package top.fpsmaster.features.impl.render;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.Color;

public class HitColor extends Module {

    public static final ColorSetting color = new ColorSetting("Color", new Color(255, 0, 0, 120));
    public static boolean using = false;

    public HitColor() {
        super("HitColor", Category.RENDER);
        addSettings(color);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    public static float getRed() {
        return color.getColor().getRed() / 255f;
    }

    public static float getGreen() {
        return color.getColor().getGreen() / 255f;
    }

    public static float getBlue() {
        return color.getColor().getBlue() / 255f;
    }

    public static float getAlpha() {
        return color.getColor().getAlpha() / 255f;
    }
}
