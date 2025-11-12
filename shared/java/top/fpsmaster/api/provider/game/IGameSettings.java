package top.fpsmaster.api.provider.game;

import net.minecraft.client.settings.KeyBinding;
import top.fpsmaster.api.provider.IProvider;

public interface IGameSettings extends IProvider {
    void setKeyPress(KeyBinding key, boolean value);
}