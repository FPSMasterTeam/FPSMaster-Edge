package top.fpsmaster.ui.mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which of the player's own servers are pinned to the top of the multiplayer list.
 *
 * <p>Stored in the file that used to also hold dismissed promoted servers, under the same key, so
 * pins made with older builds carry over. The promoted-server list itself came from the FPSMaster
 * servers and is gone.
 */
final class ServerPins {
    private static final String FILE = "promoted_servers.json";
    private static final String PINNED_KEY = "pinnedAddresses";
    private static final Set<String> PINNED = new HashSet<>();
    private static boolean loaded;

    private ServerPins() {
    }

    static synchronized boolean isPinned(String address) {
        String key = normalize(address);
        if (key.isEmpty()) return false;
        ensureLoaded();
        return PINNED.contains(key);
    }

    static synchronized void setPinned(String address, boolean pinned) {
        String key = normalize(address);
        if (key.isEmpty()) return;
        ensureLoaded();
        boolean changed = pinned ? PINNED.add(key) : PINNED.remove(key);
        if (changed) save();
    }

    private static String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            String content = FileUtils.readFile(FILE);
            if (content == null || content.trim().isEmpty()) return;
            JsonElement root = new JsonParser().parse(content);
            if (root == null || !root.isJsonObject()) return;
            JsonElement pinned = root.getAsJsonObject().get(PINNED_KEY);
            if (pinned == null || !pinned.isJsonArray()) return;
            for (JsonElement element : pinned.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    String key = normalize(element.getAsString());
                    if (!key.isEmpty()) PINNED.add(key);
                }
            }
        } catch (FileException e) {
            ClientLogger.warn("Failed to read pinned servers: " + e.getMessage());
        } catch (RuntimeException e) {
            ClientLogger.warn("Pinned servers file is invalid, starting fresh: " + e.getMessage());
        }
    }

    private static void save() {
        JsonArray pinned = new JsonArray();
        for (String key : PINNED) pinned.add(new JsonPrimitive(key));
        JsonObject root = new JsonObject();
        root.add(PINNED_KEY, pinned);
        try {
            FileUtils.saveFile(FILE, root.toString());
        } catch (FileException e) {
            ClientLogger.error("Failed to save pinned servers: " + e.getMessage());
        }
    }
}
