package top.fpsmaster.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Runs a scenario's one-off world setup once the world is loaded.
 *
 * <p>Expressed as server commands rather than direct world manipulation: commands go through the
 * integrated server, so entities exist server-side and behave the way they would in a real session.
 * Spawning client-side only would produce entities the server immediately removes.
 *
 * <p>Determinism is the point. Left alone, a world drifts: the sun moves, weather rolls in, mobs
 * spawn and despawn on their own schedule. Every scenario should pin time, weather and mob spawning
 * so that two runs half an hour apart are rendering the same thing.
 */
public final class BenchSetup {

    private BenchSetup() {
    }

    public static void run(Minecraft mc, JsonObject scenarioWorld) {
        if (scenarioWorld == null || !scenarioWorld.has("setupCommands")) {
            return;
        }
        JsonArray commands = scenarioWorld.getAsJsonArray("setupCommands");
        for (JsonElement element : commands) {
            String command = element.getAsString();
            mc.thePlayer.sendChatMessage(command);
        }
        ClientLogger.info("benchmark", "ran " + commands.size() + " setup command(s)");
    }
}
