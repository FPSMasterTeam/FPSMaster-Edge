package top.fpsmaster.wrapper.packets;

import net.minecraft.network.play.server.S03PacketTimeUpdate;
import top.fpsmaster.api.wrapper.packets.TimeUpdatePacketWrap;

public class SPacketTimeUpdateProvider implements TimeUpdatePacketWrap {
    public boolean isPacket(Object packet) {
        return packet instanceof S03PacketTimeUpdate;
    }
}
