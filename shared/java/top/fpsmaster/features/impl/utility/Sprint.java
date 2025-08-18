package top.fpsmaster.features.impl.utility;

import top.fpsmaster.api.provider.ProviderRegistry;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.interfaces.ProviderManager;

import static top.fpsmaster.utils.Utility.mc;

public class Sprint extends InterfaceModule {
    private final BooleanSetting toggleSprint = new BooleanSetting("ToggleSprint", true);
    public static boolean using = true;
    public static boolean sprint = true;

    public Sprint() {
        super("Sprint", Category.Utility);
        addSettings(toggleSprint, betterFont);
    }


    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!toggleSprint.getValue()) {
            sprint = true;
        }
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (toggleSprint.getValue() && e.key == mc.gameSettings.keyBindSprint.getKeyCode()) {
            sprint = !sprint;
            if (!sprint)
                ProviderRegistry.getMinecraftProvider().getMinecraft().getClientPlayer().setSprinting(false);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ProviderManager.gameSettings.setKeyPress(mc.gameSettings.keyBindSprint, false);
        mc.thePlayer.setSprinting(false);
        using = false;
    }
}