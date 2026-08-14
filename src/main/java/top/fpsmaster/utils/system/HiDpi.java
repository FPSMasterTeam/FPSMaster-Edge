package top.fpsmaster.utils.system;

import org.lwjgl.opengl.Display;

/**
 * Retina / HiDPI support for macOS.
 *
 * <p>LWJGL2 creates the game window with a 1x backing store by default, so on a Retina display the
 * 1280x800 framebuffer is bitmap-stretched to 2560x1600 by the OS and everything looks soft. LWJGL
 * 2.9.1+ can opt into a high-resolution backing via the
 * {@code org.lwjgl.opengl.Display.enableHighDPI} property, but Minecraft 1.8.9 itself is unaware of
 * it: {@code Display.getWidth()} and the mouse APIs keep reporting window <em>points</em>, while the
 * GL surface becomes larger by {@link Display#getPixelScaleFactor()}. Enabling the flag alone would
 * therefore render into a quarter of the window and put every click off by 2x.
 *
 * <p>So this class owns both halves of the contract: {@link #requestHighDpiBacking()} turns the
 * backing on early (before the window exists), and the scale helpers convert LWJGL's point values to
 * real pixels at the few places Minecraft consumes them (display size on create/resize/fullscreen,
 * GUI mouse coordinates). Everything downstream — framebuffer size, ScaledResolution, the client's
 * own UiScale, screenshots — keys off {@code mc.displayWidth} and needs no further changes. Mouse
 * <em>deltas</em> (camera look) stay in points on purpose: that is what they were on a 1x backing,
 * so sensitivity is unchanged.
 *
 * <p>Escape hatch: {@code -Dfpsmaster.hidpi=false}. Windows/Linux are untouched — their windows are
 * already sized in physical pixels.
 */
public final class HiDpi {

    private static final boolean ENABLED =
            OSUtil.isMac() && !"false".equalsIgnoreCase(System.getProperty("fpsmaster.hidpi"));

    private HiDpi() {
    }

    /**
     * Opts the window into a high-resolution backing store. Must run before
     * {@code Display.create()}; the mixin bootstrap (config plugin load) is early enough.
     */
    public static void requestHighDpiBacking() {
        if (ENABLED) {
            System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");
        }
    }

    /** Backing pixels per window point. 1 when disabled, headless, or on a non-Retina display. */
    public static float scale() {
        if (!ENABLED) {
            return 1f;
        }
        try {
            if (!Display.isCreated()) {
                return 1f;
            }
            return Math.max(1f, Display.getPixelScaleFactor());
        } catch (Throwable t) {
            return 1f;
        }
    }

    public static boolean active() {
        return scale() > 1.001f;
    }

    /** For window dimensions — clamped to at least one pixel. */
    public static int pointsToPixels(int points) {
        return Math.max(1, Math.round(points * scale()));
    }

    /** For cursor coordinates — zero stays zero. */
    public static int mouseToPixels(int points) {
        return Math.round(points * scale());
    }

    public static int pixelsToPoints(int pixels) {
        return Math.max(1, Math.round(pixels / scale()));
    }
}
