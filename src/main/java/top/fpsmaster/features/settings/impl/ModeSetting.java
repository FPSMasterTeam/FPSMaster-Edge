package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;

import java.util.Objects;

public class ModeSetting extends Setting<Integer> {

    private final String[] modes;

    public ModeSetting(String name, int value, String... modes) {
        super(name, value);
        this.modes = modes;
    }

    public ModeSetting(String name, int value, VisibleCondition visible, String... modes) {
        super(name, value, visible);
        this.modes = modes;
    }

    public void cycle() {
        setValue((getValue() + 1) % modes.length);
    }

    public String getMode(int num) {
        return modes[num - 1];
    }

    public boolean isMode(String mode) {
        return Objects.equals(modes[getValue()], mode);
    }

    public String getModeName() {
        return modes[getValue()];
    }

    public int getMode() {
        return getValue();
    }

    public int getModesSize() {
        return modes.length;
    }
}
