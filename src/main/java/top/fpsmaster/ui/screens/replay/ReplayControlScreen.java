package top.fpsmaster.ui.screens.replay;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;

/**
 * Hands the cursor back so the replay controls can be used, and nothing else.
 *
 * <p>A free camera and a cursor cannot both have the mouse, so one of them has to be asked for.
 * Escape asks: with no screen open it opens this one, and with this one open it closes again.
 * Clicking the game view above the timeline also closes it. Camera preview is not touched — that
 * stays on the timeline eye toggle.
 *
 * <p>It does not pause the game. Watching the timeline move while scrubbing to a point is the reason
 * to have it open at all.
 */
public class ReplayControlScreen extends ScaledGuiScreen {

    @Override
    public void initGui() {
        super.initGui();
        showCursor();
    }

    @Override
    public void updateScreen() {
        showCursor();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        showCursor();
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            // The recording ended underneath, or something else stopped it. Its own teardown puts
            // the browser up; staying here would leave a cursor over controls for nothing.
            mc.displayGuiScreen(null);
            return;
        }
        ReplayHud.drawInteractive(this, guiWidth, mouseX, mouseY);
        DirectorPanel.draw(this, guiWidth, guiHeight, mouseX, mouseY);
        DirectorPanel.drawResultBanner(this, guiWidth, guiHeight, mouseX, mouseY);
        // Clicking the world (not the HUD / timeline) drops the cursor so the camera can fly.
        // Skip while a timeline drag is live: releasing a scrub over the game view is not a click.
        if (!hasActiveDrag()
                && !DirectorPanel.covers(guiHeight, mouseX, mouseY)
                && !ReplayHud.contains(mouseX, mouseY)
                && consumePressInBounds(0f, 0f, guiWidth, guiHeight, 0) != null) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (ctrl && keyCode == Keyboard.KEY_S) {
            top.fpsmaster.replay.director.DirectorCamera.markDirty();
            top.fpsmaster.replay.director.DirectorCamera.saveIfDirty();
            return;
        }
        if (ctrl && keyCode == Keyboard.KEY_Z) {
            if (shift) {
                top.fpsmaster.replay.director.DirectorCamera.redo();
            } else {
                top.fpsmaster.replay.director.DirectorCamera.undo();
            }
            return;
        }
        if (ctrl && keyCode == Keyboard.KEY_Y) {
            top.fpsmaster.replay.director.DirectorCamera.redo();
            return;
        }
        if (ctrl && keyCode == Keyboard.KEY_D) {
            DirectorPanel.duplicateSelectedClip();
            return;
        }
        if (DirectorPanel.handleKey(keyCode)) {
            return;
        }
        // Swallow the rest. This screen is the director, not a pause menu: T/E/Q must not fall
        // through to vanilla chat / inventory / drop.
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * LWJGL2 on macOS sometimes grabs the cursor mid-drag (especially once the pointer leaves the
     * timeline and sits over the world). While this screen is open the cursor must stay visible.
     */
    private static void showCursor() {
        if (Mouse.isGrabbed()) {
            Mouse.setGrabbed(false);
        }
    }
}
