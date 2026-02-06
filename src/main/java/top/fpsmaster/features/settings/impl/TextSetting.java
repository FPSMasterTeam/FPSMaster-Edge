package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;

public class TextSetting extends Setting<String> {

    public TextSetting(String name, String value) {
        super(name, value);
    }

    public TextSetting(String name, String value, VisibleCondition visible) {
        super(name, value, visible);
    }

}



