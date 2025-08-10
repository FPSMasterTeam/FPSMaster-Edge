package top.fpsmaster.features.impl.interfaces;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.event.ClickEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.interfaces.gui.IGuiNewChatProvider;

import static top.fpsmaster.utils.Utility.mc;

public class BetterChat extends InterfaceModule {
    public static boolean using = false;
    private final BooleanSetting foldMessage = new BooleanSetting("FoldMessage", false);
    private String lastMessage = "";
    private int counter = 1;

    public BetterChat() {
        super("BetterChat", Category.Interface);
        addSettings(foldMessage, backgroundColor, fontShadow, betterFont, bg);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    @Subscribe
    public void onPacketReceive(EventPacket e) {
        if (e.type == EventPacket.PacketType.SEND || !foldMessage.getValue()) return;
        if (e.packet instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) e.packet;
            if (packet.getType() == 2) return;
            IGuiNewChatProvider chatProvider = (IGuiNewChatProvider) mc.ingameGUI.getChatGUI();
            if (chatProvider.getDrawnChatLines().isEmpty()) {
                counter = 1;
                lastMessage = packet.getChatComponent().getUnformattedText();
                return;
            }
            if (lastMessage.equals(packet.getChatComponent().getUnformattedText())
                    && packet.getChatComponent().getChatStyle().getChatHoverEvent() == null
                    && packet.getChatComponent().getChatStyle().getChatClickEvent() == null
                    && packet.getChatComponent().getSiblings() != null
                    && packet.getChatComponent().getSiblings().stream().allMatch(c -> c.getChatStyle().getChatClickEvent() == null && c.getChatStyle().getChatHoverEvent() == null)) {
                ChatLine c = chatProvider.getDrawnChatLines().get(0);
                String text = packet.getChatComponent().getUnformattedText();
                c = new ChatLine(c.getUpdatedCounter(), new ChatComponentText(text + "\247r\247f [x" + ++counter + "]"), c.getChatLineID());
                chatProvider.getChatLines().set(0, c);
                chatProvider.getDrawnChatLines().set(0, c);
                e.cancel();
            } else {
                counter = 1;
                lastMessage = packet.getChatComponent().getUnformattedText();
            }
        }
    }
}
