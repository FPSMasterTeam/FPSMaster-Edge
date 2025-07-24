package top.fpsmaster.features.settings;

import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventValueChange;

public class Setting<T> {

    public String name;
    public T value;
    public VisibleCondition visible;

    public Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public Setting(String name, T value, VisibleCondition visible) {
        this(name, value);
        this.visible = visible;
    }

    public boolean getVisible() {
        return visible == null || visible.isVisible();
    }

    // Functional interface to represent the visibility check (similar to the Kotlin lambda)
    public interface VisibleCondition {
        boolean isVisible();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        EventValueChange event = new EventValueChange(this, this.value, value);
        EventDispatcher.dispatchEvent(event);
        if (!event.isCanceled()) {
            this.value = value;
        }
    }
}
