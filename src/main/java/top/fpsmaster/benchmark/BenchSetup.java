package top.fpsmaster.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Runs a scenario's one-off world setup once the world is loaded.
 *
 * <p>Expressed as server commands rather than direct world manipulation: commands go through the
 * integrated server, so entities exist server-side and behave the way they would in a real session.
 * Spawning client-side only would produce entities the server immediately removes.
 *
 * <p>Commands are handed to the server's command manager directly rather than sent as chat. The
 * client truncates outgoing chat at 100 characters, which silently mangles any {@code /summon}
 * carrying an NBT tag — the server then rejects it with "unbalanced brackets" and the scenario
 * quietly comes up empty.
 *
 * <p>Determinism is the point. Left alone, a world drifts: the sun moves, weather rolls in, mobs
 * spawn and despawn on their own schedule. Every scenario should pin time, weather and mob spawning
 * so that two runs half an hour apart are rendering the same thing.
 */
public final class BenchSetup {

    private static volatile boolean setupComplete;
    private static volatile boolean setupFailed;

    private BenchSetup() {
    }

    /** Whether the queued commands have finished running on the server thread. */
    public static boolean isComplete() {
        return setupComplete;
    }

    public static boolean hasFailed() {
        return setupFailed;
    }

    public static void run(Minecraft mc, JsonObject scenarioWorld) {
        if (scenarioWorld == null || !scenarioWorld.has("setupCommands")) {
            setupComplete = true;
            return;
        }
        setupComplete = false;
        setupFailed = false;
        final MinecraftServer server = mc.getIntegratedServer();
        if (server == null) {
            throw new IllegalStateException("scenario has setup commands but no integrated server");
        }

        JsonArray commands = scenarioWorld.getAsJsonArray("setupCommands");
        final String[] toRun = new String[commands.size()];
        int i = 0;
        for (JsonElement element : commands) {
            toRun[i++] = element.getAsString();
        }

        // Queued onto the server thread: the command manager mutates world state, and this runs on
        // the render thread.
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                int failed = 0;
                String firstFailure = null;
                for (String command : toRun) {
                    // The command manager expects no leading slash.
                    String stripped = command.startsWith("/") ? command.substring(1) : command;
                    if (server.getCommandManager().executeCommand(server, stripped) == 0) {
                        failed++;
                        if (firstFailure == null) {
                            firstFailure = command;
                        }
                    }
                }
                // A partly-built scenario is worse than no scenario: it looks like a valid
                // measurement. An earlier run silently lost its occluding wall to a chunk-loading
                // race and reported a perfectly plausible "nothing was culled".
                if (failed > 0) {
                    ClientLogger.error("benchmark", failed + " of " + toRun.length
                            + " setup command(s) failed, first: " + firstFailure);
                    setupFailed = true;
                } else {
                    ClientLogger.info("benchmark", "ran " + toRun.length + " setup command(s)");
                }
                setupComplete = true;
            }
        });
    }
}
