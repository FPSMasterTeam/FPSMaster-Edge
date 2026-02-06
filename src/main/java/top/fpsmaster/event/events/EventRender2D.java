package top.fpsmaster.event.events;

import top.fpsmaster.event.Event;

public class EventRender2D implements Event {
    public float partialTicks;

    public EventRender2D(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}



