package top.fpsmaster.modules.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.os.HttpRequest;

public class AccountManager {
    private String token = "";
    private String username = "";
    private String[] itemsHeld = new String[0];

    public void autoLogin() {
        try {
            token = FileUtils.readTempValue("token").trim();
            username = FPSMaster.configManager.configure.getOrCreate("username", "offline").trim();
            if (!token.isEmpty() && !username.isEmpty()) {
                if (attemptLogin(username, token)) {
                    ClientLogger.info("自动登录成功！  " + username);
                    FPSMaster.INSTANCE.loggedIn = true;
                    getItems(username, token);
                } else {
                    ClientLogger.info(username);
                    ClientLogger.error("自动登录失败！");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ClientLogger.error("尝试自动登录失败！" + e.getMessage());
        }
    }

    private boolean attemptLogin(String username, String token) {
        if (username.isEmpty() || token.isEmpty()) {
            return false;
        }
        String s = HttpRequest.get(FPSMaster.SERVICE_API + "/checkToken?username=" + username + "&token=" + token + "&timestamp=" + System.currentTimeMillis());
        JsonObject json = parser.parse(s).getAsJsonObject();
        this.username = username;
        this.token = token;
        return json.get("code").getAsInt() == 200;
    }

    public void getItems(String username, String token) {
        String s = HttpRequest.get(FPSMaster.SERVICE_API + "/getWebUser?username=" + username + "&token=" + token + "&timestamp=" + System.currentTimeMillis());
        JsonObject json = parser.parse(s).getAsJsonObject();
        if (json.get("code").getAsInt() == 200) {
            String items = json.getAsJsonObject("data").getAsJsonObject("items").getAsString();
            itemsHeld = items.split(",");
            itemsHeld = itemsHeld.length > 0 ? itemsHeld : new String[0]; // Ensuring it's not empty
        }
    }

    public static JsonParser parser = new JsonParser();
    public static String cape = "";
    public static String skin = "";

    public static JsonObject login(String username, String password) {
        String s = HttpRequest.get(FPSMaster.SERVICE_API + "/login?username=" + username + "&password=" + password + "&timestamp=" + System.currentTimeMillis());
        return parser.parse(s).getAsJsonObject();
    }

    // Getter and Setter methods
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String[] getItemsHeld() {
        return itemsHeld;
    }

    public void setItemsHeld(String[] itemsHeld) {
        this.itemsHeld = itemsHeld;
    }
}
