package top.fpsmaster.features.impl.optimizes;

import org.junit.jupiter.api.Test;
import top.fpsmaster.features.settings.impl.BooleanSetting;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FastRenderDefaultTest {

    @Test
    void fastRenderSettingDefaultsOff() {
        BooleanSetting setting = new BooleanSetting("FastRender", false);
        assertFalse(setting.getValue(), "快速渲染 / Fast Render must default to off");
        setting.setValue(true);
        setting.resetValue();
        assertFalse(setting.getValue());
    }
}
