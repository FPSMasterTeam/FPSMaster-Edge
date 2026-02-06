package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class Scoreboard extends InterfaceModule {
    public static boolean using = false;
    public static BooleanSetting score = new BooleanSetting("Score", false);

    public Scoreboard() {
        super("Scoreboard", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, score, bg, rounded, roundRadius);
    }

    @Override
    public void onEnable() {
        using = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }
}
