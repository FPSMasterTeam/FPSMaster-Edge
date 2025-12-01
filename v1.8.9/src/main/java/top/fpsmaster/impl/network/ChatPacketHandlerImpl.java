package top.fpsmaster.impl.network;

import net.minecraft.network.play.server.S02PacketChat;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.domain.network.IChatPacketHandler;

public class ChatPacketHandlerImpl implements IChatPacketHandler {
    
    @Override
    public boolean isChatPacket(@NotNull Object packet) {
        return packet instanceof S02PacketChat;
    }
    
    @Override
    @NotNull
    public String getUnformattedText(@NotNull Object packet) {
        if (packet instanceof S02PacketChat) {
            return ((S02PacketChat) packet).getChatComponent().getUnformattedText();
        }
        return "";
    }
    
    @Override
    public int getChatType(@NotNull Object packet) {
        if (packet instanceof S02PacketChat) {
            return ((S02PacketChat) packet).getType();
        }
        return 0;
    }
}
