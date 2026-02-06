package top.fpsmaster.event.events;

import top.fpsmaster.event.Event;

public class EventRender3D implements Event {
    public float partialTicks;

    public EventRender3D(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}
