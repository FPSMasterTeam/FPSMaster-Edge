package top.fpsmaster.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.CosmeticItem;
import top.fpsmaster.modules.client.api.model.CosmeticLoadoutView;
import top.fpsmaster.modules.client.api.model.ResolvedLoadoutView;
import top.fpsmaster.modules.logger.ClientLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes this account's cosmetic loadout and resolves everyone else's, so two FPSMaster players on
 * the same server see each other's cosmetics.
 *
 * <p>Reads are a batch resolve keyed by Minecraft UUID, refreshed when the world is entered, when the
 * tab list changes and at most once a minute otherwise; an entry is trusted for five minutes and the
 * whole cache is dropped on world leave or logout, so a stale loadout never outlives the session that
 * produced it.
 *
 * <p>Writes are deliberately asymmetric: equipping or toggling pushes at once because it is a discrete
 * decision, while the scale slider debounces so dragging it does not become one request per frame.
 * Nothing here reports success it did not observe — a failed push leaves local rendering exactly as it
 * was and moves {@link #status()} to {@link Status#FAILED}, which the cosmetics screen shows.
 */
public final class RemoteCosmeticService {
    /** Batch ceiling the backend enforces; a larger tab list resolves over consecutive passes. */
    private static final int MAX_BATCH = 200;
    private static final long CACHE_TTL_MS = 5L * 60_000L;
    private static final long REFRESH_INTERVAL_MS = 60_000L;
    private static final long PUSH_DEBOUNCE_MS = 500L;

    private static final RemoteCosmeticService INSTANCE = new RemoteCosmeticService();

    public enum Status {
        /** Nothing pushed yet this session. */
        IDLE,
        SYNCING,
        SYNCED,
        /** The push failed; local rendering is unchanged and nothing was stored server-side. */
        FAILED,
        /** No account, an offline session, or a backend without the loadout endpoint. */
        UNAVAILABLE
    }

    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean resolveInFlight = new AtomicBoolean(false);
    private final AtomicBoolean pushInFlight = new AtomicBoolean(false);

    private final Set<UUID> lastOnline = new HashSet<>();
    private volatile long lastFullRefreshAt;
    private volatile boolean hadWorld;

    private volatile long pushDueAt;
    private volatile boolean pushPending;
    private volatile Status status = Status.IDLE;
    private volatile String statusMessage = "";
    /** Set once any loadout round-trip succeeds; see {@link #syncStatusKey()}. */
    private volatile boolean everSucceeded;
    private volatile CompletableFuture<?> lastPush;

    private RemoteCosmeticService() {
    }

    public static RemoteCosmeticService getInstance() {
        return INSTANCE;
    }

    public Status status() {
        return status;
    }

    public String statusMessage() {
        return statusMessage;
    }

    /**
     * The cosmetics screen footer, in Prism's vocabulary: {@code ok}, {@code failed} or
     * {@code unavailable}.
     *
     * <p>This answers "does cloud sync work for this account", not "has the last write landed" —
     * so an in-flight push after an earlier success stays {@code ok} rather than flickering through
     * {@code unavailable}. It only ever reports {@code ok} once a round-trip has actually succeeded;
     * having pushed nothing yet is {@code unavailable}, never an optimistic success.
     */
    public String syncStatusKey() {
        if (status == Status.FAILED) {
            return "failed";
        }
        if (status == Status.SYNCED || everSucceeded && status == Status.SYNCING) {
            return "ok";
        }
        return "unavailable";
    }

    // ================== Reading other players ================== //

    /** The cached loadout for a Minecraft UUID, or null when unknown or older than the TTL. */
    public RemoteLoadout loadoutFor(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Entry entry = cache.get(uuid);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.fetchedAt > CACHE_TTL_MS) {
            cache.remove(uuid);
            return null;
        }
        return entry.loadout;
    }

    /** Called once a second from the client tick. */
    public void onClientTick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean hasWorld = minecraft.theWorld != null && minecraft.getNetHandler() != null;
        if (hadWorld && !hasWorld) {
            clearRemote();
        }
        boolean entered = hasWorld && !hadWorld;
        hadWorld = hasWorld;

        flushDuePush();

        if (!hasWorld || !AuthService.getInstance().isLoggedIn()) {
            return;
        }

        Set<UUID> online = onlinePlayers(minecraft);
        long now = System.currentTimeMillis();
        boolean periodic = now - lastFullRefreshAt >= REFRESH_INTERVAL_MS;
        boolean tabChanged = !online.equals(lastOnline);

        Set<UUID> wanted;
        if (entered || periodic) {
            wanted = new LinkedHashSet<>(online);
            lastFullRefreshAt = now;
        } else if (tabChanged) {
            wanted = new LinkedHashSet<>(online);
            wanted.removeAll(lastOnline);
        } else {
            // Steady state still has to pick up entries that aged out, and the remainder of a tab
            // list too large for one batch.
            wanted = new LinkedHashSet<>();
            for (UUID uuid : online) {
                if (loadoutFor(uuid) == null) {
                    wanted.add(uuid);
                }
            }
        }

        lastOnline.clear();
        lastOnline.addAll(online);
        if (!wanted.isEmpty()) {
            resolve(wanted);
        }
    }

    private Set<UUID> onlinePlayers(Minecraft minecraft) {
        Set<UUID> online = new LinkedHashSet<>();
        Collection<NetworkPlayerInfo> infos = minecraft.getNetHandler().getPlayerInfoMap();
        if (infos == null) {
            return online;
        }
        for (NetworkPlayerInfo info : infos) {
            if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null) {
                continue;
            }
            online.add(info.getGameProfile().getId());
        }
        return online;
    }

    private void resolve(Collection<UUID> uuids) {
        if (!resolveInFlight.compareAndSet(false, true)) {
            return;
        }
        List<String> batch = new ArrayList<String>(Math.min(uuids.size(), MAX_BATCH));
        for (UUID uuid : uuids) {
            if (batch.size() >= MAX_BATCH) {
                break;
            }
            // UUID.toString() is already the canonical dashed lowercase form the endpoint requires;
            // it rejects anything else.
            batch.add(uuid.toString());
        }
        FPSMasterApiClient.getInstance().resolveLoadouts(batch).whenComplete((response, error) -> {
            try {
                if (error != null || response == null || !response.isSuccess() || response.getData() == null) {
                    // A resolve failure degrades to "nobody else has cosmetics", never to a wrong
                    // loadout, so it does not touch this account's own sync status. The next pass
                    // retries, since nothing was cached for those players.
                    return;
                }
                long now = System.currentTimeMillis();
                for (ResolvedLoadoutView entry : response.getData()) {
                    if (entry == null || entry.getLoadout() == null) {
                        continue;
                    }
                    UUID uuid = parseUuid(entry.getMinecraftUuid());
                    if (uuid != null) {
                        store(uuid, entry.getLoadout(), now);
                    }
                }
            } catch (Exception exception) {
                ClientLogger.warn("Failed to apply resolved cosmetic loadouts: " + exception.getMessage());
            } finally {
                resolveInFlight.set(false);
            }
        });
    }

    private void store(UUID uuid, CosmeticLoadoutView view, long now) {
        CosmeticManager cosmetics = CosmeticManager.getInstance();
        String capeId = register(cosmetics, view.getCapeItem(), view.getCapeItemId());
        String backId = register(cosmetics, view.getBackItem(), view.getBackItemId());
        String backCategory = view.getBackItem() != null
                ? view.getBackItem().getCategory().toLowerCase(Locale.ROOT) : "wings";
        cache.put(uuid, new Entry(new RemoteLoadout(
                capeId, backId, backCategory,
                view.isBuiltinWingsEnabled(), view.getWingScale(), view.isCapeAnimationEnabled()
        ), now));
    }

    private String register(CosmeticManager cosmetics, CosmeticItem item, String fallbackId) {
        return item == null ? fallbackId : cosmetics.registerRemoteOption(item);
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // ================== Publishing this account ================== //

    /** Equip and toggle changes: discrete decisions, pushed immediately. */
    public void publishNow() {
        schedule(0L);
    }

    /** Slider drags: coalesced so one gesture becomes one request. */
    public void publishDebounced() {
        schedule(PUSH_DEBOUNCE_MS);
    }

    /** Screen close: sends the final value rather than losing it to the debounce. */
    public void flush() {
        if (pushPending) {
            pushDueAt = 0L;
            flushDuePush();
        }
    }

    /**
     * Shutdown variant. The HTTP call runs on a daemon pool that the JVM will not wait for, so a
     * pending scale change would simply vanish when the game closes; this gives it a bounded moment
     * to land and then gives up rather than holding up the exit.
     */
    public void flushBlocking(long timeoutMs) {
        flush();
        CompletableFuture<?> push = lastPush;
        if (push == null) {
            return;
        }
        try {
            push.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            ClientLogger.warn("Cosmetic loadout was not flushed before shutdown: "
                    + exception.getClass().getSimpleName());
        }
    }

    private void schedule(long delayMs) {
        if (!AuthService.getInstance().isLoggedIn()) {
            status = Status.UNAVAILABLE;
            statusMessage = FPSMaster.i18n.get("cosmetics.sync.signedout");
            return;
        }
        // An offline session has no verifiable Minecraft identity to attach a loadout to, so there is
        // nothing to publish. The chosen cosmetics still render locally.
        if (!MinecraftLinkService.isOnlineSession()) {
            status = Status.UNAVAILABLE;
            statusMessage = FPSMaster.i18n.get("cosmetics.sync.offline");
            return;
        }
        pushPending = true;
        long due = System.currentTimeMillis() + delayMs;
        // An immediate push must not inherit an earlier debounced deadline.
        if (pushDueAt == 0L || due < pushDueAt) {
            pushDueAt = due;
        }
        if (delayMs == 0L) {
            flushDuePush();
        }
    }

    private void flushDuePush() {
        if (!pushPending || System.currentTimeMillis() < pushDueAt) {
            return;
        }
        if (!pushInFlight.compareAndSet(false, true)) {
            return;
        }
        pushPending = false;
        pushDueAt = 0L;
        status = Status.SYNCING;
        statusMessage = FPSMaster.i18n.get("cosmetics.sync.pending");

        CosmeticManager cosmetics = CosmeticManager.getInstance();
        CosmeticManager.CosmeticOption cape = cosmetics.selectedCape();
        CosmeticManager.CosmeticOption back = cosmetics.selectedWings();
        boolean builtin = CosmeticManager.BUILTIN_WINGS_ID.equals(back.getId());
        String capeId = remoteId(cape == null ? null : cape.getId());
        String backId = builtin ? null : remoteId(back.getId());
        boolean builtinEnabled = builtin && cosmetics.wingsEnabled();
        float wingScale = cosmetics.storedWingScale();
        boolean capeAnimation = cosmetics.capeAnimationEnabled();

        lastPush = FPSMasterApiClient.getInstance()
                .putCosmeticLoadout(capeId, backId, builtinEnabled, wingScale, capeAnimation)
                .whenComplete((response, error) -> {
                    pushInFlight.set(false);
                    if (error == null && response != null && response.isSuccess()) {
                        everSucceeded = true;
                        status = Status.SYNCED;
                        statusMessage = FPSMaster.i18n.get("cosmetics.sync.ok");
                        return;
                    }
                    // A backend without the endpoint answers 404: that is "unavailable", not "failed".
                    // Either way the local loadout keeps rendering exactly as it was chosen.
                    String message = error != null ? error.getMessage()
                            : response == null ? "" : response.getMessage();
                    boolean missing = message != null && message.contains("404");
                    status = missing ? Status.UNAVAILABLE : Status.FAILED;
                    // The backend's message is human-readable ("item 7 is not owned"), so it is worth
                    // surfacing rather than replacing with a generic failure line.
                    statusMessage = !missing && message != null && !message.isEmpty()
                            ? message
                            : FPSMaster.i18n.get(missing ? "cosmetics.sync.unavailable" : "cosmetics.sync.failed");
                    ClientLogger.warn("Cosmetic loadout sync failed: " + message);
                });
    }

    /**
     * Locally authored cosmetics have no server-side identity, so they publish as "nothing in that
     * slot" rather than as an id the backend would reject.
     */
    private static String remoteId(String id) {
        return id == null || id.startsWith("custom:") || id.startsWith("builtin:") ? null : id;
    }

    // ================== Lifecycle ================== //

    /** World leave: other players' loadouts stop being true the moment we stop sharing a server. */
    public void clearRemote() {
        cache.clear();
        lastOnline.clear();
        lastFullRefreshAt = 0L;
    }

    /** Logout: everything remote goes, including the sync status of an account we no longer hold. */
    public void onLogout() {
        clearRemote();
        pushPending = false;
        pushDueAt = 0L;
        status = Status.IDLE;
        statusMessage = "";
        everSucceeded = false;
        MinecraftLinkService.getInstance().reset();
    }

    /** Pulls this account's stored loadout so a second client starts from the account, not from disk. */
    public void pullOwnLoadout() {
        if (!AuthService.getInstance().isLoggedIn()) {
            return;
        }
        FPSMasterApiClient.getInstance().getCosmeticLoadout().whenComplete((response, error) -> {
            if (error != null || response == null || !response.isSuccess() || response.getData() == null) {
                return;
            }
            everSucceeded = true;
            status = Status.SYNCED;
            statusMessage = FPSMaster.i18n.get("cosmetics.sync.ok");
            CosmeticLoadoutView view = response.getData();
            Minecraft.getMinecraft().addScheduledTask(
                    () -> CosmeticManager.getInstance().applyAccountLoadout(view));
        });
    }

    private static final class Entry {
        private final RemoteLoadout loadout;
        private final long fetchedAt;

        private Entry(RemoteLoadout loadout, long fetchedAt) {
            this.loadout = loadout;
            this.fetchedAt = fetchedAt;
        }
    }

    /** A resolved loadout in the terms the renderers need: cosmetic option ids and a real scale. */
    public static final class RemoteLoadout {
        public final String capeItemId;
        public final String backItemId;
        public final String backCategory;
        public final boolean builtinWingsEnabled;
        public final float wingScale;
        public final boolean capeAnimationEnabled;

        RemoteLoadout(String capeItemId, String backItemId, String backCategory,
                      boolean builtinWingsEnabled, float wingScale, boolean capeAnimationEnabled) {
            this.capeItemId = capeItemId;
            this.backItemId = backItemId;
            this.backCategory = backCategory;
            this.builtinWingsEnabled = builtinWingsEnabled;
            this.wingScale = wingScale;
            this.capeAnimationEnabled = capeAnimationEnabled;
        }
    }
}
