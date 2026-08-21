package top.fpsmaster.modules.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import top.fpsmaster.exception.AccountException;
import top.fpsmaster.utils.io.HttpRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minecraft Java Microsoft login, same Azure app and token chain as the FPSMaster launcher.
 *
 * <p>Public client (no secret): device-code + Xbox Live + XSTS + Minecraft Services.
 * Client id defaults to the launcher builtin; override with
 * {@code FPSMASTER_MINECRAFT_CLIENT_ID} or {@code MICROSOFT_CLIENT_ID}.
 */
public final class MicrosoftAuth {
    /** Same builtin as {@code FPSMaster-Launcher/.../microsoft_auth.rs}. */
    public static final String DEFAULT_CLIENT_ID = "057064c6-d180-43df-b010-834b4571532f";
    public static final String SCOPE = "XboxLive.signin offline_access openid profile email";

    private static final String DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL =
            "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_ENTITLEMENTS_URL =
            "https://api.minecraftservices.com/entitlements/mcstore";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private MicrosoftAuth() {
    }

    public static String clientId() {
        String env = firstNonEmpty(
                System.getenv("FPSMASTER_MINECRAFT_CLIENT_ID"),
                System.getenv("MICROSOFT_CLIENT_ID"),
                System.getProperty("fpsmaster.minecraft.clientId"),
                System.getProperty("microsoft.clientId"));
        return env != null ? env : DEFAULT_CLIENT_ID;
    }

    public static DeviceLogin startDeviceLogin() throws IOException, AccountException {
        Map<String, String> form = new HashMap<String, String>();
        form.put("client_id", clientId());
        form.put("scope", SCOPE);
        HttpRequest.HttpResponseResult response = HttpRequest.postForm(DEVICE_CODE_URL, form);
        if (!response.isSuccess()) {
            throw new AccountException(parseError(response.getBody(), "Failed to start Microsoft device login"));
        }
        return parseDeviceLogin(response.getBody());
    }

    public static PollResult pollDeviceLogin(String deviceCode) throws IOException, AccountException {
        if (deviceCode == null || deviceCode.trim().isEmpty()) {
            throw new AccountException("Minecraft device code is empty");
        }
        Map<String, String> form = new HashMap<String, String>();
        form.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        form.put("client_id", clientId());
        form.put("device_code", deviceCode.trim());
        HttpRequest.HttpResponseResult response = HttpRequest.postForm(TOKEN_URL, form);
        if (response.isSuccess()) {
            JsonObject token = asObject(response.getBody());
            String access = stringField(token, "access_token");
            String refresh = optionalString(token, "refresh_token");
            if (access.isEmpty()) {
                throw new AccountException("Microsoft token response did not include an access token");
            }
            MinecraftProfile account = complete(access, refresh);
            return PollResult.completed(account);
        }
        JsonObject errorJson = tryObject(response.getBody());
        String errorCode = errorJson == null ? "" : optionalString(errorJson, "error").toLowerCase(Locale.ROOT);
        String description = errorJson == null ? "" : optionalString(errorJson, "error_description");
        String classified = classifyTokenError(errorCode);
        if ("pending".equals(classified)) {
            return PollResult.pending(5, firstNonEmpty(description, "Waiting for Microsoft confirmation."));
        }
        if ("slow_down".equals(classified)) {
            return PollResult.pending(8, firstNonEmpty(description, "Waiting for Microsoft confirmation."));
        }
        if ("denied".equals(classified)) {
            return PollResult.denied(firstNonEmpty(description, "Microsoft device login was cancelled."));
        }
        if ("expired".equals(classified)) {
            return PollResult.expired(firstNonEmpty(description, "Microsoft device login code expired."));
        }
        throw new AccountException(firstNonEmpty(description, parseError(response.getBody(), "Microsoft device login failed")));
    }

