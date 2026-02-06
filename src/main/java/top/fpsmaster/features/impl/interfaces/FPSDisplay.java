package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;

public class FPSDisplay extends InterfaceModule {

    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));

    public FPSDisplay() {
        super("FPSDisplay", Category.Interface);
        addSettings(textColor);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, rounded, roundRadius);
    }
}
