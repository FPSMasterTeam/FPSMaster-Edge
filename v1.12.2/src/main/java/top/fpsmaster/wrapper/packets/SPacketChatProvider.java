package top.fpsmaster.wrapper.packets;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.api.wrapper.packets.ChatPacketWrap;
import top.fpsmaster.wrapper.TextFormattingProvider;


public class SPacketChatProvider implements ChatPacketWrap {
    public boolean isPacket(@NotNull Object p) {
        return p instanceof SPacketChat;
    }

    @NotNull
    public String getUnformattedText(@NotNull Object p){
        return ((SPacketChat) p).getChatComponent().getUnformattedText();
    }

    public int getType(Object p){
        return ((SPacketChat) p).getType().ordinal();
    }

    public void appendTranslation(Object p){
        String unformattedText = getUnformattedText(p);
        if (!unformattedText.endsWith(" [T]") && unformattedText.length() > 5) {
            ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "#TRANSLATE" + unformattedText);
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(FPSMaster.i18n.get("translate.hover")));
            Style style = new Style().setClickEvent(clickEvent).setHoverEvent(hoverEvent);
            TextComponentString iTextComponents = new TextComponentString(TextFormattingProvider.getGray().toString() + " [T]");
            iTextComponents.setStyle(style);
            ((SPacketChat) p).getChatComponent().appendSibling(iTextComponents);
        }
    }
}

