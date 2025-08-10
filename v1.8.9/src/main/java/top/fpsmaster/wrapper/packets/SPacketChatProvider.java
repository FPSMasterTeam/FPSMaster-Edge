package top.fpsmaster.wrapper.packets;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.interfaces.packets.IPacketChat;
import top.fpsmaster.wrapper.TextFormattingProvider;


public class SPacketChatProvider implements IPacketChat {
    public boolean isPacket(@NotNull Object p) {
        return p instanceof S02PacketChat;
    }

    @NotNull
    @Override
    public String getUnformattedText(@NotNull Object p) {
        return getChatComponent(p).getUnformattedText();
    }

    @NotNull
    @Override
    public IChatComponent getChatComponent(@NotNull Object p) {
        return ((S02PacketChat) p).getChatComponent();
    }

    @Override
    public int getType(@NotNull Object p) {
        return ((S02PacketChat) p).getType();
    }

    @Override
    public void appendTranslation(@NotNull Object p) {
        String unformattedText = getUnformattedText(p);
        if (!unformattedText.endsWith(" [T]") && unformattedText.length() > 5) {
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "#TRANSLATE" + unformattedText);
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(FPSMaster.i18n.get("translate.hover")));
            ChatStyle style = new ChatStyle().setChatClickEvent(clickEvent).setChatHoverEvent(hoverEvent);
            ChatComponentText iTextComponents = new ChatComponentText(TextFormattingProvider.getGray().toString() + " [T]");
            iTextComponents.setChatStyle(style);
            ((S02PacketChat) p).getChatComponent().appendSibling(iTextComponents);
        }
    }
}