    public static MinecraftProfile refresh(String refreshToken) throws IOException, AccountException {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AccountException("Minecraft refresh token is empty");
        }
        Map<String, String> form = new HashMap<String, String>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId());
        form.put("refresh_token", refreshToken.trim());
        form.put("scope", SCOPE);
        HttpRequest.HttpResponseResult response = HttpRequest.postForm(TOKEN_URL, form);
        if (!response.isSuccess()) {
            throw new AccountException(parseError(response.getBody(),
                    "Microsoft premium account refresh failed. Please sign in again."));
        }
        JsonObject token = asObject(response.getBody());
        String access = stringField(token, "access_token");
        String nextRefresh = optionalString(token, "refresh_token");
        if (nextRefresh.isEmpty()) {
            nextRefresh = refreshToken.trim();
        }
        if (access.isEmpty()) {
            throw new AccountException("Microsoft refresh response did not include an access token");
        }
        return complete(access, nextRefresh);
    }

    public static MinecraftProfile complete(String microsoftAccessToken, String refreshToken)
            throws IOException, AccountException {
        XboxAuth xbox = authenticateXbox(microsoftAccessToken);
        XboxAuth xsts = authorizeXsts(xbox.token);
        String userHash = firstNonEmpty(xsts.uhs, xbox.uhs);
        if (userHash == null) {
            throw new AccountException("Xbox Live authentication response did not include a user hash");
        }
        MinecraftLogin mc = loginMinecraft(userHash, xsts.token);
        assertMinecraftLicense(mc.accessToken);
        JsonObject profile = fetchProfile(mc.accessToken);
        String name = stringField(profile, "name");
        String uuid = dashedUuid(stringField(profile, "id"));
        if (name.isEmpty() || uuid.isEmpty()) {
            throw new AccountException("Minecraft profile response was incomplete");
        }
        MinecraftProfile result = new MinecraftProfile();
        result.name = name;
        result.uuid = uuid;
        result.accessToken = mc.accessToken;
        result.refreshToken = refreshToken;
        result.xuid = firstNonEmpty(xsts.xuid, xbox.xuid);
        result.expiresAt = System.currentTimeMillis() + mc.expiresIn * 1000L;
        return result;
    }

    public static String dashedUuid(String raw) {
        if (raw == null) {
            return "";
        }
        String hex = raw.replace("-", "").trim().toLowerCase(Locale.ROOT);
        if (hex.length() != 32) {
            return raw.trim();
        }
        return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16)
                + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
    }

    static DeviceLogin parseDeviceLogin(String body) throws AccountException {
        JsonObject json = asObject(body);
        DeviceLogin login = new DeviceLogin();
        login.deviceCode = stringField(json, "device_code");
        login.userCode = stringField(json, "user_code");
        login.verificationUri = stringField(json, "verification_uri");
        login.verificationUriComplete = optionalString(json, "verification_uri_complete");
        login.expiresIn = intField(json, "expires_in", 900);
        login.interval = Math.max(1, intField(json, "interval", 5));
        login.message = optionalString(json, "message");
        if (login.deviceCode.isEmpty() || login.userCode.isEmpty() || login.verificationUri.isEmpty()) {
            throw new AccountException("Microsoft device login response was incomplete");
        }
        return login;
    }

    static String classifyTokenError(String errorCode) {
        if (errorCode == null) {
            return "failed";
        }
        String code = errorCode.trim().toLowerCase(Locale.ROOT);
        if ("authorization_pending".equals(code)) {
            return "pending";
        }
        if ("slow_down".equals(code)) {
            return "slow_down";
        }
        if ("authorization_declined".equals(code) || "access_denied".equals(code)) {
            return "denied";
        }
        if ("expired_token".equals(code) || "bad_verification_code".equals(code)) {
            return "expired";
        }
        return "failed";
    }

    static boolean hasMinecraftLicense(JsonObject entitlements) {
        if (entitlements == null || !entitlements.has("items") || !entitlements.get("items").isJsonArray()) {
            return false;
        }
        JsonArray items = entitlements.getAsJsonArray("items");
        for (JsonElement element : items) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            String name = optionalString(element.getAsJsonObject(), "name");
            if ("product_minecraft".equalsIgnoreCase(name) || "game_minecraft".equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static XboxAuth authenticateXbox(String microsoftAccessToken) throws IOException, AccountException {
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + microsoftAccessToken);
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://auth.xboxlive.com");
        body.addProperty("TokenType", "JWT");
        HttpRequest.HttpResponseResult response = HttpRequest.postJson(XBOX_AUTH_URL, body);
        if (!response.isSuccess()) {
            throw new AccountException(parseError(response.getBody(), "Xbox Live authentication failed"));
        }
        return parseXbox(response.getBody());
    }

    private static XboxAuth authorizeXsts(String xboxToken) throws IOException, AccountException {
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(xboxToken));
        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        properties.add("UserTokens", userTokens);
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        body.addProperty("TokenType", "JWT");
        HttpRequest.HttpResponseResult response = HttpRequest.postJson(XSTS_URL, body);
        if (!response.isSuccess()) {
            throw new AccountException(parseError(response.getBody(),
                    "Minecraft premium account is not eligible for Xbox authorization"));
        }
        return parseXbox(response.getBody());
    }

    private static MinecraftLogin loginMinecraft(String userHash, String xstsToken)
            throws IOException, AccountException {
        JsonObject body = new JsonObject();
        body.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
        HttpRequest.HttpResponseResult response = HttpRequest.postJson(MC_LOGIN_URL, body);
        if (!response.isSuccess()) {
            throw new AccountException(parseError(response.getBody(), "Minecraft Services login failed"));
        }
        JsonObject json = asObject(response.getBody());
        MinecraftLogin login = new MinecraftLogin();
        login.accessToken = stringField(json, "access_token");
        login.expiresIn = intField(json, "expires_in", 86400);
        if (login.accessToken.isEmpty()) {
            throw new AccountException("Minecraft Services login response did not include an access token");
        }
        return login;
    }

    private static void assertMinecraftLicense(String mcAccessToken) throws IOException, AccountException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mcAccessToken);
        HttpRequest.HttpResponseResult response = HttpRequest.get(MC_ENTITLEMENTS_URL, headers);
        if (!response.isSuccess()) {
            String fallback = response.getStatusCode() == 403
                    ? "Minecraft Services API access was denied. Make sure this Azure app has been granted Minecraft API access."
                    : "Failed to fetch Minecraft entitlements";
            throw new AccountException(parseError(response.getBody(), fallback));
        }
        if (!hasMinecraftLicense(asObject(response.getBody()))) {
            throw new AccountException("NO_JAVA_LICENSE");
        }
    }

    private static JsonObject fetchProfile(String mcAccessToken) throws IOException, AccountException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mcAccessToken);
        HttpRequest.HttpResponseResult response = HttpRequest.get(MC_PROFILE_URL, headers);
        if (!response.isSuccess()) {
            String fallback = response.getStatusCode() == 404
                    ? "NO_JAVA_PROFILE"
                    : response.getStatusCode() == 403
                    ? "Minecraft Services API access was denied. Make sure this Azure app has been granted Minecraft API access."
                    : "Failed to fetch Minecraft profile";
            throw new AccountException(parseError(response.getBody(), fallback));
        }
        return asObject(response.getBody());
    }

    private static XboxAuth parseXbox(String body) throws AccountException {
        JsonObject json = asObject(body);
        XboxAuth auth = new XboxAuth();
        auth.token = stringField(json, "Token");
        if (auth.token.isEmpty()) {
            throw new AccountException("Xbox Live authentication response did not include a token");
        }
        if (json.has("DisplayClaims") && json.get("DisplayClaims").isJsonObject()) {
            JsonObject claims = json.getAsJsonObject("DisplayClaims");
            if (claims.has("xui") && claims.get("xui").isJsonArray()) {
                JsonArray users = claims.getAsJsonArray("xui");
                if (users.size() > 0 && users.get(0).isJsonObject()) {
                    JsonObject user = users.get(0).getAsJsonObject();
                    auth.uhs = optionalString(user, "uhs");
                    auth.xuid = optionalString(user, "xid");
                }
            }
        }
        return auth;
    }

    private static JsonObject asObject(String body) throws AccountException {
        JsonObject object = tryObject(body);
        if (object == null) {
            throw new AccountException("Unexpected Microsoft login response");
        }
        return object;
    }

    private static JsonObject tryObject(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement element = new JsonParser().parse(body);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String parseError(String body, String fallback) {
        JsonObject json = tryObject(body);
        if (json == null) {
            return fallback;
        }
        String description = firstNonEmpty(
                optionalString(json, "error_description"),
                optionalString(json, "errorMessage"),
                optionalString(json, "error"),
                optionalString(json, "Message"));
        return description != null ? description : fallback;
    }

    private static String stringField(JsonObject json, String key) {
        return optionalString(json, key);
    }

    private static String optionalString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return "";
        }
        try {
            return json.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int intField(JsonObject json, String key, int fallback) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return json.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public static final class DeviceLogin {
        public String deviceCode;
        public String userCode;
        public String verificationUri;
        public String verificationUriComplete;
        public int expiresIn;
        public int interval;
        public String message;

        public String browserUrl() {
            return verificationUriComplete != null && !verificationUriComplete.isEmpty()
                    ? verificationUriComplete
                    : verificationUri;
        }
    }

    public static final class PollResult {
        public final String status;
        public final int interval;
        public final String error;
        public final MinecraftProfile account;

        private PollResult(String status, int interval, String error, MinecraftProfile account) {
            this.status = status;
            this.interval = interval;
            this.error = error;
            this.account = account;
        }

        public static PollResult pending(int interval, String message) {
            return new PollResult("pending", interval, message, null);
        }

        public static PollResult completed(MinecraftProfile account) {
            return new PollResult("completed", 0, null, account);
        }

        public static PollResult denied(String message) {
            return new PollResult("denied", 0, message, null);
        }

        public static PollResult expired(String message) {
            return new PollResult("expired", 0, message, null);
        }

        public boolean isPending() {
            return "pending".equals(status) || "slow_down".equals(status);
        }
    }

    public static final class MinecraftProfile {
        public String name;
        public String uuid;
        public String accessToken;
        public String refreshToken;
        public String xuid;
        public long expiresAt;
    }

    private static final class XboxAuth {
        String token;
        String uhs;
        String xuid;
    }

    private static final class MinecraftLogin {
        String accessToken;
        int expiresIn;
    }
}
