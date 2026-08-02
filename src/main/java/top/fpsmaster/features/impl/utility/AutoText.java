package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.AutoTextEntry;
import top.fpsmaster.features.settings.impl.AutoTextSetting;
import top.fpsmaster.utils.core.Utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Editable shortcut-to-chat entries. Each entry binds a key to a message.
 * When the key is pressed (and the module is enabled, the screen is null,
 * and the player is alive), the message is sent via {@link Utility#sendChatMessage}.
 *
 * <p>Default: G → gg. Cap: 20 entries. Duplicate key bindings are rejected at the editor.
 */
public class AutoText extends Module {
    private static final ArrayList<AutoTextEntry> DEFAULT = new ArrayList<>();
    static {
        DEFAULT.add(new AutoTextEntry(0x22 /* G key LWJGL */, "gg"));
    }

    public final AutoTextSetting entries = new AutoTextSetting("Entries", new ArrayList<>(DEFAULT));

    public AutoText() {
        super("AutoText", Category.Utility);
        addSettings(entries);
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (e.key == 0) return;
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen != null) return;
        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer == null) return;

        List<AutoTextEntry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<>(entries.getValue());
        }

        for (AutoTextEntry entry : snapshot) {
            if (entry.keyCode == e.key && !entry.message.isEmpty()) {
                Utility.sendChatMessage(entry.message);
            }
        }
    }
}