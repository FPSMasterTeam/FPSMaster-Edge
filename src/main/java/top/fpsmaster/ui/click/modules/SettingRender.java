package top.fpsmaster.ui.click.modules;

import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

public class SettingRender<T extends Setting<?>> extends ValueRender {

    protected T setting;

    public SettingRender(T setting) {
        this.setting = setting;
    }

    public boolean isWide() {
        return false;
    }

    @Override
    public void render(
            ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom
    ) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}



