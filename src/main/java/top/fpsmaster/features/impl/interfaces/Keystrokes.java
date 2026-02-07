package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.*;

public class Keystrokes extends InterfaceModule {
    public static ColorSetting pressedColor = new ColorSetting("PressedColor", new Color(255, 255, 255, 120));
    public static ColorSetting fontColor = new ColorSetting("FontColor", new Color(255, 255, 255));
    public static ColorSetting pressedFontColor = new ColorSetting("PressedFontColor", new Color(201, 201, 201));
    public static ModeSetting textColorMode = new ModeSetting("TextColorMode", 0, "Static", "Rainbow", "Chroma");
    public static NumberSetting textColorSpeed = new NumberSetting("TextColorSpeed", 1.0, 0.1, 5.0, 0.1, () -> !textColorMode.isMode("Static"));
    public static NumberSetting textColorSaturation = new NumberSetting("TextColorSaturation", 0.7, 0.0, 1.0, 0.05, () -> !textColorMode.isMode("Static"));

    public static NumberSetting borderWidth = new NumberSetting("BorderWidth", 1.0, 0.0, 4.0, 0.5);
    public static ModeSetting borderColorMode = new ModeSetting("BorderColorMode", 0, () -> borderWidth.getValue().floatValue() > 0, "Static", "Rainbow", "Chroma");
    public static ColorSetting borderColor = new ColorSetting(
            "BorderColor",
            new Color(255, 255, 255, 80),
            () -> borderWidth.getValue().floatValue() > 0 && borderColorMode.isMode("Static")
    );
    public static NumberSetting borderColorSpeed = new NumberSetting(
            "BorderColorSpeed",
            1.0,
            0.1,
            5.0,
            0.1,
            () -> borderWidth.getValue().floatValue() > 0 && !borderColorMode.isMode("Static")
    );
    public static NumberSetting borderColorSaturation = new NumberSetting(
            "BorderColorSaturation",
            0.7,
            0.0,
            1.0,
            0.05,
            () -> borderWidth.getValue().floatValue() > 0 && !borderColorMode.isMode("Static")
    );

    public static ModeSetting pressAnimMode = new ModeSetting("PressAnimMode", 0, "Color", "Pulse", "Ripple", "Bloom", "Stack");
    public static ColorSetting pressAnimColor = new ColorSetting("PressAnimColor", new Color(255, 255, 255, 120), () -> !pressAnimMode.isMode("Color"));
    public static NumberSetting pressAnimDuration = new NumberSetting("PressAnimDuration", 0.25, 0.05, 1.0, 0.05, () -> !pressAnimMode.isMode("Color"));

    public static BooleanSetting showSpace = new BooleanSetting("ShowSpace", true);
    public static ModeSetting cpsMode = new ModeSetting("CPSMode", 0, "Below", "ClickOnly", "Off");
    public static ModeSetting wasdStyle = new ModeSetting("WASDStyle", 0, "Text", "Triangle");
    public static ModeSetting spaceStyle = new ModeSetting("SpaceStyle", 0, () -> showSpace.getValue(), "Text", "Bar");

    public Keystrokes() {
        super("Keystrokes", Category.Interface);
        CPSDisplay.ensureTracking();
        addSettings(
                fontShadow, betterFont,
                pressedColor, fontColor, pressedFontColor,
                textColorMode, textColorSpeed, textColorSaturation,
                borderColorMode, borderColor, borderColorSpeed, borderColorSaturation, borderWidth,
                pressAnimMode, pressAnimColor, pressAnimDuration,
                showSpace, cpsMode, wasdStyle, spaceStyle, spacing, bg, backgroundColor, rounded, roundRadius
        );
    }
}


