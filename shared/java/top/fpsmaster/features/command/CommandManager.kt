package top.fpsmaster.features.command

import top.fpsmaster.FPSMaster
import top.fpsmaster.event.EventDispatcher
import top.fpsmaster.event.Subscribe
import top.fpsmaster.event.events.EventSendChatMessage
import top.fpsmaster.features.command.impl.chat.IRCChat
import top.fpsmaster.features.command.impl.dev.Dev
import top.fpsmaster.features.impl.utility.ClientCommand
import top.fpsmaster.utils.Utility

class CommandManager {
    private val commands = mutableListOf<Command>()

    fun init() {
        // add commands
        commands.add(Dev())
        commands.add(IRCChat())
        EventDispatcher.registerListener(this)
    }

    @Subscribe
    fun onChat(e: EventSendChatMessage){
        if(ClientCommand.using && e.msg.startsWith(ClientCommand.prefix.value)){
            e.cancel()
            try {
                runCommand(e.msg.substring(1))
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    private fun runCommand(command: String){
        val args = command.split(" ")
        val cmd = args[0]
        if (args.size == 1){
            commands.forEach {
                if(it.name == cmd){
                    it.execute(arrayOf())
                    return
                }
            }
            Utility.sendClientMessage(FPSMaster.i18n["command.notfound"])
            return
        }
        val cmdArgs = args.subList(1, args.size).toTypedArray()
        commands.forEach {
            if(it.name == cmd){
                it.execute(cmdArgs)
                return
            }
        }
        Utility.sendClientMessage(FPSMaster.i18n["command.notfound"])
    }
}