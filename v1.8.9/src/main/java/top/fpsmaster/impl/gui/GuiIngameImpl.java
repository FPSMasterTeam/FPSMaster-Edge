package top.fpsmaster.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import top.fpsmaster.api.domain.gui.IGuiIngame;

public class GuiIngameImpl implements IGuiIngame {
    
    @Override
    public void printChatMessage(String message) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI()
            .printChatMessage(new ChatComponentText(message));
    }
}
