package top.fpsmaster.features.impl.utility;

import net.minecraft.util.StringUtils;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.notification.Notification;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.Utility;

public class AutoGG extends Module {
    public BooleanSetting autoPlay = new BooleanSetting("AutoPlay", false);
    public TextSetting message = new TextSetting("Message", "gg");
    public ModeSetting servers = new ModeSetting("Servers", 0, "hypxiel");

    public AutoGG() {
        super("AutoGG", Category.Utility);
        this.addSettings(autoPlay, message, servers);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.type == EventPacket.PacketType.RECEIVE && ProviderManager.packetChat.isPacket(event.packet)) {
            switch (servers.getValue()) {
                case 0:
                    String componentValue = ProviderManager.packetChat.getChatComponent(event.packet).toString();
                    boolean hasPlayCommand = componentValue.contains("ClickEvent{action=RUN_COMMAND, value='/play ");
                    String chatMessage = ProviderManager.packetChat.getUnformattedText(event.packet);
                    boolean hasEndInformation = StringUtils.stripControlCodes(chatMessage).contains("                               胜利者  ") || StringUtils.stripControlCodes(chatMessage).startsWith("                              Winner ");
                    if (hasEndInformation) {
                        Utility.sendChatMessage("/ac " + message.getValue());
                    }
                    if (hasPlayCommand) {
                        if (autoPlay.getValue()) {
                            Utility.sendChatMessage(componentValue.substring(componentValue.indexOf("value='") + 7, componentValue.indexOf("'}")));
                        }
                    }
                    break;
                default:

            }
        }
    }
}
