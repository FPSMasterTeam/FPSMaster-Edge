package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.Color;

/**
 * 把原版隐藏的饱和度显示成一根条加数值。原版饥饿条照常渲染，不受影响。
 */
public class SaturationDisplay extends InterfaceModule {
    public static boolean using;

    public static ColorSetting barColor = new ColorSetting("BarColor", new Color(255, 190, 60));

    public SaturationDisplay() {
        super("SaturationDisplay", Category.Interface, Trait.BACKGROUND, Trait.TEXT);
        addSettings(barColor);
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
