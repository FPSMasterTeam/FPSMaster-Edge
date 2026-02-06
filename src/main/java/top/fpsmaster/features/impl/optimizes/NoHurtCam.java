package top.fpsmaster.features.impl.optimizes;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

public class NoHurtCam extends Module {

    public static boolean using = false;

    public NoHurtCam() {
        super("NoHurtCam", Category.OPTIMIZE);
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

    public static void setUsing(boolean using) {
        NoHurtCam.using = using;
    }
}
