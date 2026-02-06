package top.fpsmaster.event.events;

import top.fpsmaster.event.Event;

public class EventKey implements Event {
    public int key;
    public EventKey(int keyCode) {
        this.key = keyCode;
    }
}
