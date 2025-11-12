package top.fpsmaster.features.impl.utility;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.IChatComponent;
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
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.MathTimer;

public class AutoGG extends Module {
    private final BooleanSetting autoPlay = new BooleanSetting("AutoPlay", false);
    private final NumberSetting delay = new NumberSetting("DelayToPlay", 5, 0, 10, 1, autoPlay::getValue);
    private final TextSetting message = new TextSetting("Message", "gg");
    private final ModeSetting servers = new ModeSetting("Servers", 0, "hypixel", "kkcraft");
    private final String[] hypixelTrigger = new String[]{"Reward Summary", "1st Killer", "Damage Dealt", "奖励总览", "击杀数第一名", "造成伤害"};
    private final String[] kkcraftTrigger = new String[]{"获胜者", "第一名杀手", "击杀第一名"};
    private final MathTimer timer = new MathTimer();

    public AutoGG() {
        super("AutoGG", Category.Utility);
        this.addSettings(autoPlay, delay, message, servers);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.type == EventPacket.PacketType.RECEIVE && ProviderManager.packetChat.isPacket(event.packet)) {
            IChatComponent componentValue = ProviderManager.packetChat.getChatComponent(event.packet);
            String chatMessage = componentValue.getUnformattedText();
            switch (servers.getValue()) {
                case 0:
                    if (timer.delay(10000)) {
                        for (String s : hypixelTrigger) {
                            if (StringUtils.stripControlCodes(chatMessage).contains(s)) {
                                Utility.sendChatMessage("/ac " + message.getValue());
                                timer.reset();
                                break;
                            }
                        }
                    }
                    if (autoPlay.getValue()) {
                        for (IChatComponent chatComponent : componentValue.getSiblings()) {
                            ClickEvent clickEvent = chatComponent.getChatStyle().getChatClickEvent();
                            if (clickEvent != null && clickEvent.getAction().equals(ClickEvent.Action.RUN_COMMAND) && clickEvent.getValue().trim().toLowerCase().startsWith("/play ")) {
                                if (delay.getValue().doubleValue() > 0) {
                                    Utility.sendClientNotify("Sending you to the next game in " + delay.getValue().intValue() + " seconds");
                                    FPSMaster.async.runnable(() -> {
                                        try {
                                            Thread.sleep(delay.getValue().longValue() * 1000);
                                            Utility.sendChatMessage(clickEvent.getValue());
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                } else {
                                    Utility.sendChatMessage(clickEvent.getValue());
                                }
                            }
                        }
                    }
                    break;
                case 1:
                    if (timer.delay(10000)) {
                        for (String s : kkcraftTrigger) {
                            if (StringUtils.stripControlCodes(chatMessage).contains(s)) {
                                Utility.sendChatMessage(message.getValue());
                                timer.reset();
                                if (autoPlay.getValue()) {
                                    Utility.sendClientNotify("AutoPlay is not supported in KKCraft yet");
                                }
                            }
                        }
                    }
                    break;
                default:

            }
        }
    }
}
