package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class CustomFOV extends Module {

    public static boolean using = false;
    public static final BooleanSetting noSpeedFov = new BooleanSetting("NoSpeedFov", false);
    public static final BooleanSetting noFlyFov = new BooleanSetting("NoFlyFov", false);
    public static final BooleanSetting noBowFov = new BooleanSetting("NoBowFov", false);

    public CustomFOV() {
        super("CustomFov", Category.Utility);
        addSettings(noSpeedFov, noFlyFov, noBowFov);
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

    public static boolean isUsing() {
        return using;
    }
}
