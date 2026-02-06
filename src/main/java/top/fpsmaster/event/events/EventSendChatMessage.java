package top.fpsmaster.event.events;

import top.fpsmaster.event.CancelableEvent;

public class EventSendChatMessage extends CancelableEvent {
    public String msg;

    public EventSendChatMessage(String msg) {
        this.msg = msg;
    }
}



