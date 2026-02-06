package top.fpsmaster.ui.click.modules;

import top.fpsmaster.features.settings.Setting;

public class SettingRender<T extends Setting<?>> extends ValueRender {

    protected T setting;

    public SettingRender(T setting) {
        this.setting = setting;
    }

    @Override
    public void render(
            float x, float y, float width, float height, float mouseX, float mouseY, boolean custom
    ) {
    }

    @Override
    public void mouseClick(
            float x, float y, float width, float height, float mouseX, float mouseY, int btn
    ) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}



