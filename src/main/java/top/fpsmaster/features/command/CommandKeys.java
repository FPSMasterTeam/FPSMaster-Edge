package top.fpsmaster.features.command;

import org.lwjgl.input.Keyboard;

import java.util.Locale;

/**
 * Key-name parsing for {@code .bind} and {@code .shortcut set}, over LWJGL2's keyboard tables.
 *
 * <p>{@link Keyboard#getKeyIndex(String)} returns {@link Keyboard#KEY_NONE} for anything it does not
 * know, which is the same value a deliberate unbind uses. {@link #parse(String)} therefore returns
 * {@code null} for unknown names so the caller can tell "unbind" from "typo" and report the typo.
 */
public final class CommandKeys {
    /** Names accepted as "clear this binding". */
    private static final String[] UNBIND_NAMES = {"none", "unbind", "unset", "clear", "off"};

    private CommandKeys() {
    }

    /** Returns the LWJGL key code, 0 for an explicit unbind, or {@code null} if the name is unknown. */
    public static Integer parse(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        for (String unbind : UNBIND_NAMES) {
            if (unbind.equals(normalized)) {
                return Keyboard.KEY_NONE;
            }
        }
        int key = Keyboard.getKeyIndex(normalized.toUpperCase(Locale.ROOT));
        return key == Keyboard.KEY_NONE ? null : key;
    }

    /** Human-readable name for a key code, for echoing back what was bound. */
    public static String format(int key) {
        if (key == Keyboard.KEY_NONE) {
            return "NONE";
        }
        String name = Keyboard.getKeyName(key);
        return name == null || name.isEmpty() ? "KEY_" + key : name;
    }
}
