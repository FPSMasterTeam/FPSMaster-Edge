package top.fpsmaster.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * Opens a screen on the next client tick.
 *
 * <p>Needed because some places that want to open one are themselves about to close a screen, and
 * closing a screen means {@code displayGuiScreen(null)} — which throws away whatever was just
 * opened. A chat command is the clearest case: it runs while the chat GUI is up, and the chat GUI
 * closes itself once the message is handled.
 *
 * <p>{@code Minecraft.addScheduledTask} does not help here. It only queues when called from another
 * thread; on the client thread it runs the task immediately, which is exactly the case this has to
 * deal with.
 */
public final class PendingScreen {

    private static GuiScreen pending;

    private PendingScreen() {
    }

    public static void open(GuiScreen screen) {
        pending = screen;
    }

    /** Called once per client tick. */
    public static void tick() {
        if (pending == null) {
            return;
        }
        GuiScreen screen = pending;
        pending = null;
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }
}
