package top.fpsmaster.cosmetic;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.MinecraftLinkChallenge;
import top.fpsmaster.modules.logger.ClientLogger;

import java.util.UUID;

/**
 * Proves ownership of the running Minecraft account to the FPSMaster backend, which is what makes this
 * player visible to other FPSMaster clients on the same server.
 *
 * <p>The client never tells the backend its UUID. It asks for a challenge, joins Mojang's session
 * server with the challenge's {@code serverId} using the session it already holds, and the backend
 * then asks Mojang who joined. The session access token is used exactly once, is passed straight to
 * Mojang and is never logged, stored or sent to FPSMaster.
 *
 * <p>Offline sessions cannot complete this handshake, so they are skipped outright: their loadout
 * still renders locally and still syncs to their FPSMaster account, they are simply not published to
 * other players.
 */
public final class MinecraftLinkService {
    private static final MinecraftLinkService INSTANCE = new MinecraftLinkService();

    public enum State {
        IDLE,
        /** Offline session — no Mojang identity to prove. */
        SKIPPED_OFFLINE,
        LINKING,
        LINKED,
        FAILED
    }

    private volatile State state = State.IDLE;
    private volatile String message = "";
    private volatile boolean attempted;

    private MinecraftLinkService() {
    }

    public static MinecraftLinkService getInstance() {
        return INSTANCE;
    }

    public State state() {
        return state;
    }

    public String message() {
        return message;
    }

    /**
     * A Mojang session has a version-4 (random) profile UUID and a real access token; an offline
     * launcher produces a version-3 UUID derived from the name, with a placeholder token.
     */
    public static boolean isOnlineSession() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getSession() == null) {
            return false;
        }
        GameProfile profile = minecraft.getSession().getProfile();
        UUID id = profile == null ? null : profile.getId();
        String token = minecraft.getSession().getToken();
        return id != null && id.version() == 4 && token != null && token.length() > 8;
    }

    /** Runs the handshake once per session; repeated calls after a success are no-ops. */
    public void linkIfNeeded() {
        if (attempted || !AuthService.getInstance().isLoggedIn()) {
            return;
        }
        attempted = true;
        if (!isOnlineSession()) {
            state = State.SKIPPED_OFFLINE;
            message = FPSMaster.i18n.get("cosmetics.link.offline");
            return;
        }
        state = State.LINKING;
        message = FPSMaster.i18n.get("cosmetics.link.pending");
        FPSMasterApiClient.getInstance().requestMinecraftLinkChallenge().whenComplete((response, error) -> {
            if (error != null || response == null || !response.isSuccess() || response.getData() == null) {
                fail(error != null ? error.getMessage() : response == null ? "" : response.getMessage());
                return;
            }
            joinAndConfirm(response.getData());
        });
    }

    /** Allows a retry after a failure — a new sign-in, for example. */
    public void reset() {
        attempted = false;
        state = State.IDLE;
        message = "";
    }

    private void joinAndConfirm(MinecraftLinkChallenge challenge) {
        if (!challenge.isWellFormed()) {
            fail("malformed challenge");
            return;
        }
        FPSMaster.async.runnable(() -> {
            Minecraft minecraft = Minecraft.getMinecraft();
            GameProfile profile = minecraft.getSession().getProfile();
            try {
                minecraft.getSessionService().joinServer(
                        profile, minecraft.getSession().getToken(), challenge.getServerId());
            } catch (Exception exception) {
                // Deliberately reports the exception type only: the message can echo request details.
                fail(exception.getClass().getSimpleName());
                return;
            }
            FPSMasterApiClient.getInstance()
                    .confirmMinecraftLink(challenge.getChallengeId(), profile.getName())
                    .whenComplete((response, error) -> {
                        if (error == null && response != null && response.isSuccess()) {
                            state = State.LINKED;
                            message = FPSMaster.i18n.get("cosmetics.link.ok");
                            return;
                        }
                        fail(error != null ? error.getMessage() : response == null ? "" : response.getMessage());
                    });
        });
    }

    private void fail(String reason) {
        state = State.FAILED;
        message = FPSMaster.i18n.get("cosmetics.link.failed");
        ClientLogger.warn("Minecraft account link failed: " + (reason == null ? "unknown" : reason));
    }
}
