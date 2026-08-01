package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;

/**
 * Replaces the vanilla food HUD with a movable saturation display.
 */
public class SaturationDisplay extends InterfaceModule {
    public static boolean using;

    public SaturationDisplay() {
        super("SaturationDisplay", Category.Interface, Trait.BACKGROUND, Trait.TEXT);
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
