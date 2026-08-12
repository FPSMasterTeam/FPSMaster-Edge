package top.fpsmaster.modules.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import top.fpsmaster.features.impl.interfaces.TargetDisplay;
import top.fpsmaster.features.impl.render.DamageIndicator;
import top.fpsmaster.minimap.Minimap;
import top.fpsmaster.modules.logger.ClientLogger;

import java.lang.ref.WeakReference;

/**
 * Drops references that only make sense inside one client world.
 *
 * <p>A {@code WorldClient} owns its chunk provider, every loaded chunk, every entity and every tile
 * entity in them, so a single field still pointing at an entity after a disconnect keeps the whole
 * previous world resident. The client holds several such fields — the last player hit, the entities
 * the minimap drew — and none of them is reachable from a place that gets told the world went away:
 * they are written from render and attack handlers that simply stop firing.
 *
 * <p>So the notification is manufactured here, from the client tick, which keeps running across a
 * disconnect. Feature classes expose a release hook and this calls them; nothing here reaches into
 * their state directly.
 */
public final class WorldSession {

    /**
     * The world the last check saw.
     *
     * <p>Weak on purpose. A strong field here would pin the very world this class exists to let go
     * of, which is the bug rather than the fix. While {@code mc.theWorld} points at a world it is
     * strongly reachable and this reference cannot be cleared early; once it is gone, a cleared
     * reference and a null world compare equal and there is nothing left to release anyway.
     */
    private static WeakReference<WorldClient> lastWorld = new WeakReference<WorldClient>(null);

    private WorldSession() {
    }

    /** Call once per client tick, whether or not a world is loaded. */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc == null ? null : mc.theWorld;
        if (world == lastWorld.get()) {
            return;
        }
        lastWorld = new WeakReference<WorldClient>(world);
        release();
    }

    /**
     * Releases every per-world reference the client holds.
     *
     * <p>Each hook is isolated: a feature that throws while clearing its own state must not stop the
     * others from being cleared, because whatever it failed to drop is exactly what would keep the
     * old world alive.
     */
    private static void release() {
        clear("TargetDisplay", new Runnable() {
            @Override
            public void run() {
                TargetDisplay.releaseWorldState();
            }
        });
        clear("DamageIndicator", new Runnable() {
            @Override
            public void run() {
                DamageIndicator.releaseWorldState();
            }
        });
        clear("Minimap", new Runnable() {
            @Override
            public void run() {
                Minimap.releaseWorldState();
            }
        });
    }

    private static void clear(String owner, Runnable hook) {
        try {
            hook.run();
        } catch (Throwable e) {
            ClientLogger.warn("Failed to release world state for " + owner + ": " + e.getMessage());
        }
    }
}
