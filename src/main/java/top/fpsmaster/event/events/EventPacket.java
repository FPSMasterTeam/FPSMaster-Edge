package top.fpsmaster.event.events;

import net.minecraft.network.Packet;
import top.fpsmaster.event.CancelableEvent;

public class EventPacket extends CancelableEvent {
    
    public PacketType type;
    
    public Packet<?> packet;

    public EventPacket(PacketType type, Packet<?> packet) {
        this.type = type;
        this.packet = packet;
    }

    public enum PacketType {
        SEND,
        RECEIVE
    }
}
