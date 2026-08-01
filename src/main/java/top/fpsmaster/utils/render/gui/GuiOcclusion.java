package top.fpsmaster.utils.render.gui;

/**
 * Where the topmost custom screen is painting, in device pixels.
 *
 * <p>Hit-test ordering within one screen is handled by {@code GuiInputState}'s hovered-id. This solves
 * the other half: the HUD component editor runs from {@code EventRender2D}, which 1.8.9 fires while
 * drawing the in-game overlay — that is, <em>before</em> {@code currentScreen.drawScreen()}. The editor
 * therefore reacts to presses underneath a screen that has not even been painted yet, using a
 * completely separate input path, with no way for the two to arbitrate.
 *
 * <p>The fix is not to disable HUD editing while a panel is open — opening the ClickGUI is precisely
 * when a user wants to move HUD elements. It is to make the panel occlude the cursor: components under
 * the panel rectangle stop responding, components beside it keep working.
 *
 * <p>Device pixels are used because that is the only space the HUD editor and the ClickGUI agree on;
 * they scale their own coordinates by different factors.
 */
public final class GuiOcclusion {

    private static float x;
    private static float y;
    private static float width;
    private static float height;
    private static boolean active;

    private GuiOcclusion() {
    }

    /** Reported once per frame by the screen that is painting, before the next frame's HUD pass. */
    public static void set(float deviceX, float deviceY, float deviceWidth, float deviceHeight) {
        x = deviceX;
        y = deviceY;
        width = deviceWidth;
        height = deviceHeight;
        active = deviceWidth > 0f && deviceHeight > 0f;
    }

    public static void clear() {
        active = false;
    }

    /** @param deviceX cursor position in device pixels, y measured from the top */
    public static boolean covers(float deviceX, float deviceY) {
        return active
                && deviceX >= x && deviceX <= x + width
                && deviceY >= y && deviceY <= y + height;
    }
}
