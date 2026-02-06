package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class SoundModifier extends Module {
    public static boolean using;
    public static NumberSetting liquid = new NumberSetting("Liquid", 1, 0, 1, 0.01);
    public static NumberSetting dig = new NumberSetting("Dig", 1, 0, 1, 0.01);
    public static NumberSetting game = new NumberSetting("Game", 1, 0, 1, 0.01);
    public static NumberSetting mob = new NumberSetting("Mob", 1, 0, 1, 0.01);

    public SoundModifier() {
        super("SoundModifier", Category.Utility);
        addSettings(liquid, dig, game, mob);
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



