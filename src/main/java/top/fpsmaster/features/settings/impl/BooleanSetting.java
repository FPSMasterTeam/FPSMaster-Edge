package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, Boolean value) {
        super(name, value);
    }

    public BooleanSetting(String name, Boolean value, VisibleCondition visible) {
        this(name, value);
        this.visible = visible;
    }

    public void toggle() {
        setValue(!getValue());
    }
}



