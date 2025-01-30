package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;

public class LevelTag extends Module {

    public static boolean using = false;
    public static final BooleanSetting showSelf = new BooleanSetting("ShowSelf", true);
    public static final BooleanSetting health = new BooleanSetting("Health", true);
    public static final ModeSetting levelMode = new ModeSetting("RankMode", 0, "None", "Bedwars", "Bedwars-xp", "Skywars", "Kit");

    public LevelTag() {
        super("Nametags", Category.Utility);
        addSettings(showSelf, health, levelMode);
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
