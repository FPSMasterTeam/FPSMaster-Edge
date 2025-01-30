package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.Color;

public class MusicOverlay extends InterfaceModule {
    public MusicOverlay() {
        super("MusicDisplay", Category.Interface);
        addSettings(backgroundColor, progressColor, color, amplitude, bg, rounded, roundRadius);
    }

    public static NumberSetting amplitude = new NumberSetting("Amplitude", 10, 0, 10, 0.1);
    public static ColorSetting progressColor = new ColorSetting("ProgressColor", new Color(255, 255, 255, 100));
    public static ColorSetting color = new ColorSetting("Visual", new Color(255, 255, 255, 100));
}
