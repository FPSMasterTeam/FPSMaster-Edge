package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class CustomTitles extends Module {

    static NumberSetting x = new NumberSetting("x", 0, -500, 500, 1);
    static NumberSetting y = new NumberSetting("y", 0, -500, 500, 1);
    static NumberSetting scale = new NumberSetting("scale", 1, 0, 3, 0.02);

    static boolean using;

    public CustomTitles() {
        super("CustomTitles", Category.Interface);
        addSettings(x, y, scale);
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

    public static int getX() {
        if (!using)
            return 0;
        return x.getValue().intValue();
    }
    public static int getY() {
        if (!using)
            return 0;
        return y.getValue().intValue();
    }
    public static float getScale() {
        if (!using)
            return 1;
        return scale.getValue().floatValue();
    }
}
