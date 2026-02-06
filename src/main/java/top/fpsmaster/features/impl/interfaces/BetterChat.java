package top.fpsmaster.features.impl.interfaces;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

import static top.fpsmaster.utils.core.Utility.mc;

public class BetterChat extends InterfaceModule {
    public static boolean using = false;
    private final BooleanSetting foldMessage = new BooleanSetting("FoldMessage", false);
    private final BooleanSetting copyMessage = new BooleanSetting("CopyMessage", false);
    private IChatComponent lastMessage = null;
    private int counter = 1;
    private static java.lang.reflect.Field chatLinesField;
    private static java.lang.reflect.Field drawnChatLinesField;


    public BetterChat() {
        super("BetterChat", Category.Interface);
        addSettings(foldMessage, copyMessage, backgroundColor, fontShadow, betterFont, bg);
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
    public void onChat(EventPacket e) {
        if (e.packet instanceof S02PacketChat && e.type == EventPacket.PacketType.RECEIVE && ((S02PacketChat) e.packet).getType() != 2) {
            S02PacketChat packet = (S02PacketChat) e.packet;
            IChatComponent copyText = new ChatComponentText(" \247f[C]");
            copyText.getChatStyle()
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "\u0000#COPY" + ((S02PacketChat) e.packet).getChatComponent().getUnformattedText()))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(FPSMaster.i18n.get("copy.hover"))));
            if (foldMessage.getValue()) {
                GuiNewChat chatProvider = mc.ingameGUI.getChatGUI();
                java.util.List<ChatLine> drawnChatLines = getDrawnChatLines(chatProvider);
                java.util.List<ChatLine> chatLines = getChatLines(chatProvider);
                if (drawnChatLines.isEmpty()) {
                    counter = 1;
                    lastMessage = packet.getChatComponent().createCopy();
                } else if (lastMessage.equals(packet.getChatComponent())) {
                    ChatLine c = drawnChatLines.get(0);
                    IChatComponent chatComponent = lastMessage.createCopy().appendSibling(new ChatComponentText("\247r\247f [x" + ++counter + "]"));
                    if (copyMessage.getValue()) {
                        chatComponent.appendSibling(copyText);
                    }
                    c = new ChatLine(c.getUpdatedCounter(), chatComponent, c.getChatLineID());
                    if (!chatLines.isEmpty()) {
                        chatLines.set(0, c);
                    }
                    if (!drawnChatLines.isEmpty()) {
                        drawnChatLines.set(0, c);
                    }
                    e.cancel();
                    return;
                } else {
                    counter = 1;
                    lastMessage = packet.getChatComponent().createCopy();
                }
            }
            if (copyMessage.getValue() && packet.getChatComponent().getUnformattedText().trim().length() > 2) {
                packet.getChatComponent().appendSibling(copyText);
            }
        }
    }

    private static java.util.List<ChatLine> getChatLines(GuiNewChat chat) {
        try {
            if (chatLinesField == null) {
                chatLinesField = GuiNewChat.class.getDeclaredField("chatLines");
                chatLinesField.setAccessible(true);
            }
            return (java.util.List<ChatLine>) chatLinesField.get(chat);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return new java.util.ArrayList<>();
        }
    }

    private static java.util.List<ChatLine> getDrawnChatLines(GuiNewChat chat) {
        try {
            if (drawnChatLinesField == null) {
                drawnChatLinesField = GuiNewChat.class.getDeclaredField("drawnChatLines");
                drawnChatLinesField.setAccessible(true);
            }
            return (java.util.List<ChatLine>) drawnChatLinesField.get(chat);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return new java.util.ArrayList<>();
        }
    }
}



