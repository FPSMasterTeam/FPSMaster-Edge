package top.fpsmaster.features.impl.interfaces;

import org.junit.jupiter.api.Test;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.BooleanSetting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterScreenSettingsTest {

    @Test
    void commonSettingsDoNotDuplicateTheScreenBackground() {
        BetterScreen module = new BetterScreen();
        module.registerCommonSettings();
        module.registerCommonSettings();

        Set<String> names = new HashSet<>();
        for (Setting<?> setting : module.settings) {
            assertTrue(names.add(setting.name), "Duplicate setting name: " + setting.name);
        }
        assertTrue(module.settings.contains(BetterScreen.screenBackground));
        assertFalse(module.settings.contains(module.bg));
        assertFalse(module.settings.contains(module.backgroundColor));
        assertTrue(module.settings.contains(module.betterFont));
        assertTrue(module.settings.contains(module.fontShadow));
    }

    @Test
    void savingByNameKeepsTheUsersScreenBackgroundChoice() {
        BetterScreen module = new BetterScreen();
        module.registerCommonSettings();
        boolean previous = BetterScreen.screenBackground.getValue();
        try {
            BetterScreen.screenBackground.setValue(false);
            Map<String, Boolean> saved = new HashMap<>();
            for (Setting<?> setting : module.settings) {
                if (setting instanceof BooleanSetting) {
                    saved.put(setting.name, ((BooleanSetting) setting).getValue());
                }
            }
            assertEquals(Boolean.FALSE, saved.get("Background"));
        } finally {
            BetterScreen.screenBackground.setValue(previous);
        }
    }
}
