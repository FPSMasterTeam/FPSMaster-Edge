package top.fpsmaster.modules.client.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Session;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.config.Configure;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.statistics.TelemetryIdentityStatistics;
import top.fpsmaster.utils.io.HttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class EdgeTelemetryReporter {
    private static final long HEARTBEAT_INTERVAL_MS = 90_000L;
    private static final long PRESENCE_INTERVAL_MS = 180_000L;
    private static final String TELEMETRY_URL = "https://api.fpsmaster.top/api/v1/telemetry/heartbeat";
    private static final String PRESENCE_URL = "https://api.fpsmaster.top/api/v1/telemetry/presence";
    private static final String OFFLINE_URL = "https://api.fpsmaster.top/api/v1/telemetry/offline";
    private static final String CLIENT_NAME = "FPSMaster-Edge";
    private static final int MAX_SAMPLED_PLAYERS = 8;
    // Fixed salt so identity hashes are stable across sessions (enables unique-user counting)
    // while not being trivially reversible via a bare-hash rainbow table.
    private static final String IDENTITY_SALT = "fpsmaster-edge-telemetry-v1";

    private final AtomicBoolean heartbeatInFlight = new AtomicBoolean(false);
    private final AtomicBoolean presenceInFlight = new AtomicBoolean(false);
    private volatile boolean multiplayerActive;
    private volatile long lastHeartbeatAt;
    private volatile long lastPresenceAt;
    private volatile String lastPresenceServerHash;

    public void tick(long now) {
        if (!isTelemetryEnabled()) {
            clearState();
            return;
        }

        boolean activeNow = isRemoteMultiplayerActive();
        if (!activeNow) {
            if (multiplayerActive) {
                multiplayerActive = false;
                submitOffline(false);
                clearState();
            }
            multiplayerActive = false;
            return;
        }

        boolean shouldSend = !multiplayerActive || now - lastHeartbeatAt >= HEARTBEAT_INTERVAL_MS;
        PresenceSnapshot presenceSnapshot = capturePresenceSnapshot();
        boolean shouldSendPresence = presenceSnapshot != null && (
                !multiplayerActive
                        || now - lastPresenceAt >= PRESENCE_INTERVAL_MS
                        || !presenceSnapshot.serverIdHash.equals(lastPresenceServerHash)
        );
        multiplayerActive = true;
        if (shouldSend) {
            submitHeartbeat(now);
        }
        if (shouldSendPresence) {
            submitPresence(now, presenceSnapshot);
        }
    }

    public void shutdown() {
        if (multiplayerActive) {
            submitOffline(true);
        }
        clearState();
    }

    private void submitHeartbeat(long now) {
        if (!heartbeatInFlight.compareAndSet(false, true)) {
            return;
        }

        JsonObject payload = buildHeartbeatPayload();
        if (payload == null) {
            heartbeatInFlight.set(false);
            return;
        }

        FPSMaster.async.runnable(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(TELEMETRY_URL, payload);
                if (response.isSuccess()) {
                    lastHeartbeatAt = now;
                } else {
                    ClientLogger.warn("Edge heartbeat failed with status " + response.getStatusCode());
                }
            } catch (IOException exception) {
                ClientLogger.warn("Edge heartbeat failed: " + exception.getMessage());
            } finally {
                heartbeatInFlight.set(false);
            }
        });
    }

    private void submitPresence(long now, PresenceSnapshot snapshot) {
        if (snapshot == null || !presenceInFlight.compareAndSet(false, true)) {
            return;
        }

        JsonObject payload = buildPresencePayload(snapshot);
        if (payload == null) {
            presenceInFlight.set(false);
            return;
        }

        FPSMaster.async.runnable(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(PRESENCE_URL, payload);
                if (response.isSuccess()) {
                    lastPresenceAt = now;
                    lastPresenceServerHash = snapshot.serverIdHash;
                } else {
                    ClientLogger.warn("Edge presence failed with status " + response.getStatusCode());
                }
            } catch (IOException exception) {
                ClientLogger.warn("Edge presence failed: " + exception.getMessage());
            } finally {
                presenceInFlight.set(false);
            }
        });
    }

    private void submitOffline(boolean blocking) {
        JsonObject payload = buildOfflinePayload();
        if (payload == null) {
            return;
        }

        Runnable request = () -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(OFFLINE_URL, payload);
                if (!response.isSuccess()) {
                    ClientLogger.warn("Edge offline failed with status " + response.getStatusCode());
                }
            } catch (IOException exception) {
                ClientLogger.warn("Edge offline failed: " + exception.getMessage());
            }
        };

        if (blocking) {
            request.run();
            return;
        }
        FPSMaster.async.runnable(request);
    }

    private JsonObject buildHeartbeatPayload() {
        Configure configure = FPSMaster.configManager.configure;
        if (configure == null) {
            return null;
        }

        String sessionId = normalize(TelemetryIdentityStatistics.getInstanceId());
        if (sessionId == null) {
            return null;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("clientName", CLIENT_NAME);
        payload.addProperty("clientKind", FPSMaster.EDITION.toUpperCase());
        payload.addProperty("sessionId", sessionId);
        payload.addProperty("clientVersion", FPSMaster.CLIENT_VERSION);

        // Send only de-identified, salted hashes of account identity — never the raw
        // username or Mojang UUID — so the "anonymous data" toggle actually stays anonymous.
        Session session = Minecraft.getMinecraft().getSession();
        if (session != null) {
            String username = normalize(session.getUsername());
            if (username != null) {
                payload.addProperty("usernameHash", hashIdentity(username.toLowerCase(Locale.ROOT)));
            }
        }

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && player.getUniqueID() != null) {
            payload.addProperty("playerUuidHash", hashIdentity(player.getUniqueID().toString().toLowerCase(Locale.ROOT)));
        }
        return payload;
    }

    private JsonObject buildPresencePayload(PresenceSnapshot snapshot) {
        Configure configure = FPSMaster.configManager.configure;
        if (configure == null || snapshot == null) {
            return null;
        }

        String sessionId = normalize(TelemetryIdentityStatistics.getInstanceId());
        if (sessionId == null) {
            return null;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("clientName", CLIENT_NAME);
        payload.addProperty("clientKind", FPSMaster.EDITION.toUpperCase(Locale.ROOT));
        payload.addProperty("sessionId", sessionId);
        payload.addProperty("instanceId", sessionId);
        payload.addProperty("platform", normalizePlatform());
        payload.addProperty("serverIpMasked", snapshot.serverIpMasked);
        payload.addProperty("serverIdHash", snapshot.serverIdHash);
        payload.addProperty("serverPlayerCount", snapshot.serverPlayerCount);
        payload.addProperty("sampledPlayersPayload", snapshot.sampledPlayersPayload);
        payload.addProperty("payloadJson", snapshot.payloadJson);
        payload.addProperty("clientVersion", FPSMaster.CLIENT_VERSION);
        return payload;
    }

    private JsonObject buildOfflinePayload() {
        Configure configure = FPSMaster.configManager.configure;
        if (configure == null) {
            return null;
        }

        String sessionId = normalize(TelemetryIdentityStatistics.getInstanceId());
        if (sessionId == null) {
            return null;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("clientName", CLIENT_NAME);
        payload.addProperty("clientKind", FPSMaster.EDITION.toUpperCase(Locale.ROOT));
        payload.addProperty("sessionId", sessionId);
        return payload;
    }

    private boolean isTelemetryEnabled() {
        Configure configure = FPSMaster.configManager.configure;
        return configure != null && configure.anonymousDataEnabled;
    }

    private boolean isRemoteMultiplayerActive() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null
                && mc.theWorld != null
                && mc.thePlayer != null
                && !mc.isIntegratedServerRunning();
    }

    private PresenceSnapshot capturePresenceSnapshot() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getCurrentServerData() == null || mc.getNetHandler() == null) {
            return null;
        }

        ServerData serverData = mc.getCurrentServerData();
        String rawServerAddress = normalize(serverData.serverIP);
        if (rawServerAddress == null) {
            return null;
        }

        String normalizedServerAddress = rawServerAddress.toLowerCase(Locale.ROOT);
        String serverIdHash = sha256Hex(normalizedServerAddress);
        if (serverIdHash == null) {
            return null;
        }

        Collection<NetworkPlayerInfo> playerInfos = mc.getNetHandler().getPlayerInfoMap();
        int serverPlayerCount = playerInfos == null ? 0 : playerInfos.size();
        String sampledPlayersPayload = buildSampledPlayersPayload(playerInfos, serverIdHash);

        JsonObject payload = new JsonObject();
        payload.addProperty("tabPlayerCount", serverPlayerCount);
        if (mc.thePlayer != null && mc.thePlayer.dimension != 0) {
            payload.addProperty("dimension", mc.thePlayer.dimension);
        }

        return new PresenceSnapshot(
                maskServerAddress(rawServerAddress),
                serverIdHash,
                serverPlayerCount,
                sampledPlayersPayload,
                payload.toString()
        );
    }

    private String buildSampledPlayersPayload(Collection<NetworkPlayerInfo> playerInfos, String serverIdHash) {
        JsonArray samples = new JsonArray();
        if (playerInfos == null) {
            return samples.toString();
        }

        int count = 0;
        for (NetworkPlayerInfo info : playerInfos) {
            if (info == null || info.getGameProfile() == null) {
                continue;
            }
            String name = normalize(info.getGameProfile().getName());
            if (name == null) {
                continue;
            }

            JsonObject sample = new JsonObject();
            sample.addProperty("playerHash", shortenHash(sha256Hex(serverIdHash + ":" + name.toLowerCase(Locale.ROOT)), 16));
            sample.addProperty("self", Minecraft.getMinecraft().thePlayer != null
                    && info.getGameProfile().getId() != null
                    && info.getGameProfile().getId().equals(Minecraft.getMinecraft().thePlayer.getUniqueID()));
            samples.add(sample);
            count++;
            if (count >= MAX_SAMPLED_PLAYERS) {
                break;
            }
        }
        return samples.toString();
    }

    private String normalizePlatform() {
        return normalize(System.getProperty("os.name"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskServerAddress(String rawAddress) {
        String address = normalize(rawAddress);
        if (address == null) {
            return null;
        }

        String host = address;
        String port = null;
        int portSeparator = address.lastIndexOf(':');
        if (portSeparator > 0 && address.indexOf(':') == portSeparator) {
            host = address.substring(0, portSeparator);
            port = address.substring(portSeparator + 1);
        }

        String maskedHost;
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] segments = host.split("\\.");
            maskedHost = segments[0] + ".*.*." + segments[segments.length - 1];
        } else {
            String[] labels = host.split("\\.");
            List<String> maskedLabels = new ArrayList<>();
            for (String label : labels) {
                if (label.isEmpty()) {
                    continue;
                }
                if (label.length() == 1) {
                    maskedLabels.add("*");
                } else {
                    maskedLabels.add(label.substring(0, 1) + repeat('*', label.length() - 1));
                }
            }
            maskedHost = String.join(".", maskedLabels);
        }

        if (port == null || port.isEmpty()) {
            return maskedHost;
        }
        return maskedHost + ":****";
    }

    private String repeat(char character, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(character);
        }
        return builder.toString();
    }

    private String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            ClientLogger.warn("SHA-256 is unavailable: " + exception.getMessage());
            return null;
        }
    }

    private String hashIdentity(String value) {
        if (value == null) {
            return null;
        }
        return sha256Hex(IDENTITY_SALT + ":" + value);
    }

    private String shortenHash(String value, int length) {
        if (value == null) {
            return null;
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private void clearState() {
        multiplayerActive = false;
        lastHeartbeatAt = 0L;
        lastPresenceAt = 0L;
        lastPresenceServerHash = null;
    }

    private static class PresenceSnapshot {
        private final String serverIpMasked;
        private final String serverIdHash;
        private final int serverPlayerCount;
        private final String sampledPlayersPayload;
        private final String payloadJson;

        private PresenceSnapshot(
                String serverIpMasked,
                String serverIdHash,
                int serverPlayerCount,
                String sampledPlayersPayload,
                String payloadJson
        ) {
            this.serverIpMasked = serverIpMasked;
            this.serverIdHash = serverIdHash;
            this.serverPlayerCount = serverPlayerCount;
            this.sampledPlayersPayload = sampledPlayersPayload;
            this.payloadJson = payloadJson;
        }
    }
}
