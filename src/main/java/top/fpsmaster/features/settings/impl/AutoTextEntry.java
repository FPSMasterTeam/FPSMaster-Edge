package top.fpsmaster.features.settings.impl;

/**
 * One AutoText entry: a key that sends {@code message} when pressed.
 *
 * <p>Immutable on purpose — entries are stored inside the {@code ArrayList} a {@link AutoTextSetting}
 * owns, and {@link top.fpsmaster.features.settings.Setting#copyValue(Object)} shallow-copies that list.
 * With immutable entries the copy is a safe deep copy, so reset-to-default can never leak edits.
 */
public final class AutoTextEntry {
    public final int keyCode;
    public final String message;

    public AutoTextEntry(int keyCode, String message) {
        this.keyCode = keyCode;
        this.message = message == null ? "" : message;
    }

    public AutoTextEntry(AutoTextEntry other) {
        this(other.keyCode, other.message);
    }
}
