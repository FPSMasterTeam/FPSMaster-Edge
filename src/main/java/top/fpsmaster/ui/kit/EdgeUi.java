package top.fpsmaster.ui.kit;

import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.prism.input.FrameInput;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

/**
 * Binds a {@link UiFrame} to the current Edge paint pass.
 */
public final class EdgeUi {
    private static UiFrame current;
    private static final FrameInput fallback = new FrameInput();

    private EdgeUi() {
    }

    public static Theme theme() {
        boolean light = ClientSettings.theme.getValue() == 1;
        return Theme.of(light, ClientSettings.blur.getValue());
    }

    public static void begin(ScaledGuiScreen screen) {
        float w = screen == null ? 0f : screen.guiWidth;
        float h = screen == null ? 0f : screen.guiHeight;
        current = new UiFrame(new EdgeHost(screen, fallback, w, h), theme());
    }

    public static void beginOverlay(float guiWidth, float guiHeight) {
        current = new UiFrame(new EdgeHost(null, fallback, guiWidth, guiHeight), theme());
    }

    public static void end() {
        fallback.endFrame();
        current = null;
    }

    public static UiFrame frame() {
        if (current == null) {
            throw new IllegalStateException("EdgeUi.begin() not called for this frame");
        }
        return current;
    }
}
