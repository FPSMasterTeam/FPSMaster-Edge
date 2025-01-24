package top.fpsmaster.features.manager

import net.minecraft.client.Minecraft
import top.fpsmaster.FPSMaster
import top.fpsmaster.event.EventDispatcher.registerListener
import top.fpsmaster.event.EventDispatcher.unregisterListener
import top.fpsmaster.features.settings.Setting
import top.fpsmaster.features.settings.impl.*
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.ui.notification.NotificationManager
import java.util.*

open class Module {
    var name: String
    private var description: String = ""
    var category: Category
    var settings = LinkedList<Setting<*>>()
    var isEnabled = false
    var key = 0

    constructor(name: String, description: String, category: Category) {
        this.name = name
        this.description = description
        this.category = category
    }

    constructor(name: String, category: Category) {
        this.name = name
        this.category = category
    }

    fun addSettings(vararg settings: Setting<*>?) {
        for (setting in settings) {
            if (setting != null && setting is BooleanSetting) {
                this.settings.add(setting)
            }
        }
        for (setting in settings) {
            if (setting != null && setting is BindSetting) {
                this.settings.add(setting)
            }
        }
        for (setting in settings) {
            if (setting != null && setting is ModeSetting) {
                this.settings.add(setting)
            }
        }
        for (setting in settings) {
            if (setting != null && setting is NumberSetting) {
                this.settings.add(setting)
            }
        }
        for (setting in settings) {
            if (setting != null && setting is TextSetting) {
                this.settings.add(setting)
            }
        }
        for (setting in settings) {
            if (setting != null && setting is ColorSetting) {
                this.settings.add(setting)
            }
        }
    }

    fun setState(state: Boolean) {
        isEnabled = state
    }

    fun toggle() {
        set(!isEnabled)
    }

    fun set(state: Boolean) {
        isEnabled = state
        try {
            if (state) {
                onEnable()
                if (ProviderManager.mcProvider.getPlayer() != null)
                    NotificationManager.addNotification(
                        FPSMaster.i18n["notification.module.enable"],
                        String.format(
                            FPSMaster.i18n["notification.module.enable.desc"],
                            FPSMaster.i18n[this.name.lowercase(Locale.getDefault())]
                        ),
                        2f
                    )
            } else {
                onDisable()
                if (ProviderManager.mcProvider.getPlayer() != null)
                    NotificationManager.addNotification(
                        FPSMaster.i18n["notification.module.disable"],
                        String.format(
                            FPSMaster.i18n["notification.module.disable.desc"],
                            FPSMaster.i18n[this.name.lowercase(Locale.getDefault())]
                        ),
                        2f
                    )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun onEnable() {
        registerListener(this)
    }

    open fun onDisable() {
        unregisterListener(this)
    }

    companion object {
        var mc: Minecraft = Minecraft.getMinecraft()
    }
}
