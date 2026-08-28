package top.fpsmaster.features.command;

import org.junit.jupiter.api.Test;
import top.fpsmaster.features.settings.impl.NumberSetting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandNumberTest {

    @Test
    void rejectsANumberOutsideTheSettingRange() {
        NumberSetting fov = new NumberSetting("fov", 70, 30, 110, 5);
        CommandException failure = assertThrows(CommandException.class,
                () -> CommandManager.applyNumber(fov, "200"));
        assertTrue(failure.getMessage().contains("越界"));
        assertEquals(70, fov.getValue().doubleValue(), 0.001);
    }

    @Test
    void rejectsANonNumber() {
        NumberSetting fov = new NumberSetting("fov", 70, 30, 110, 5);
        CommandException failure = assertThrows(CommandException.class,
                () -> CommandManager.applyNumber(fov, "nope"));
        assertTrue(failure.getMessage().contains("无效"));
        assertEquals(70, fov.getValue().doubleValue(), 0.001);
    }
}
