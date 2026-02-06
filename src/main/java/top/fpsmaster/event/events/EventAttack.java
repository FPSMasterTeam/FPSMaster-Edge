package top.fpsmaster.event.events;

import net.minecraft.entity.Entity;
import top.fpsmaster.event.Event;

public class EventAttack implements Event {
    public Entity target;
    public EventAttack(Entity target) {
        this.target = target;
    }
}



