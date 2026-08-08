package top.fpsmaster.features.manager;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.notification.NotificationManager;

import java.util.LinkedList;
import java.util.Locale;

public class Module {

    public String name;
    public String description = "";
    public Category category;
    public LinkedList<Setting<?>> settings = new LinkedList<>();
    public int key = 0;

    @Getter
    private boolean isEnabled = false;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    /**
     * Registration is idempotent by identity. Several modules used to pass the same setting twice,
     * which rendered a duplicate row in the ClickGUI bound to the same underlying value.
     */
    public void addSettings(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            if (setting != null && !containsSetting(setting)) {
                this.settings.add(setting);
            }
        }
    }

    private boolean containsSetting(Setting<?> setting) {
        for (Setting<?> existing : this.settings) {
            if (existing == setting) {
                return true;
            }
        }
        return false;
    }

    public void toggle() {
        if (name.equals("ClientSettings")) return;
        set(!isEnabled);
    }

    public void set(boolean state) {
        try {
            if (state && !isEnabled) {
                isEnabled = true;
                onEnable();
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    NotificationManager.addNotification(
                            FPSMaster.i18n.get("notification.module.enable"),
                            String.format(
                                    FPSMaster.i18n.get("notification.module.enable.desc"),
                                    FPSMaster.i18n.get(this.name.toLowerCase(Locale.getDefault()))
                            ),
                            2f
                    );
                }
            } else if (!state && isEnabled){
                isEnabled = false;
                onDisable();
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    NotificationManager.addNotification(
                            FPSMaster.i18n.get("notification.module.disable"),
                            String.format(
                                    FPSMaster.i18n.get("notification.module.disable.desc"),
                                    FPSMaster.i18n.get(this.name.toLowerCase(Locale.getDefault()))
                            ),
                            2f
                    );
                }
            }
        } catch (Exception e) {
            ClientLogger.error("An error occurred while toggling module: " + this.name);
        }
    }

    public void onEnable() {
        EventDispatcher.registerListener(this);
    }

    public void onDisable() {
        EventDispatcher.unregisterListener(this);
    }

}



