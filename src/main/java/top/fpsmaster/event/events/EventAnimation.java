package top.fpsmaster.event.events;


import top.fpsmaster.event.CancelableEvent;

public class EventAnimation extends CancelableEvent {
    Type type;
    float equipProgress;
    float swingProgress;

    public EventAnimation(Type type, float equipProgress, float swingProgress) {
        this.type = type;
        this.equipProgress = equipProgress;
        this.swingProgress = swingProgress;
    }

    public enum Type {
        NONE,
        USE,
        BLOCK,
        BOW
    }
}
