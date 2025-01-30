package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.Color;

public class ModsList extends InterfaceModule {

    private BooleanSetting showLogo;
    private BooleanSetting english;
    private BooleanSetting rainbow;
    private ColorSetting color;

    public ModsList() {
        super("ModsList", Category.Interface);
        showLogo = new BooleanSetting("ShowLogo", true);
        english = new BooleanSetting("English", true);
        rainbow = new BooleanSetting("Rainbow", true);
        color = new ColorSetting("Color", new Color(255, 255, 255), () -> !rainbow.value);

        addSettings(showLogo, english, color, rainbow, betterFont, backgroundColor, bg);
    }

    public BooleanSetting getShowLogo() {
        return showLogo;
    }

    public BooleanSetting getEnglish() {
        return english;
    }

    public BooleanSetting getRainbow() {
        return rainbow;
    }

    public ColorSetting getColor() {
        return color;
    }
}
