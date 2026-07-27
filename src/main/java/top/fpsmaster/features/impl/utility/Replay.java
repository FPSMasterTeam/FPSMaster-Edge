package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.PendingScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;

/**
 * Opens the replay browser.
 *
 * <p>A module rather than only a chat command so it can be bound to a key like anything else — you
 * start a recording before joining a match and stop it after, and typing into chat at either moment
 * is the wrong thing to be doing.
 *
 * <p>It acts rather than toggles: the bound key opens the browser and the module turns itself back
 * off, so the entry in the list never sits in an "on" state that means nothing.
 */
public class Replay extends Module {

    public Replay() {
        super("Replay", Category.Utility);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        set(false);
        // Next tick, for the same reason the chat command needs it: whatever screen the key was
        // pressed in may close itself afterwards, and that would take this one with it.
        PendingScreen.open(new ReplayScreen(null));
    }
}
