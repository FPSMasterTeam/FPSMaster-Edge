package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.TextSetting;

import java.awt.*;

public class ModsList extends InterfaceModule {

    public BooleanSetting showLogo = new BooleanSetting("ShowText", true);
    public BooleanSetting english = new BooleanSetting("English", true);
    public BooleanSetting rainbow = new BooleanSetting("Rainbow", true);
    public ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255), () -> !rainbow.getValue());
    public TextSetting text = new TextSetting("Text", "FPSMaster", () -> showLogo.getValue());

    public ModsList() {
        super("ModsList", Category.Interface);
        addSettings(showLogo, text, english, color, rainbow, betterFont, spacing, backgroundColor, bg);
    }
}
