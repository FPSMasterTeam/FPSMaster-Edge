package top.fpsmaster.wrapper;

import net.minecraft.client.settings.KeyBinding;
import top.fpsmaster.forge.api.IKeyBinding;
import top.fpsmaster.api.wrapper.GameSettingsWrap;

public class GameSettingsProvider implements GameSettingsWrap {
    public void setKeyPress(KeyBinding key, boolean value){
        ((IKeyBinding) key).setPressed(value);
    }
}
