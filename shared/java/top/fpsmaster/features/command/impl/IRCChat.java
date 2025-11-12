package top.fpsmaster.features.command.impl;

import net.minecraft.client.network.NetworkPlayerInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.command.Command;
import top.fpsmaster.features.impl.utility.IRC;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.utils.Utility;

import static top.fpsmaster.utils.Utility.mc;

public class IRCChat extends Command {

    public IRCChat() {
        super("irc");
    }

    @Override
    public void execute(String[] args) {
        if (!IRC.using) {
            Utility.sendClientNotify("IRC is not using");
            return;
        }
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
            } else if ("update".equals(args[0])) {
                FPSMaster.INSTANCE.wsClient.sendInformation(AccountManager.skin, AccountManager.cosmeticsUsing, Wrappers.minecraft().getPlayer().getName(), Wrappers.minecraft().getServerAddress());
            } else if ("fetch".equals(args[0])) {
                for (NetworkPlayerInfo networkPlayerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                    FPSMaster.INSTANCE.wsClient.fetchPlayer(networkPlayerInfo.getGameProfile().getId().toString(), networkPlayerInfo.getGameProfile().getName());
                }
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
