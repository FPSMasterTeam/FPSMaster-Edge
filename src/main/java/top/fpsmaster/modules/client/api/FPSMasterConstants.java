package top.fpsmaster.modules.client.api;

public final class FPSMasterConstants {
    private FPSMasterConstants() {
    }

    public static final String API_BASE_URL = "https://api.fpsmaster.top";
    public static final String API_VERSION = "/api/v1";
    public static final String USER_AGENT = "FPSMaster-Edge/" + getClientVersion();

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
    }

    public static final class ResponseFields {
        private ResponseFields() {
        }

        public static final String SUCCESS = "success";
        public static final String MESSAGE = "message";
        public static final String DATA = "data";
    }
}
