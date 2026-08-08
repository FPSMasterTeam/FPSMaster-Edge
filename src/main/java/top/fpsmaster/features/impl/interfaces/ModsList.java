package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;

import java.awt.*;

public class ModsList extends InterfaceModule {
    public BooleanSetting showLogo = new BooleanSetting("ShowText", true);
    public BooleanSetting english = new BooleanSetting("English", true);
    public ColorSetting color = new ColorSetting("Color", new CustomColor(0f, 0.7f, 1f, 1f),
            ColorSetting.ColorType.STATIC, ColorSetting.ColorType.WAVE, ColorSetting.ColorType.WAVE_BRIGHTNESS,
            ColorSetting.ColorType.CHROMA, ColorSetting.ColorType.RAINBOW);
    public TextSetting text = new TextSetting("Text", "FPSMaster", () -> showLogo.getValue());
    public NumberSetting titleSize = new NumberSetting("TitleSize", 36, 10, 100, 1, () -> showLogo.getValue());
    public ColorSetting titleColor = new ColorSetting("TitleColor", new Color(113, 127, 254), () -> showLogo.getValue(),
            ColorSetting.ColorType.STATIC, ColorSetting.ColorType.WAVE, ColorSetting.ColorType.WAVE_BRIGHTNESS,
            ColorSetting.ColorType.CHROMA, ColorSetting.ColorType.RAINBOW);
    public ModeSetting bgStyle = new ModeSetting("BackgroundStyle", 0, () -> bg.getValue(), "Solid", "Bar");
    public NumberSetting barWidth = new NumberSetting("BarWidth", 3, 1, 10, 1, () -> bg.getValue() && bgStyle.isMode("Bar"));
    public ColorSetting barColor = new ColorSetting("BarColor", new Color(255, 255, 255, 120), () -> bg.getValue() && bgStyle.isMode("Bar"));
    public ModeSetting animation = new ModeSetting("Animation", 0, "Fade", "Slide", "Zoom", "None");
    public NumberSetting animationSpeed = new NumberSetting("AnimationSpeed", 8, 2, 50, 1, () -> !animation.isMode("None"));

    public ModsList() {
        super("ModsList", Category.Interface);
        color.setColorType(ColorSetting.ColorType.RAINBOW);
        addSettings(showLogo, text, english, color, titleSize, titleColor, bgStyle, barColor, barWidth, animation, animationSpeed);
    }

    /** The list background is always a plain rectangle; the shared rounded settings do not apply. */
    @Override
    public void registerCommonSettings() {
        if (has(Trait.BACKGROUND)) {
            addSettings(bg, backgroundColor);
        }
        if (has(Trait.TEXT)) {
            addSettings(betterFont, fontShadow);
        }
        if (has(Trait.SPACING)) {
            addSettings(spacing);
        }
    }
}
