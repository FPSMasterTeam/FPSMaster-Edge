package top.fpsmaster.features.impl.utility

import top.fpsmaster.FPSMaster
import top.fpsmaster.event.Subscribe
import top.fpsmaster.event.events.EventTick
import top.fpsmaster.features.manager.Category
import top.fpsmaster.features.manager.Module
import top.fpsmaster.features.settings.impl.TextSetting
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.modules.account.AccountManager.skin

class SkinChanger : Module("SkinChanger", Category.Utility) {
    private var skinName = TextSetting("Skin", "")
    private var updateThread = Thread {
        while (true) {
            update()
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }

    init {
        addSettings(skinName)
    }

    override fun onEnable() {
        super.onEnable()
        using = true
        if (ProviderManager.mcProvider.getPlayer() != null) {
            if (!updateThread.isAlive) {
                updateThread = Thread { update() }
                updateThread.start()
            }
        }
    }

    @Subscribe
    fun onTick(e: EventTick?) {
        if (ProviderManager.mcProvider.getPlayer() != null && ProviderManager.mcProvider.getPlayer()!!.ticksExisted % 30 == 0) {
            FPSMaster.async.runnable { update() }
        }
        skin = skinName.value
    }

    fun update() {
        ProviderManager.skinProvider.updateSkin(
            ProviderManager.mcProvider.getPlayer()!!.name,
            ProviderManager.mcProvider.getPlayer()!!.uniqueID.toString(),
            skinName.value
        )
    }

    override fun onDisable() {
        super.onDisable()
        FPSMaster.async.runnable {
            ProviderManager.skinProvider.updateSkin(
                ProviderManager.mcProvider.getPlayer()!!.name,
                ProviderManager.mcProvider.getPlayer()!!.uniqueID.toString(),
                ProviderManager.mcProvider.getPlayer()!!.name
            )
        }
        using = false
    }

    companion object {
        var using = false
    }
}
