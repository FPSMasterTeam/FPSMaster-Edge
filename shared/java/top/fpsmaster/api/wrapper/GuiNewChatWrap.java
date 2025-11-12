package top.fpsmaster.api.wrapper;

import net.minecraft.client.gui.ChatLine;

import java.util.List;

public interface GuiNewChatWrap {
    List<ChatLine> getChatLines();
    List<ChatLine> getDrawnChatLines();
}