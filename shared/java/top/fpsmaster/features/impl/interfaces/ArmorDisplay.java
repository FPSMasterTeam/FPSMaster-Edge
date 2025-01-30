package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;

import java.awt.Color;

public class ArmorDisplay extends InterfaceModule {
    public static ModeSetting mode = new ModeSetting("Mode", 0, "SimpleHoriz", "SimpleVertical", "Vertical");

    public ArmorDisplay() {
        super("ArmorDisplay", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, mode, bg, rounded, roundRadius);
    }
}