package top.fpsmaster.features.impl;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.*;

public class InterfaceModule extends Module {

    public BooleanSetting rounded = new BooleanSetting("Round", true);
    public NumberSetting roundRadius = new NumberSetting("RoundRadius", 3, 0, 30, 1, () -> rounded.getValue());
    public BooleanSetting betterFont = new BooleanSetting("BetterFont", false);
    public BooleanSetting fontShadow = new BooleanSetting("FontShadow", true);
    public BooleanSetting bg = new BooleanSetting("Background", true);
    public ColorSetting backgroundColor = new ColorSetting("BackgroundColor", new Color(0, 0, 0, 0), () -> bg.getValue());
    public NumberSetting spacing = new NumberSetting("Spacing",0,0,3,1);


    public InterfaceModule(String name, Category category) {
        super(name, category);
    }
}
