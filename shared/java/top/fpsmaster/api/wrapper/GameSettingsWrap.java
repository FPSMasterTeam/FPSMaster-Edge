package top.fpsmaster.api.wrapper;

import net.minecraft.client.settings.KeyBinding;

public interface GameSettingsWrap {
    void setKeyPress(KeyBinding key, boolean value);
}