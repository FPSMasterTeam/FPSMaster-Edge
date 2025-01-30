package top.fpsmaster.features.command;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.command.impl.Dev;
import top.fpsmaster.features.command.impl.IRCChat;
import top.fpsmaster.features.impl.utility.ClientCommand;
import top.fpsmaster.utils.Utility;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public void init() {
        // add commands
        commands.add(new Dev());
        commands.add(new IRCChat());
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventSendChatMessage e) {
        if (ClientCommand.using && e.msg.startsWith(ClientCommand.prefix.getValue())) {
            e.cancel();
            try {
                runCommand(e.msg.substring(1));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void runCommand(String command) {
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
