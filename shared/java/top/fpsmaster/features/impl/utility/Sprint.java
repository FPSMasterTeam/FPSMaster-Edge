package top.fpsmaster.features.impl.utility;

import net.minecraft.potion.Potion;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.wrapper.MinecraftProvider;

import static top.fpsmaster.utils.Utility.mc;

public class Sprint extends InterfaceModule {

    BooleanSetting toggleSprint = new BooleanSetting("ToggleSprint", true);

    public Sprint() {
        super("Sprint", Category.Utility);
        addSettings(toggleSprint, betterFont);
    }

    public static boolean sprint = true;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (sprint || !toggleSprint.getValue()) {
            if (mc.thePlayer.moveForward <= 0)
                return;
            if (mc.thePlayer.isCollidedHorizontally)
                return;
            if (mc.thePlayer.isPotionActive(Potion.blindness))
                return;
            if (mc.thePlayer.getFoodStats().getFoodLevel() < 6f)
                return;
            if (mc.thePlayer.isUsingItem())
                return;
            mc.thePlayer.setSprinting(true);
        }
    }

    @Subscribe
    public void onKey(EventKey e){
        if (e.key == mc.gameSettings.keyBindSprint.getKeyCode()) {
            sprint = !sprint;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ProviderManager.gameSettings.setKeyPress(mc.gameSettings.keyBindSprint, false);
    }
}