package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.logger.ClientLogger;
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
                    ClientLogger.info(componentValue);
                    boolean hasPlayCommand = componentValue.contains("ClickEvent{action=RUN_COMMAND, value='/play ");
                    if (hasPlayCommand) {
                        Utility.sendChatMessage("/ac " + message.getValue());
                        if (autoPlay.getValue()){
                            Utility.sendChatMessage(componentValue.substring(componentValue.indexOf("value='/play ") + 11, componentValue.indexOf("'}")));
                        }
                    }
                    break;
                default:

            }
        }
    }
}
