package top.fpsmaster.replay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventMotionBlur;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.ui.screens.replay.ReplayControlScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;

/**
 * The director camera is a separate picture from the game: no vanilla HUD, and no vanilla binds
 * that would open chat, inventory, drop items, swap hotbar slots, or cycle perspective.
 *
 * <p>WASD / jump / sneak / sprint stay live so the camera can still fly. Possession is the other
 * view — the recorder's first person — and keeps the normal overlay.
 */
public final class ReplayDirectorIsolation {

    private ReplayDirectorIsolation() {
    }

    public static boolean isDirectorView() {
        ReplayPlayer player = ReplayPlayer.instance();
        return player.isActive() && !player.isPossessing();
    }

    public static boolean blocksVanillaBind(KeyBinding binding) {
        if (!isDirectorView() || binding == null) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return false;
        }
        GameSettings settings = mc.gameSettings;
        return binding != settings.keyBindForward
                && binding != settings.keyBindBack
                && binding != settings.keyBindLeft
                && binding != settings.keyBindRight
                && binding != settings.keyBindJump
                && binding != settings.keyBindSneak
                && binding != settings.keyBindSprint;
    }

    /**
     * Cancels vanilla overlay widgets but keeps the 2D projection Forge would have set up
     * inside {@code renderGameOverlay}. Skipping that call leaves the perspective matrix in
     * place, so the replay chrome and {@code ReplayControlScreen} draw off-screen.
     */
    public static boolean consumeVanillaHud(float partialTicks) {
        if (!isDirectorView()) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.entityRenderer != null) {
            mc.entityRenderer.setupOverlayRendering();
        }
        EventDispatcher.dispatchEvent(new EventRender2D(partialTicks));
        EventDispatcher.dispatchEvent(new EventMotionBlur());
        return true;
    }

    public static void tick(Minecraft mc) {
        if (mc == null || !isDirectorView()) {
            return;
        }
        if (mc.gameSettings != null && mc.gameSettings.thirdPersonView != 0) {
            mc.gameSettings.thirdPersonView = 0;
        }
        GuiScreen screen = mc.currentScreen;
        if (screen == null || isOwnScreen(screen)) {
            return;
        }
        mc.displayGuiScreen(null);
    }

    private static boolean isOwnScreen(GuiScreen screen) {
        return screen instanceof ReplayControlScreen
                || screen instanceof ReplayScreen;
    }
}
