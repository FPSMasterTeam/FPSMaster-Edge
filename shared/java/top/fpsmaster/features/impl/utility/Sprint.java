package top.fpsmaster.features.impl.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class Sprint extends InterfaceModule {
    private final BooleanSetting toggleSprint = new BooleanSetting("ToggleSprint", true);
    public static boolean using = false;
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
        if (toggleSprint.getValue() && e.key == Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode()) {
            sprint = !sprint;
            if (!sprint) {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.setSprinting(false);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode(), false);
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.setSprinting(false);
        }
        using = false;
    }
}
