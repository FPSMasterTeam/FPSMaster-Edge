package top.fpsmaster.event.events;

import top.fpsmaster.event.CancelableEvent;
import top.fpsmaster.features.settings.Setting;

public class EventValueChange extends CancelableEvent {
    public Setting<?> setting;
    public Object oldValue;
    public Object newValue;
    public EventValueChange(Setting<?> setting, Object oldValue, Object newValue) {
        this.setting = setting;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
