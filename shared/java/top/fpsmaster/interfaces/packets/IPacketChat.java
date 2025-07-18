package top.fpsmaster.interfaces.packets;

import net.minecraft.util.IChatComponent;

public interface IPacketChat extends IPacket {
    String getUnformattedText(Object packet);
    IChatComponent getChatComponent(Object packet);
    int getType(Object p);
    void appendTranslation(Object p);
}
