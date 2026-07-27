package top.fpsmaster.features.impl.utility;

import net.minecraft.client.Minecraft;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
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
        Minecraft mc = Minecraft.getMinecraft();
        // Deferred by a tick for the same reason the chat command has to be: whatever screen the key
        // was pressed in may close itself afterwards, and closing a screen sets the current one to
        // null - taking this one with it.
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                set(false);
                Minecraft.getMinecraft().displayGuiScreen(new ReplayScreen(null));
            }
        });
    }
}
