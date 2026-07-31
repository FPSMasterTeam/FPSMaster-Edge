package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.TextSetting;

import java.awt.Color;

public class ClockDisplay extends InterfaceModule {
    public final BooleanSetting showSeconds = new BooleanSetting("ShowSeconds", true);
    public final BooleanSetting hour24Mode = new BooleanSetting("Hour24", true);
    public final TextSetting label = new TextSetting("Label", "");
    public final ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255, 255));

    public ClockDisplay() {
        super("ClockDisplay", Category.Interface);
        backgroundColor.setColor(new Color(18, 20, 26, 160));
        addSettings(showSeconds, hour24Mode, label, textColor);
    }
}
