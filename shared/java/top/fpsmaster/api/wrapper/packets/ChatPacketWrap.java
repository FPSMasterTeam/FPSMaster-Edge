package top.fpsmaster.api.wrapper.packets;

import net.minecraft.util.IChatComponent;

public interface ChatPacketWrap {
    boolean isPacket(Object packet);
    String getUnformattedText(Object packet);
    IChatComponent getChatComponent(Object packet);
    int getType(Object p);
    void appendTranslation(Object p);
}