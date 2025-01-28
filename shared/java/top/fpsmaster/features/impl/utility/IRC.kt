package top.fpsmaster.features.impl.utility

import top.fpsmaster.FPSMaster
import top.fpsmaster.event.Subscribe
import top.fpsmaster.event.events.EventTick
import top.fpsmaster.features.manager.Category
import top.fpsmaster.features.manager.Module
import top.fpsmaster.features.settings.impl.BooleanSetting
import top.fpsmaster.utils.Utility
import top.fpsmaster.utils.math.MathTimer
import top.fpsmaster.websocket.client.WsClient
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.modules.dev.DevMode

class IRC : Module("IRC", Category.Utility) {
    init {
        addSettings(showMates)
    }

    override fun onEnable() {
        super.onEnable()
        using = true
    }

    private val timer = MathTimer()

    @Subscribe
    fun onTick(e: EventTick) {
        if (!timer.delay(5000))
            return
        if (ProviderManager.mcProvider.getWorld() == null)
            return
        if (FPSMaster.INSTANCE.wsClient == null) {
            FPSMaster.INSTANCE.wsClient = WsClient.start("wss://service.fpsmaster.top/")
            if (DevMode.INSTACE.dev)
                Utility.sendClientMessage("尝试连接")
        }else if (FPSMaster.INSTANCE.wsClient!!.isClosed && !FPSMaster.INSTANCE.wsClient!!.isOpen){
            FPSMaster.INSTANCE.wsClient!!.connect()
            if (DevMode.INSTACE.dev)
                Utility.sendClientMessage("尝试连接")
        }
    }

    override fun onDisable() {
        super.onDisable()
        if (FPSMaster.INSTANCE.wsClient != null && FPSMaster.INSTANCE.wsClient!!.isOpen) {
            FPSMaster.INSTANCE.wsClient!!.close()
        }
        using = false
    }

    companion object {
        var using: Boolean = false
        val showMates = BooleanSetting("showMates", true)

    }
}