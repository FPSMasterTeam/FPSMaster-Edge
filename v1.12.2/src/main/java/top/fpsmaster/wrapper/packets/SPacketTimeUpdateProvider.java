package top.fpsmaster.wrapper.packets;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.wrapper.packets.TimeUpdatePacketWrap;

public class SPacketTimeUpdateProvider implements TimeUpdatePacketWrap {
    public boolean isPacket(@NotNull Object packet) {
        return packet instanceof SPacketTimeUpdate;
    }
}
