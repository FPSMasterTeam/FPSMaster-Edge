package top.fpsmaster.impl.client;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import top.fpsmaster.api.domain.client.IGameSettings;
import top.fpsmaster.forge.api.IKeyBinding;

/**
 * 游戏设置实现 (v1.8.9)
 */
public class GameSettingsImpl implements IGameSettings {
    
    private final GameSettings settings;
    
    public GameSettingsImpl(GameSettings settings) {
        this.settings = settings;
    }
    
    @Override
    public void setKeyPressed(String keyName, boolean pressed) {
        // 通过按键名称查找对应的 KeyBinding
        for (KeyBinding key : settings.keyBindings) {
            if (key.getKeyDescription().equals(keyName)) {
                ((IKeyBinding) key).setPressed(pressed);
                return;
            }
        }
    }
    
    @Override
    public int getRenderDistance() {
        return settings.renderDistanceChunks;
    }
    
    @Override
    public void setRenderDistance(int chunks) {
        settings.renderDistanceChunks = chunks;
    }
    
    @Override
    public float getMouseSensitivity() {
        return settings.mouseSensitivity;
    }
    
    @Override
    public void setMouseSensitivity(float sensitivity) {
        settings.mouseSensitivity = sensitivity;
    }
    
    @Override
    public boolean isShowDebugInfo() {
        return settings.showDebugInfo;
    }
    
    @Override
    public int getGuiScale() {
        return settings.guiScale;
    }
    
    @Override
    public float getFov() {
        return settings.fovSetting;
    }
    
    @Override
    public boolean isVsyncEnabled() {
        return settings.enableVsync;
    }
    
    @Override
    public int getFramerateLimit() {
        return settings.limitFramerate;
    }
}
