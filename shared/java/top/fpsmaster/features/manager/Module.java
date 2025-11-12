package top.fpsmaster.features.manager;

import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.*;
import top.fpsmaster.api.Wrappers;
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

    public void addSettings(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            if (setting != null) {
                if (setting instanceof BooleanSetting) {
                    this.settings.add(setting);
                } else if (setting instanceof BindSetting) {
                    this.settings.add(setting);
                } else if (setting instanceof ModeSetting) {
                    this.settings.add(setting);
                } else if (setting instanceof NumberSetting) {
                    this.settings.add(setting);
                } else if (setting instanceof TextSetting) {
                    this.settings.add(setting);
                } else if (setting instanceof ColorSetting) {
                    this.settings.add(setting);
                } else if (setting instanceof MultipleItemSetting) {
                    this.settings.add(setting);
                }
            }
        }
    }

    public void toggle() {
        set(!isEnabled);
    }

    public void set(boolean state) {
        try {
            if (state && !isEnabled) {
                isEnabled = true;
                onEnable();
                if (Minecraft.getMinecraft() != null && Wrappers.minecraft().getPlayer() != null) {
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
                if (Minecraft.getMinecraft() != null && Wrappers.minecraft().getPlayer() != null) {
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
            e.printStackTrace();
        }
    }

    public void onEnable() {
        EventDispatcher.registerListener(this);
    }

    public void onDisable() {
        EventDispatcher.unregisterListener(this);
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
