package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;

public class PotionDisplay extends InterfaceModule {
    public static boolean using = false;

    public PotionDisplay() {
        super("PotionDisplay", Category.Interface);
        addSettings(backgroundColor, fontShadow, betterFont, spacing, bg, rounded, roundRadius);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }
}
