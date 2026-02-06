package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class ParticlesModifier extends Module {
    public static BooleanSetting block = new BooleanSetting("Block", true);
    public static boolean using;

    public ParticlesModifier() {
        super("ParticlesModifier", Category.Utility);
        addSettings(block);
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



