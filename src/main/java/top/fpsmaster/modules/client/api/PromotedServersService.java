package top.fpsmaster.modules.client.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.client.api.model.ApiResponse;
import top.fpsmaster.modules.client.api.model.FPSMasterGson;
import top.fpsmaster.modules.client.api.model.PromotedServerView;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.io.HttpRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Featured/partner servers for the multiplayer screen, backed by
 * {@code GET /api/v1/launcher/servers}, plus the player's local overrides: promoted entries the
 * player deleted (never inserted again) and the player's own pinned servers.
 *
 * <p>The fetch is fully asynchronous — the GUI only ever reads the cached snapshot via
 * {@link #promotedServers()} and watches {@link #revision()} for changes, so an offline client
 * simply keeps an empty promoted list and the local server list stays usable.
 *
 * <p>Overrides persist in a sidecar JSON ({@code promoted_servers.json} in the client data dir);
 * {@code servers.dat} is never touched with new keys.
 */
public final class PromotedServersService {
    private static final PromotedServersService INSTANCE = new PromotedServersService();
    private static final String SIDECAR_FILE = "promoted_servers.json";
    private static final String HIDDEN_KEY = "hiddenPromoted";
    private static final String PINNED_KEY = "pinnedAddresses";
    /** Hidden entries without a backend id fall back to the address, marked with this prefix. */
    private static final String ADDRESS_KEY_PREFIX = "addr:";
    private static final long REFRESH_INTERVAL_MS = 60_000L;
    private static final String SERVERS_ENDPOINT =
            FPSMasterConstants.API_BASE_URL + FPSMasterConstants.API_VERSION + "/launcher/servers";

    private volatile List<PromotedServerView> promoted = Collections.emptyList();
    private final AtomicLong revision = new AtomicLong(0);
    private final AtomicBoolean fetching = new AtomicBoolean(false);
    private volatile long lastFetchAt = 0L;

    private final Object stateLock = new Object();
    private boolean stateLoaded;
    private final Set<String> hiddenKeys = new HashSet<>();
    private final Set<String> pinnedAddresses = new HashSet<>();

    private PromotedServersService() {
    }

    public static PromotedServersService getInstance() {
        return INSTANCE;
    }

    /** Bumped once per successful fetch; the GUI rebuilds its entry list when it changes. */
    public long revision() {
        return revision.get();
    }

    /** Cached snapshot in backend order; never blocks, empty until a fetch succeeds. */
    public List<PromotedServerView> promotedServers() {
        return promoted;
    }

    /**
     * Kicks off a background fetch unless one is in flight or a recent one already ran.
     * Safe to call from {@code initGui()} — resizing the window re-runs it every time.
     */
    public void refreshIfStale() {
        long now = System.currentTimeMillis();
        long last = lastFetchAt;
        if (last != 0L && now - last >= 0L && now - last < REFRESH_INTERVAL_MS) {
            return;
        }
        if (!fetching.compareAndSet(false, true)) {
            return;
        }
        lastFetchAt = now;
        CompletableFuture.runAsync(this::fetch).whenComplete((unused, error) -> {
            fetching.set(false);
            if (error != null) {
                ClientLogger.warn("Promoted servers refresh failed: " + error);
            }
        });
    }

    private void fetch() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", FPSMasterConstants.USER_AGENT);
            headers.put("Accept", "application/json");
            HttpRequest.HttpResponseResult response = HttpRequest.get(SERVERS_ENDPOINT, headers);
            if (!response.isSuccess() || response.getBody() == null || response.getBody().isEmpty()) {
                ClientLogger.warn("Promoted servers request failed: HTTP " + response.getStatusCode());
                return;
            }
            JsonObject json = FPSMasterGson.getInstance().fromJson(response.getBody(), JsonObject.class);
            ApiResponse<PromotedServerView[]> apiResponse =
                    ApiResponse.fromJson(json, PromotedServerView[].class, response.getStatusCode());
            if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                ClientLogger.warn("Promoted servers response rejected: " + apiResponse.getMessage());
                return;
            }
            List<PromotedServerView> fetched = new ArrayList<>();
            for (PromotedServerView view : apiResponse.getData()) {
                if (view != null && view.getAddress() != null && !view.getAddress().trim().isEmpty()) {
                    fetched.add(view);
                }
            }
            promoted = Collections.unmodifiableList(fetched);
            revision.incrementAndGet();
        } catch (IOException e) {
            ClientLogger.warn("Promoted servers request failed: " + e.getMessage());
        } catch (RuntimeException e) {
            ClientLogger.warn("Promoted servers response could not be parsed: " + e.getMessage());
        }
    }

    // ================== Local overrides ================== //

    public boolean isHidden(PromotedServerView view) {
        synchronized (stateLock) {
            ensureStateLoaded();
            return hiddenKeys.contains(hideKey(view));
        }
    }

    /**
     * Records the promoted server matching {@code address} as deleted, so it is never inserted
     * into the list again. Prefers the backend id; falls back to the address when absent.
     */
    public void hideByAddress(String address) {
        String normalized = normalizeAddress(address);
        if (normalized.isEmpty()) {
            return;
        }
        String key = ADDRESS_KEY_PREFIX + normalized;
        for (PromotedServerView view : promoted) {
            if (normalizeAddress(view.getAddress()).equals(normalized)) {
                key = hideKey(view);
                break;
            }
        }
        synchronized (stateLock) {
            ensureStateLoaded();
            if (hiddenKeys.add(key)) {
                saveState();
            }
        }
    }

    public boolean isPinned(String address) {
        String normalized = normalizeAddress(address);
        if (normalized.isEmpty()) {
            return false;
        }
        synchronized (stateLock) {
            ensureStateLoaded();
            return pinnedAddresses.contains(normalized);
        }
    }

    /** Pin state for the player's own servers, keyed by address. */
    public void setPinned(String address, boolean pinned) {
        String normalized = normalizeAddress(address);
        if (normalized.isEmpty()) {
            return;
        }
        synchronized (stateLock) {
            ensureStateLoaded();
            boolean changed = pinned ? pinnedAddresses.add(normalized) : pinnedAddresses.remove(normalized);
            if (changed) {
                saveState();
            }
        }
    }

    public static String normalizeAddress(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    private String hideKey(PromotedServerView view) {
        if (view.getId() != null && !view.getId().trim().isEmpty()) {
            return view.getId().trim();
        }
        return ADDRESS_KEY_PREFIX + normalizeAddress(view.getAddress());
    }

    /** Callers hold {@code stateLock}. */
    private void ensureStateLoaded() {
        if (stateLoaded) {
            return;
        }
        stateLoaded = true;
        try {
            String content = FileUtils.readFile(SIDECAR_FILE);
            if (content == null || content.trim().isEmpty()) {
                return;
            }
            JsonObject json = FPSMasterGson.getInstance().fromJson(content, JsonObject.class);
            if (json == null) {
                return;
            }
            readStrings(json, HIDDEN_KEY, hiddenKeys);
            readStrings(json, PINNED_KEY, pinnedAddresses);
        } catch (FileException e) {
            ClientLogger.warn("Failed to read promoted servers state: " + e.getMessage());
        } catch (RuntimeException e) {
            ClientLogger.warn("Promoted servers state is invalid JSON, starting fresh: " + e.getMessage());
        }
    }

    private void readStrings(JsonObject json, String key, Set<String> target) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return;
        }
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (value != null && !value.isEmpty()) {
                    target.add(value);
                }
            }
        }
    }

    /** Callers hold {@code stateLock}. */
    private void saveState() {
        JsonObject json = new JsonObject();
        JsonArray hidden = new JsonArray();
        for (String key : hiddenKeys) {
            hidden.add(new com.google.gson.JsonPrimitive(key));
        }
        json.add(HIDDEN_KEY, hidden);
        JsonArray pinned = new JsonArray();
        for (String address : pinnedAddresses) {
            pinned.add(new com.google.gson.JsonPrimitive(address));
        }
        json.add(PINNED_KEY, pinned);
        try {
            FileUtils.saveFile(SIDECAR_FILE, FPSMasterGson.getInstance().toJson(json));
        } catch (FileException e) {
            ClientLogger.error("Failed to save promoted servers state: " + e.getMessage());
        }
    }
}
