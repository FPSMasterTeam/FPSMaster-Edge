package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;

public class HideIndicator extends InterfaceModule {
    public static boolean using = false;

    public HideIndicator() {
        super("HideIndicator", Category.Interface);
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


