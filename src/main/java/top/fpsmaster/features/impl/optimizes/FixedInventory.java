package top.fpsmaster.features.impl.optimizes;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

public class FixedInventory extends Module {

    public static boolean using = false;

    public FixedInventory() {
        super("FixedInventory", Category.OPTIMIZE);
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
        FixedInventory.using = using;
    }
}



