package top.fpsmaster.api.provider.gui;

import net.minecraft.client.gui.ChatLine;
import top.fpsmaster.api.provider.IProvider;

import java.util.List;

public interface IGuiNewChatProvider extends IProvider {
    List<ChatLine> getChatLines();
    List<ChatLine> getDrawnChatLines();
}