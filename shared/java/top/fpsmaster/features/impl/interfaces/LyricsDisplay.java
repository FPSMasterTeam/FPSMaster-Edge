package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;

public class LyricsDisplay extends InterfaceModule {
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));
    public static ColorSetting textBG = new ColorSetting("TextColorBG", new Color(255, 255, 255));

    public LyricsDisplay() {
        super("LyricsDisplay", Category.Interface);
        addSettings(backgroundColor, rounded, betterFont, textColor, textBG, bg, rounded, roundRadius);
    }
}