package top.fpsmaster.features.command;

import org.junit.jupiter.api.Test;
import top.fpsmaster.features.settings.impl.ModeSetting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandModeTest {

    @Test
    void rejectsAnUnknownChoice() {
        ModeSetting mode = new ModeSetting("mode", 0, "vanilla", "custom");
        CommandException failure = assertThrows(CommandException.class,
                () -> CommandManager.applyMode(mode, "nope"));
        assertTrue(failure.getMessage().contains("非法选项"));
        assertEquals(0, mode.getValue().intValue());
    }

    @Test
    void acceptsAKnownChoice() throws CommandException {
        ModeSetting mode = new ModeSetting("mode", 0, "vanilla", "custom");
        CommandManager.applyMode(mode, "custom");
        assertEquals(1, mode.getValue().intValue());
    }
}
