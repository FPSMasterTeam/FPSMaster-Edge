package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;

public class PingDisplay extends InterfaceModule {
    public PingDisplay() {
        super("PingDisplay", Category.Interface);
        addSettings(textColor);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, rounded, roundRadius);
    }

    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));
}
