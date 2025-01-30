package top.fpsmaster.features.command.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.command.Command;
import top.fpsmaster.utils.Utility;

public class IRCChat extends Command {

    public IRCChat() {
        super("irc");
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();

            if ("cmd".equals(args[0])) {
                for (int i = 1; i < args.length; i++) {
                    if (i == args.length - 1) {
                        sb.append(args[i]);
                    } else {
                        sb.append(args[i]).append(" ");
                    }
                }
                String message = sb.toString();
                FPSMaster.INSTANCE.wsClient.sendCommand(message);
            } else if ("dm".equals(args[0])) {
                for (int i = 2; i < args.length; i++) {
                    if (i == args.length - 1) {
                        sb.append(args[i]);
                    } else {
                        sb.append(args[i]).append(" ");
                    }
                }
                String message = sb.toString();
                FPSMaster.INSTANCE.wsClient.sendDM(args[1], message);
            } else {
                for (String arg : args) {
                    if (arg.equals(args[args.length - 1])) {
                        sb.append(arg);
                    } else {
                        sb.append(arg).append(" ");
                    }
                }
                String message = sb.toString();
                FPSMaster.INSTANCE.wsClient.sendMessage(message);
            }
        } else {
            Utility.sendClientMessage("Usage: #irc <message>");
        }
    }
}
