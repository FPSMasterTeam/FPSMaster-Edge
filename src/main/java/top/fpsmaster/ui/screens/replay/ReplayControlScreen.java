package top.fpsmaster.ui.screens.replay;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;

/**
 * Hands the cursor back so the replay controls can be used, and nothing else.
 *
 * <p>A free camera and a cursor cannot both have the mouse, so one of them has to be asked for.
 * Escape asks: with no screen open it opens this one, and with this one open it closes again. That
 * is the whole screen — it draws no chrome of its own and takes no space, because {@link ReplayHud}
 * is already drawing the bar and only needs to be told that a pointer exists this frame.
 *
 * <p>It does not pause the game. Watching the timeline move while scrubbing to a point is the reason
 * to have it open at all.
 */
public class ReplayControlScreen extends ScaledGuiScreen {

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
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
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_P) {
            ReplayPlayer.instance().togglePause();
            return;
        }
        // K: capture a camera keyframe without reaching for the workbench button.
        if (keyCode == Keyboard.KEY_K) {
            ReplayPlayer player = ReplayPlayer.instance();
            if (player.isActive() && !player.isPossessing()) {
                top.fpsmaster.replay.director.CameraPose pose =
                        top.fpsmaster.replay.director.DirectorCamera.capturePose();
                if (pose != null) {
                    top.fpsmaster.replay.director.DirectorCamera.track()
                            .add(player.elapsedMillis(), pose,
                                    top.fpsmaster.replay.director.DirectorCamera.MERGE_WINDOW_MILLIS);
                    top.fpsmaster.replay.director.DirectorCamera.markDirty();
                    top.fpsmaster.replay.director.DirectorCamera.saveIfDirty();
                }
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
