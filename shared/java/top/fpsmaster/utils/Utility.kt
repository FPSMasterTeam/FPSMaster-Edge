package top.fpsmaster.utils

import net.minecraft.client.Minecraft
import top.fpsmaster.interfaces.ProviderManager

open class Utility {
    companion object {
        @JvmField
        var mc: Minecraft = Minecraft.getMinecraft()

        @JvmStatic
        fun sendClientMessage(msg: String?) {
            if (ProviderManager.mcProvider.getWorld() != null) {
                ProviderManager.mcProvider.printChatMessage(ProviderManager.utilityProvider.makeChatComponent(msg))
            }
        }

        @JvmStatic
        fun sendClientNotify(msg: String?) {
            if (ProviderManager.mcProvider.getWorld() != null) {
                ProviderManager.mcProvider.printChatMessage("§9[FPSMaster]§r " + ProviderManager.utilityProvider.makeChatComponent(msg))
            }
        }
    }
}
