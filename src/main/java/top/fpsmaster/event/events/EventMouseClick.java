package top.fpsmaster.event.events;

import top.fpsmaster.event.Event;

public class EventMouseClick implements Event {
    public int button;
    public EventMouseClick(int button) {
        this.button = button;
    }
}



