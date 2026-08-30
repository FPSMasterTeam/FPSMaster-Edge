package top.fpsmaster.features.impl.optimizes;

import org.junit.jupiter.api.Test;
import top.fpsmaster.features.settings.impl.BooleanSetting;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastRenderDefaultTest {

    @Test
    void fastRenderSettingDefaultsOff() {
        BooleanSetting setting = new BooleanSetting("FastRender", false);
        assertFalse(setting.getValue(), "快速渲染 / Fast Render must default to off");
        setting.setValue(true);
        setting.resetValue();
        assertFalse(setting.getValue());
    }

    /**
     * {@link Performance} pulls Minecraft through {@code Utility.mc}, so the production
     * field cannot be constructed in this JVM. Lock the source default instead.
     */
    @Test
    void performanceSourceDefaultsFastRenderOff() throws Exception {
        Path src = Paths.get("src/main/java/top/fpsmaster/features/impl/optimizes/Performance.java");
        String text = new String(Files.readAllBytes(src), StandardCharsets.UTF_8);
        assertTrue(text.contains("new BooleanSetting(\"FastRender\", false)"),
                "Performance.fastRender must be constructed with default false");
        assertFalse(text.contains("new BooleanSetting(\"FastRender\", true)"),
                "Performance.fastRender must not default to true");
    }
}
