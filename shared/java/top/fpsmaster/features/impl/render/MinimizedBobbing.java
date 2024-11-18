package top.fpsmaster.features.impl.render;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

public class MinimizedBobbing extends Module {

    public static boolean using = false;

    public MinimizedBobbing() {
        super("MinimizedBobbing", Category.RENDER);
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
