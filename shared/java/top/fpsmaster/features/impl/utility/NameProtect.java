package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.api.ProviderManager;

public class NameProtect extends Module {
    private static boolean using = false;
    private static String playerName = "";
    private static String replacement = "";
    public static TextSetting name = new TextSetting("Name", "Hide");

    public NameProtect() {
        super("NameProtect", Category.Utility);
        addSettings(name);
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

    @Subscribe
    public void onTick(EventTick e) {
        if (ProviderManager.mcProvider.getPlayer() != null) {
            playerName = ProviderManager.mcProvider.getPlayer().getName();
            replacement = name.getValue().replace("&", "§");
        }
    }

    public static String filter(String s) {
        if (using && ProviderManager.mcProvider.getPlayer() != null) {
            if (playerName == null) return s;
            if (replacement == null) return s;
            return s.replaceAll(playerName, replacement);
        } else {
            return s;
        }
    }
}
