package top.fpsmaster.features.command;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.utils.core.Utility;

import java.util.ArrayList;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public void init() {
        // add commands
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventSendChatMessage e) throws Exception {
        if (ClientSettings.clientCommand.getValue() && e.msg.startsWith(ClientSettings.prefix.getValue())) {
            e.cancel();
            mc.ingameGUI.getChatGUI().addToSentMessages(e.msg);
            runCommand(e.msg.substring(1));

        }
    }

    private void runCommand(String command) throws Exception {
        String[] args = command.split(" ");
        String cmd = args[0];
        if (args.length == 1) {
            for (Command commandItem : commands) {
                if (commandItem.name.equals(cmd)) {
                    commandItem.execute(new String[]{});
                    return;
                }
            }
            Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
            return;
        }
        String[] cmdArgs = new String[args.length - 1];
        System.arraycopy(args, 1, cmdArgs, 0, cmdArgs.length);
        for (Command commandItem : commands) {
            if (commandItem.name.equals(cmd)) {
                commandItem.execute(cmdArgs);
                return;
            }
        }
        Utility.sendClientMessage(FPSMaster.i18n.get("command.notfound"));
    }
}



