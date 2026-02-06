package top.fpsmaster.features.impl.optimizes;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class BetterFishingRod extends Module {
    public static boolean using;
    public static final NumberSetting stringWidth = new NumberSetting("StringWidth", 1.0, 1.0, 10.0, 0.5);

    public BetterFishingRod() {
        super("BetterFishingRod", Category.OPTIMIZE);
        addSettings(stringWidth);
    }

    @Override
    public void onEnable() {
        using = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        using = false;
        super.onDisable();
    }
}
