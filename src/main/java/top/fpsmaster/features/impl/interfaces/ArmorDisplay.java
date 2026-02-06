package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ModeSetting;

public class ArmorDisplay extends InterfaceModule {
    public static ModeSetting mode = new ModeSetting("Mode", 0, "SimpleHoriz", "SimpleVertical", "Vertical");

    public ArmorDisplay() {
        super("ArmorDisplay", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, spacing, mode, bg, rounded, roundRadius);
    }
}