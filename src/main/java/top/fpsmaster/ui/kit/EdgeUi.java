package top.fpsmaster.ui.kit;

import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.prism.input.FrameInput;
import top.fpsmaster.prism.input.Keys;
import top.fpsmaster.prism.theme.Theme;
import top.fpsmaster.prism.widget.UiFrame;

/**
 * Binds a {@link UiFrame} to the current Edge paint pass.
 */
public final class EdgeUi {
    private static UiFrame current;
    private static final FrameInput fallback = new FrameInput();
    /**
     * 这一帧是不是宿主屏幕开的。键鼠缓冲只有喂过 {@link #keyTyped} 的那一帧有权清：
     * HUD 覆盖层每帧都 begin/end 一次、又排在 drawScreen 前面，无脑清会把玩家这一帧
     * 敲的键冲掉——绑定按钮和文本框双双点不动，纯鼠标的开关滑条却照常。
     */
    private static boolean screenFrame;

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
        screenFrame = true;
    }

    public static void beginOverlay(float guiWidth, float guiHeight) {
        current = new UiFrame(new EdgeHost(null, fallback, guiWidth, guiHeight), theme());
        screenFrame = false;
    }

    public static void end() {
        if (screenFrame) {
            fallback.endFrame();
        }
        screenFrame = false;
        EdgeCanvas.clearPanelClip();
        current = null;
    }

    /**
     * Extra GL clip intersected with every {@code pushClip}. Used so ClickGUI module
     * settings cannot paint past the panel when an expanded card is taller than the window.
     */
    public static void clipPanel(float x, float y, float w, float h) {
        EdgeCanvas.setPanelClip(x, y, w, h);
    }

    public static UiFrame frame() {
        if (current == null) {
            throw new IllegalStateException("EdgeUi.begin() not called for this frame");
        }
        return current;
    }

    public static boolean hasFrame() {
        return current != null;
    }

    public static void keyTyped(char typedChar, int keyCode) {
        if (!Character.isISOControl(typedChar)) {
            fallback.type(String.valueOf(typedChar));
        }
        fallback.pressRawKey(keyCode);
        switch (keyCode) {
            case org.lwjgl.input.Keyboard.KEY_BACK:
                fallback.pressKey(Keys.BACKSPACE);
                break;
            case org.lwjgl.input.Keyboard.KEY_RETURN:
            case org.lwjgl.input.Keyboard.KEY_NUMPADENTER:
                fallback.pressKey(Keys.ENTER);
                break;
            case org.lwjgl.input.Keyboard.KEY_ESCAPE:
                fallback.pressKey(Keys.ESCAPE);
                break;
            case org.lwjgl.input.Keyboard.KEY_LEFT:
                fallback.pressKey(Keys.LEFT);
                break;
            case org.lwjgl.input.Keyboard.KEY_RIGHT:
                fallback.pressKey(Keys.RIGHT);
                break;
            case org.lwjgl.input.Keyboard.KEY_DELETE:
                fallback.pressKey(Keys.DELETE);
                break;
            default:
                break;
        }
    }
}
