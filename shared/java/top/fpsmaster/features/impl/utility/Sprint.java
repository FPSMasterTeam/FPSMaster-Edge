package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.Utility;

public class Sprint extends Module {

    public Sprint() {
        super("Sprint", Category.Utility);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        ProviderManager.gameSettings.setKeyPress(Utility.mc.gameSettings.keyBindSprint, true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ProviderManager.gameSettings.setKeyPress(Utility.mc.gameSettings.keyBindSprint, false);
    }
}