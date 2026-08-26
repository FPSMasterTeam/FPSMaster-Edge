package top.fpsmaster.modules.client.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import top.fpsmaster.cosmetic.RemoteCosmeticService;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for managing authentication tokens at system user level.
 * Tokens are stored in AppData/Roaming/FPSMaster/auth.json
 * Launcher can pass tokens via system properties: fpsmaster.auth.token
 */
public class AuthService {
    private static final String AUTH_FILE_NAME = "auth.json";
    private static final String SYSTEM_PROPERTY_TOKEN = "fpsmaster.auth.token";
    private static final String SYSTEM_PROPERTY_REFRESH = "fpsmaster.auth.refreshToken";
    private static final String SYSTEM_PROPERTY_EXPIRES = "fpsmaster.auth.tokenExpiresAt";

    private static AuthService instance;
    private final Gson gson;
    private final File authFile;

    private String accessToken;
    private String refreshToken;
    private long tokenExpiresAt;

    private AuthService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.authFile = resolveAuthFile();
        loadFromFile();
    }

    private File resolveAuthFile() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            File fpsmasterDir = new File(appData, "FPSMaster");
            if (!fpsmasterDir.exists()) {
                fpsmasterDir.mkdirs();
            }
            return new File(fpsmasterDir, AUTH_FILE_NAME);
        }
        // Fallback to user home
        String userHome = System.getProperty("user.home");
        File fpsmasterDir = new File(userHome, ".fpsmaster");
        if (!fpsmasterDir.exists()) {
            fpsmasterDir.mkdirs();
        }
        return new File(fpsmasterDir, AUTH_FILE_NAME);
    }

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Initialize the service and check for launcher-provided tokens
     */
    public void initialize() {
        // Check if launcher provided tokens via system properties
        String launcherToken = System.getProperty(SYSTEM_PROPERTY_TOKEN);
        if (launcherToken != null && !launcherToken.isEmpty()) {
            ClientLogger.info("Found auth token from launcher");
            this.accessToken = launcherToken;
            this.refreshToken = System.getProperty(SYSTEM_PROPERTY_REFRESH);
            String expiresStr = System.getProperty(SYSTEM_PROPERTY_EXPIRES);
            if (expiresStr != null && !expiresStr.isEmpty()) {
                try {
                    this.tokenExpiresAt = Long.parseLong(expiresStr);
                } catch (NumberFormatException e) {
                    this.tokenExpiresAt = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
                }
            } else {
                this.tokenExpiresAt = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
            }
            saveToFile();
        } else {
            loadFromFile();
        }
    }

    public boolean isLoggedIn() {
        return accessToken != null && !accessToken.isEmpty() && !isTokenExpired();
    }

    public boolean isTokenExpired() {
        return tokenExpiresAt > 0 && System.currentTimeMillis() >= tokenExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    /**
     * Save tokens (called after successful login)
     */
    public void saveTokens(String access, String refresh, long expiresAt) {
        this.accessToken = access;
        this.refreshToken = refresh;
        this.tokenExpiresAt = expiresAt;
        saveToFile();
    }

    /**
     * Save tokens with default expiration (7 days)
     */
    public void saveTokens(String access, String refresh) {
        saveTokens(access, refresh, System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
    }

    /**
     * Clear all tokens (called after logout)
     */
    public void clearTokens() {
        this.accessToken = null;
        this.refreshToken = null;
        this.tokenExpiresAt = 0;
        saveToFile();
        // Cosmetics resolved for this account stop being ours to show the moment the account goes.
        RemoteCosmeticService.getInstance().onLogout();
    }

    private void saveToFile() {
        try {
            JsonObject json = new JsonObject();
            if (accessToken != null) {
                json.addProperty("accessToken", accessToken);
            }
            if (refreshToken != null) {
                json.addProperty("refreshToken", refreshToken);
            }
            json.addProperty("tokenExpiresAt", tokenExpiresAt);
            json.addProperty("lastUpdated", System.currentTimeMillis());

            String content = gson.toJson(json);
            try (BufferedWriter writer = Files.newBufferedWriter(authFile.toPath())) {
                writer.write(content);
            }
            ClientLogger.debug("Auth tokens saved to: " + authFile.getAbsolutePath());
        } catch (IOException e) {
            ClientLogger.error("Failed to save auth tokens: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        if (!authFile.exists()) {
            ClientLogger.debug("No auth file found at: " + authFile.getAbsolutePath());
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = Files.newBufferedReader(authFile.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            JsonObject json = gson.fromJson(content.toString(), JsonObject.class);
            if (json != null) {
                accessToken = json.has("accessToken") && !json.get("accessToken").isJsonNull()
                        ? json.get("accessToken").getAsString()
                        : null;
                refreshToken = json.has("refreshToken") && !json.get("refreshToken").isJsonNull()
                        ? json.get("refreshToken").getAsString()
                        : null;
                tokenExpiresAt = json.has("tokenExpiresAt") && !json.get("tokenExpiresAt").isJsonNull()
                        ? json.get("tokenExpiresAt").getAsLong()
                        : 0L;
                ClientLogger.debug("Auth tokens loaded from: " + authFile.getAbsolutePath());
            }
        } catch (IOException e) {
            ClientLogger.error("Failed to load auth tokens: " + e.getMessage());
        }
    }

    public File getAuthFile() {
        return authFile;
    }
}
