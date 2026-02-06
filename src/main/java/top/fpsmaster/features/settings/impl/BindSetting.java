package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;

public class BindSetting extends Setting<Integer> {

    public BindSetting(String name, Integer value) {
        super(name, value);
    }

    public BindSetting(String name, Integer value, VisibleCondition visible) {
        super(name, value, visible);
    }
}
