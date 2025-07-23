package top.fpsmaster.features.impl.utility;

import net.minecraft.util.StringUtils;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.notification.Notification;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.Utility;

public class AutoGG extends Module {
    public BooleanSetting autoPlay = new BooleanSetting("AutoPlay", false);
    public NumberSetting delay = new NumberSetting("DelayToPlay", 5, 0, 10, 1, () -> autoPlay.getValue());
    public TextSetting message = new TextSetting("Message", "gg");
    public ModeSetting servers = new ModeSetting("Servers", 0, "hypxiel", "kkcraft");

    public String hypixelTrigger = "Reward Summary;1st Killer;Damage Dealt;奖励总览;击杀数第一名;造成伤害";
    public String kkcraftTrigger = "获胜者;第一名杀手;击杀第一名";
    public AutoGG() {
        super("AutoGG", Category.Utility);
        this.addSettings(autoPlay, delay, message, servers);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.type == EventPacket.PacketType.RECEIVE && ProviderManager.packetChat.isPacket(event.packet)) {
            String componentValue = ProviderManager.packetChat.getChatComponent(event.packet).toString();
            String chatMessage = ProviderManager.packetChat.getUnformattedText(event.packet);
            boolean hasEndInformation = false;
            Utility.sendClientMessage(componentValue);
            switch (servers.getValue()) {
                case 0:
                    boolean hasPlayCommand = componentValue.contains("ClickEvent{action=RUN_COMMAND, value='/play ");
                    for (String s : hypixelTrigger.split(";")) {
                        hasEndInformation = StringUtils.stripControlCodes(chatMessage).contains(s);
                        if (hasEndInformation) break;
                    }
                    if (hasEndInformation) {
                        Utility.sendChatMessage("/ac " + message.getValue());
                    }
                    if (hasPlayCommand) {
                        if (autoPlay.getValue()) {
                            FPSMaster.async.runnable(() -> {
                                Utility.sendClientNotify("Sending you to the next game in " + delay.getValue() + " seconds");
                                try {
                                    Thread.sleep(delay.getValue().longValue() * 1000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                Utility.sendChatMessage(componentValue.substring(componentValue.indexOf("value='") + 7, componentValue.indexOf("'}")));
                            });
                        }
                    }
                    break;
                case 1:
                    for (String s : kkcraftTrigger.split(";")) {
                        hasEndInformation = StringUtils.stripControlCodes(chatMessage).contains(s);
                        if (hasEndInformation) break;
                    }
                    if (hasEndInformation) {
                        Utility.sendChatMessage(message.getValue());
                    }
                    if(autoPlay.getValue()) {
                        Utility.sendClientNotify("AutoPlay is not supported at the moment in KKCraft");
                    }
                    break;
                default:

            }
        }
    }
}
