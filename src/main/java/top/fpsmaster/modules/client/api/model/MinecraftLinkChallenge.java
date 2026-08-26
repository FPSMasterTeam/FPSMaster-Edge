package top.fpsmaster.modules.client.api.model;

/** Response of {@code POST /me/minecraft-links/challenge}. The {@code serverId} is what gets joined. */
public class MinecraftLinkChallenge {
    /** The backend mints the server id as 40 hex characters; see {@link #isWellFormed()}. */
    private static final int SERVER_ID_LENGTH = 40;

    private String challengeId;
    private String serverId;
    private String expiresAt;

    public String getChallengeId() {
        return challengeId == null ? "" : challengeId;
    }

    public String getServerId() {
        return serverId == null ? "" : serverId;
    }

    public String getExpiresAt() {
        return expiresAt == null ? "" : expiresAt;
    }

    /**
     * Whether this challenge is worth acting on.
     *
     * <p>The server id is handed to Mojang's session server together with the real session token, so
     * its shape is checked before it is used rather than after: a truncated or garbled response
     * should fail the link locally instead of turning into an authenticated join for an arbitrary
     * string.
     */
    public boolean isWellFormed() {
        if (getChallengeId().isEmpty() || getServerId().length() != SERVER_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < SERVER_ID_LENGTH; i++) {
            char character = serverId.charAt(i);
            boolean hex = character >= '0' && character <= '9'
                    || character >= 'a' && character <= 'f'
                    || character >= 'A' && character <= 'F';
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}
