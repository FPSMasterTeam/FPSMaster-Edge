package top.fpsmaster.features.impl.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class ToggleSneak extends InterfaceModule {
    private final BooleanSetting toggleSneak = new BooleanSetting("ToggleSneak", true);
    private final BindSetting toggleKey = new BindSetting("ToggleKey", Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode());
    public static boolean using = false;
    public static boolean sneak = false;

    public ToggleSneak() {
        super("ToggleSneak", Category.Utility, Trait.TEXT);
        addSettings(toggleSneak, toggleKey);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!toggleSneak.getValue()) {
            sneak = false;
        }
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (toggleSneak.getValue() && e.key == toggleKey.getValue()) {
            sneak = !sneak;
            if (!sneak) {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.setSneaking(false);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode(), false);
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.setSneaking(false);
        }
        using = false;
        sneak = false;
    }
}
