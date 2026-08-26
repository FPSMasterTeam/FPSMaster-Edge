package top.fpsmaster.modules.client.api;

public final class FPSMasterConstants {
    private FPSMasterConstants() {
    }

    private static final String PRODUCTION_BASE_URL = "https://api.fpsmaster.top";

    /**
     * The one place the backend host is decided. System property beats environment variable beats the
     * production default, so a local session runs entirely against
     * {@code -Dfpsmaster.api.baseUrl=http://127.0.0.1:8722} without a single call escaping to production.
     * Every auth, catalog, purchase, loadout and texture URL is derived from this.
     */
    public static final String API_BASE_URL = resolveBaseUrl();
    public static final String API_VERSION = "/api/v1";
    public static final String USER_AGENT = "FPSMaster-Edge/" + getClientVersion();

    private static String resolveBaseUrl() {
        String property = System.getProperty("fpsmaster.api.baseUrl");
        if (property != null && !property.trim().isEmpty()) {
            return trimTrailingSlash(property.trim());
        }
        String environment = System.getenv("FPSMASTER_API_BASE_URL");
        if (environment != null && !environment.trim().isEmpty()) {
            return trimTrailingSlash(environment.trim());
        }
        return PRODUCTION_BASE_URL;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Resolves a backend-relative path (an asset key, for example) against the configured base. */
    public static String resolve(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        return path.startsWith("/") ? API_BASE_URL + path : path;
    }

    private static String getClientVersion() {
        try {
            Class<?> clazz = Class.forName("top.fpsmaster.FPSMaster");
            Object version = clazz.getField("CLIENT_VERSION").get(null);
            return version != null ? version.toString() : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    public static final class Endpoints {
        private Endpoints() {
        }

        public static final String LOGIN = API_BASE_URL + API_VERSION + "/auth/login";
        public static final String LAUNCHER_LOGIN = API_BASE_URL + API_VERSION + "/auth/launcher/login";
        public static final String REGISTER = API_BASE_URL + API_VERSION + "/auth/register";
        public static final String LOGOUT = API_BASE_URL + API_VERSION + "/auth/logout";
        public static final String REFRESH_TOKEN = API_BASE_URL + API_VERSION + "/auth/refresh";
        public static final String USER_INFO = API_BASE_URL + API_VERSION + "/user/info";
        public static final String USER_STATS = API_BASE_URL + API_VERSION + "/user/stats";
        public static final String OWNED_ITEMS = API_BASE_URL + API_VERSION + "/me/items";
        public static final String CATALOG_ITEMS = API_BASE_URL + API_VERSION + "/catalog/items";
        public static final String PURCHASES = API_BASE_URL + API_VERSION + "/me/purchases";
        public static final String COSMETIC_LOADOUT = API_BASE_URL + API_VERSION + "/me/cosmetics/loadout";
        public static final String RESOLVE_LOADOUTS = API_BASE_URL + API_VERSION + "/cosmetics/loadouts/resolve";
        public static final String MINECRAFT_LINK_CHALLENGE = API_BASE_URL + API_VERSION + "/me/minecraft-links/challenge";
        public static final String MINECRAFT_LINK_CONFIRM = API_BASE_URL + API_VERSION + "/me/minecraft-links/confirm";
        public static final String TELEMETRY_HEARTBEAT = API_BASE_URL + API_VERSION + "/telemetry/heartbeat";
        public static final String TELEMETRY_PRESENCE = API_BASE_URL + API_VERSION + "/telemetry/presence";
        public static final String TELEMETRY_OFFLINE = API_BASE_URL + API_VERSION + "/telemetry/offline";
    }

    public static final class ResponseFields {
        private ResponseFields() {
        }

        public static final String SUCCESS = "success";
        public static final String MESSAGE = "message";
        public static final String DATA = "data";
    }
}
