package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class BetterScreen extends InterfaceModule {

    public static boolean using = false;
    public static BooleanSetting useBG = new BooleanSetting("Background", true);
    public static BooleanSetting backgroundAnimation = new BooleanSetting("BackgroundAnimation", true);
    public static BooleanSetting noFlickering = new BooleanSetting("noFlickering", true);

    public BetterScreen() {
        super("BetterScreen", Category.Interface);
        addSettings(useBG, backgroundAnimation, noFlickering);
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
