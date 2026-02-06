package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class TabOverlay extends InterfaceModule {
    public static boolean using = false;
    public static BooleanSetting showPing = new BooleanSetting("ShowPing", true);

    public TabOverlay() {
        super("TabOverlay", Category.Interface);
        addSettings(showPing);
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



