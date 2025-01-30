package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import java.awt.Color;

public class Keystrokes extends InterfaceModule {
    public static ColorSetting pressedColor = new ColorSetting("PressedColor", new Color(255, 255, 255, 120));

    public Keystrokes() {
        super("Keystrokes", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, pressedColor, bg, rounded, roundRadius);
    }
}