package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;

public class Keystrokes extends InterfaceModule {
    public static ColorSetting pressedColor = new ColorSetting("PressedColor", new Color(255, 255, 255, 120));
    public static ColorSetting fontColor = new ColorSetting("FontColor", new Color(255, 255, 255));
    public static ColorSetting pressedFontColor = new ColorSetting("PressedFontColor", new Color(201, 201, 201));

    public Keystrokes() {
        super("Keystrokes", Category.Interface);
        addSettings(fontShadow, betterFont, pressedColor, fontColor, pressedFontColor, spacing, bg, backgroundColor, rounded, roundRadius);
    }
}