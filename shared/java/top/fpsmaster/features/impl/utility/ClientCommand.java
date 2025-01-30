package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.TextSetting;

public class ClientCommand extends Module {

    public static boolean using = false;
    public static final TextSetting prefix = new TextSetting("prefix", "#");

    public ClientCommand() {
        super("ClientCommand", Category.Utility);
        addSettings(prefix);
    }

    @Override
    public void onEnable() {
        using = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        using = false;
        super.onDisable();
    }
}
