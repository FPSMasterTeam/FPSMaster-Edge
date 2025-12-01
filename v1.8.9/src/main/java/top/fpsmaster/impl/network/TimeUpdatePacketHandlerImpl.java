package top.fpsmaster.impl.network;

import net.minecraft.network.play.server.S03PacketTimeUpdate;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.domain.network.ITimeUpdatePacketHandler;

public class TimeUpdatePacketHandlerImpl implements ITimeUpdatePacketHandler {
    
    @Override
    public boolean isTimeUpdatePacket(@NotNull Object packet) {
        return packet instanceof S03PacketTimeUpdate;
    }
    
    @Override
    public long getWorldTime(@NotNull Object packet) {
        if (packet instanceof S03PacketTimeUpdate) {
            return ((S03PacketTimeUpdate) packet).getWorldTime();
        }
        return 0;
    }
}
