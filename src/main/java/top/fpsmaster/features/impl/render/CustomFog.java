package top.fpsmaster.features.impl.render;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;

import java.awt.*;

public class CustomFog extends Module {
    public static boolean using;

    public static ColorSetting color = new ColorSetting("Color", new Color(0, 200, 255));
    public static NumberSetting startDistance = new NumberSetting("StartDistance", 32.0, 0, 200, 1);
    public static NumberSetting endDistance = new NumberSetting("EndDistance", 64, 1, 200, 1);
    public static ModeSetting fogMode = new ModeSetting("FogMode", 0, "Linear", "Exponential");
    public static BooleanSetting affectWater = new BooleanSetting("AffectWater", false);
    public static BooleanSetting affectLava = new BooleanSetting("AffectLava", false);

    public CustomFog() {
        super("CustomFog", Category.RENDER);
        addSettings(color, fogMode, startDistance, endDistance, affectWater, affectLava);
    }

    @Override
    public void onEnable() {
        using = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        using = false;
        super.onDisable();
    }
}