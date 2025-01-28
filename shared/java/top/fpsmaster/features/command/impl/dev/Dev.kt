package top.fpsmaster.features.command.impl.dev

import top.fpsmaster.FPSMaster
import top.fpsmaster.features.command.Command
import top.fpsmaster.modules.dev.DevMode
import top.fpsmaster.utils.Utility

class Dev : Command("dev") {
    override fun execute(args: Array<String>) {
        if (args.isEmpty()) {
            DevMode.INSTACE.setDev(!DevMode.INSTACE.dev)
            Utility.sendClientNotify("Dev mode is now ${if (DevMode.INSTACE.dev) "enabled" else "disabled"}")
        } else if (args.size == 1 && DevMode.INSTACE.dev) {
            if (args[0] == "hotswap") {
                DevMode.INSTACE.setHotswap(!DevMode.INSTACE.hotswap)
                Utility.sendClientNotify("Hotswap is now ${if (DevMode.INSTACE.hotswap) "enabled" else "disabled"}")
            }
        }
    }
}