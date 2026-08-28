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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    /**
     * 饿汉式：懒加载那版的 {@code getInstance()} 没有同步，两条线程同时第一次调用会各造一个
     * 实例，各自持有一份 token 和一把 {@code fileLock}——串行化写 auth.json 的前提当场没了。
     * 构造函数只读一次文件，类加载时做掉不值得省。
     */
    private static final AuthService INSTANCE = new AuthService();
    private final Gson gson;
    private final File authFile;

    /**
     * 渲染线程每帧经 {@code isLoggedIn()} 读这三个字段，而写它们的线程有三条
     * （登录、登出、profile 刷新回调），所以必须 volatile——否则界面可能长期
     * 读到旧值、显示成已登录。
     */
    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile long tokenExpiresAt;

    /** 串行化对 {@link #authFile} 的读写：两条线程并发 truncate 会留下半截 JSON。 */
    private final Object fileLock = new Object();

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
        return INSTANCE;
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
            synchronized (fileLock) {
                saveToFile();
            }
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
        synchronized (fileLock) {
            this.accessToken = access;
            this.refreshToken = refresh;
            this.tokenExpiresAt = expiresAt;
            saveToFile();
        }
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
        synchronized (fileLock) {
            this.accessToken = null;
            this.refreshToken = null;
            this.tokenExpiresAt = 0;
            saveToFile();
        }
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
            // 先写临时文件再原子替换：直接 truncate 目标文件的话，写到一半崩溃/掉电
            // 会留下无法解析的半截 JSON，下次启动就读不出 token 了。
            Path target = authFile.toPath();
            // 临时文件名必须是每次唯一的：Edge 和 Nova 落在同一个 auth.json 上（两边算出的是
            // 同一条路径），fileLock 只锁得住本进程。同时开着两个客户端时，固定名字的
            // auth.json.tmp 会被两边同时写，先改名的那个搬走的可能是另一边写了一半的正文
            // ——原子改名保住的只是「换名这一步」不撕裂，保不住内容。
            Path temp = Files.createTempFile(target.getParent(), AUTH_FILE_NAME + ".", ".tmp");
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
                    writer.write(content);
                }
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException | RuntimeException failure) {
                // 写失败/改名失败都会把半截临时文件留在目录里，下次 save 又写一个新的。
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 清不掉就算了，它不影响正本。
                }
                throw failure;
            }
            ClientLogger.debug("Auth tokens saved to: " + authFile.getAbsolutePath());
        } catch (IOException e) {
            ClientLogger.error("Failed to save auth tokens: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        synchronized (fileLock) {
            loadFromFileLocked();
        }
    }

    private void loadFromFileLocked() {
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
        } catch (Exception e) {
            // 不能只 catch IOException：文件被并发写坏时 gson.fromJson 抛的是
            // JsonSyntaxException（RuntimeException），漏出去会让构造器失败、
            // 之后每次 getInstance() 都炸。读不出来等于没登录，界面自然回落。
            ClientLogger.error("Failed to load auth tokens: " + e.getMessage());
        }
    }

    public File getAuthFile() {
        return authFile;
    }
}
