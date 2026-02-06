package top.fpsmaster.event.events;

import net.minecraft.network.Packet;
import top.fpsmaster.event.Event;

public class EventCustomPacket implements Event {
    Packet<?> packet;

    public EventCustomPacket(Packet<?> packet) {
        this.packet = packet;
    }
}



