package top.fpsmaster.features.impl.render;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.Color;

public class FireModifier extends Module {

    public static boolean using = false;
    public static final NumberSetting height = new NumberSetting("Height", 0.5, 0, 0.7, 0.1);
    public static final BooleanSetting customColor = new BooleanSetting("CustomColor", false);
    public static final ColorSetting colorSetting = new ColorSetting("Color", new Color(255, 0, 0), customColor::getValue);

    public FireModifier() {
        super("FireModifier", Category.RENDER);
        addSettings(height, customColor, colorSetting);
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
}
