package top.fpsmaster.modules.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.AccountException;
import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.exception.NetworkException;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.os.HttpRequest;

public class AccountManager {
    private String token = "";
    private String username = "";
    private String[] itemsHeld = new String[0];

    public void autoLogin() {
        FPSMaster.async.runnable(() -> {
            try {
                doAutoLogin();
            } catch (FileException e) {
                ExceptionHandler.handleFileException(e, "尝试自动登录失败");
            } catch (AccountException e) {
                ExceptionHandler.handleAccountException(e, "尝试自动登录失败");
            } catch (NetworkException e) {
                ExceptionHandler.handleNetworkException(e, "尝试自动登录失败");
            } catch (Exception e) {
                ExceptionHandler.handle(e, "尝试自动登录失败");
            }
        });
    }

    private void doAutoLogin() throws FileException, AccountException, NetworkException {
        token = FileUtils.readTempValue("token").trim();
        username = FPSMaster.configManager.configure.getOrCreate("username", "").trim(); // Since we do the empty check, we should make it empty.
        if (!token.isEmpty() && !username.isEmpty()) {
            if (attemptLogin(username, token)) {
                ClientLogger.info("自动登录成功！  " + username);
                FPSMaster.INSTANCE.loggedIn = true;
                getItems(username, token);
            } else {
                ClientLogger.info(username);
                ClientLogger.error("自动登录失败！");
                throw new AccountException("自动登录失败");
            }
        }
    }

    private boolean attemptLogin(String username, String token) throws NetworkException {
        if (username.isEmpty() || token.isEmpty()) {
            return false;
        }
        try {
            String s = HttpRequest.get(FPSMaster.SERVICE_API + "/checkToken?username=" + username + "&token=" + token + "&timestamp=" + System.currentTimeMillis());
            JsonObject json = parser.parse(s).getAsJsonObject();
            this.username = username;
            this.token = token;
            return json.get("code").getAsInt() == 200;
        } catch (Exception e) {
            throw new NetworkException("Failed to check token", e);
        }
    }

    public void getItems(String username, String token) throws NetworkException {
        try {
            String s = HttpRequest.get(FPSMaster.SERVICE_API + "/getWebUser?username=" + username + "&token=" + token + "&timestamp=" + System.currentTimeMillis());
            JsonObject json = parser.parse(s).getAsJsonObject();
            if (json.get("code").getAsInt() == 200) {
                String items = json.getAsJsonObject("data").getAsJsonObject("items").getAsString();
                itemsHeld = items.split(",");
                itemsHeld = itemsHeld.length > 0 ? itemsHeld : new String[0]; // Ensuring it's not empty
            } else {
                throw new NetworkException("Failed to get items: " + json.get("message").getAsString());
            }
        } catch (Exception e) {
            if (e instanceof NetworkException) {
                throw (NetworkException) e;
            }
            throw new NetworkException("Failed to get items", e);
        }
    }

    public static JsonParser parser = new JsonParser();
    public static String cape = "";
    public static String skin = "";

    public static JsonObject login(String username, String password) throws NetworkException {
        try {
            String s = HttpRequest.get(FPSMaster.SERVICE_API + "/login?username=" + username + "&password=" + password + "&timestamp=" + System.currentTimeMillis());
            JsonObject jsonObject = parser.parse(s).getAsJsonObject();
            if (jsonObject.get("code").getAsInt() != 200) {
                throw new NetworkException("Login failed: " + jsonObject.get("message").getAsString());
            }
            return jsonObject;
        } catch (Exception e) {
            if (e instanceof NetworkException) {
                throw (NetworkException) e;
            }
            throw new NetworkException("Login failed", e);
        }
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
