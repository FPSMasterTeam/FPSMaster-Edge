package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.*;

public class Keystrokes extends InterfaceModule {
    public static ColorSetting pressedColor = new ColorSetting("PressedColor", new Color(255, 255, 255, 120));

    public Keystrokes() {
        super("Keystrokes", Category.Interface);
        addSettings(fontShadow, betterFont, pressedColor, spacing, bg, backgroundColor, rounded, roundRadius);
    }
}