package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.TextSetting;

import java.awt.Color;

public class ServerAddressDisplay extends InterfaceModule {
    public final TextSetting label = new TextSetting("Label", "");
    public final ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255, 255));

    public ServerAddressDisplay() {
        super("ServerAddressDisplay", Category.Interface);
        backgroundColor.setColor(new Color(18, 20, 26, 160));
        addSettings(label, textColor, betterFont, fontShadow, bg, rounded, roundRadius, backgroundColor);
    }
}
