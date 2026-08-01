package top.fpsmaster.ui.custom;

import top.fpsmaster.FPSMaster;

/**
 * A HUD component that is one background panel with a single line of text on it.
 *
 * <p>Nine components repeated the same four statements verbatim — measure the string, set a fixed
 * height, paint the panel, paint the text — differing only in font size, padding, box height and text
 * offset. Expressing that as a template also fixes the sizing order for free: {@link #measure()} runs
 * before anything reads {@code width}/{@code height}, instead of the size being assigned midway
 * through {@code draw()}.
 */
public abstract class TextComponent extends Component {

    private String measured;

    protected TextComponent(Class<?> clazz) {
        super(clazz);
    }

    /** The line to render, or {@code null} when there is nothing to show this frame. */
    protected abstract String text();

    protected abstract int fontSize();

    protected abstract int textColor();

    protected float horizontalPadding() {
        return 4f;
    }

    protected float boxHeight() {
        return 14f;
    }

    protected float textOffsetX() {
        return 0f;
    }

    protected float textOffsetY() {
        return 2f;
    }

    /**
     * Resolves a user-editable label, falling back to the translation and then to a built-in string.
     *
     * <p>Three components carried a private copy of this. Each also wrote the resolved value back with
     * {@code setValue} from inside {@code draw()}, which fires EventValueChange and therefore a config
     * save — from the render path, potentially every frame. Resolving without persisting keeps the same
     * display and drops the write entirely: an empty setting simply resolves again next frame.
     *
     * @param i18nKey  translation key; {@code Language.get} returns the key itself when unmapped
     * @param fallback used when the key has no translation
     */
    protected String resolveLabel(String configured, String i18nKey, String fallback) {
        String label = configured;
        if (label == null || label.trim().isEmpty()) {
            label = FPSMaster.i18n.get(i18nKey);
            if (i18nKey.equals(label)) {
                label = fallback;
            }
        }
        if (!label.endsWith("：") && !label.endsWith(":") && !label.endsWith(" ")) {
            label += ": ";
        }
        return label;
    }

    @Override
    public void measure() {
        measured = text();
        if (measured == null) {
            // Zero the size rather than leaving last frame's: a stale width would have the blur mask
            // stamp a panel this component is no longer drawing.
            width = 0f;
            height = 0f;
            return;
        }
        width = getStringWidth(fontSize(), measured) + horizontalPadding();
        height = boxHeight();
    }

    @Override
    public void draw(float x, float y) {
        if (measured == null) {
            return;
        }
        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        // Offsets are positions, so they scale; drawString scales the glyphs itself.
        drawString(fontSize(), measured, x + textOffsetX() * scale, y + textOffsetY() * scale, textColor());
    }
}
