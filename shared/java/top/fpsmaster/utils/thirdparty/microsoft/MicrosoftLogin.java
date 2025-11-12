package top.fpsmaster.utils.thirdparty.microsoft;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Session;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.os.HttpRequest;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.fpsmaster.utils.Utility.mc;

public class MicrosoftLogin {

    public static String loginProgressMessage = "Waiting for login...";

    // --- Configuration ---
    private static final String CLIENT_ID = "d1ed1b72-9f7c-41bc-9702-365d2cbd2e38";
    private static final int SERVER_PORT = 17342;
    private static final String REDIRECT_URI = "http://127.0.0.1:" + SERVER_PORT;
    private static final boolean DEBUG_MODE = false;

    private static HttpServer httpServer;
    private static final AtomicBoolean loginCompleted = new AtomicBoolean(false);

    /**
     * Starts the local HTTP server to handle the Microsoft authentication redirect.
     * This method should be called before opening the browser for login.
     */
    public static void startLocalHttpServer() {
        if (httpServer != null) {
            logInfo("HTTP server is already running.");
            return;
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            logInfo("Created login server on port " + SERVER_PORT);

            httpServer.createContext("/", exchange -> {
                logInfo("New connection received on HTTP server.");
                String responseMessage = "Authentication successful! You can close this tab.";
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(200, responseMessage.length());
                    responseBody.write(responseMessage.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logError("Error sending HTTP response: " + e.getMessage());
                } finally {
                    // Stop the server immediately after receiving the code
                    stopLocalHttpServer();
                }

                String requestUri = exchange.getRequestURI().toString();
                logDebug("Received request URI: " + requestUri);

                if (requestUri.contains("code=")) {
                    String code = requestUri.substring(requestUri.indexOf("=") + 1);
                    logInfo("Authorization code received. Starting token exchange...");
                    try {
                        // Exchange authorization code for initial tokens
                        Map<String, String> tokenRequestParams = new HashMap<>();
                        tokenRequestParams.put("client_id", CLIENT_ID);
                        tokenRequestParams.put("code", code);
                        tokenRequestParams.put("grant_type", "authorization_code");
                        tokenRequestParams.put("redirect_uri", REDIRECT_URI);

                        String oauthResponse = HttpRequest.postForm("https://login.live.com/oauth20_token.srf", tokenRequestParams).getBody();
                        logDebug("OAuth Response: " + oauthResponse);
                        JsonObject oauthJson = HttpRequest.gson().fromJson(oauthResponse, JsonObject.class);
                        String accessToken = getJsonString(oauthJson, "access_token", "OAuth access token");
                        String refreshToken = getJsonString(oauthJson, "refresh_token", "OAuth refresh token");
                        logInfo("OAuth Access Token obtained. (Refresh Token: " + (DEBUG_MODE ? refreshToken : "[HIDDEN]") + ")");

                        // Continue with Minecraft authentication using the obtained access token
                        continueMinecraftAuthentication(accessToken);
                        loginCompleted.set(true);
                    } catch (Exception e) {
                        logError("Error during login completion: " + e.getMessage());
                        setStep("Login Failed. (error: " + e.getMessage() + ")");
                    }
                } else {
                    logError("No authorization code found in the redirect URI.");
                    setStep("Login failed. (no authorization code found in the redirect URI)");
                    stopLocalHttpServer();
                }
            });

            // Use a single-thread executor for the HTTP server to avoid resource issues
            httpServer.setExecutor(Executors.newSingleThreadExecutor());
            httpServer.start();
            logInfo("HTTP server started successfully.");
        } catch (IOException e) {
            logError("Failed to start HTTP server: " + e.getMessage());
            setStep("Login failed. (error: " + e.getMessage() + ")");
            httpServer = null; // Ensure server is null if creation failed
            throw new RuntimeException("Failed to start HTTP server for Microsoft login.", e);
        }
    }

    /**
     * Stops the local HTTP server.
     */
    public static void stopLocalHttpServer() {
        if (httpServer != null) {
            httpServer.stop(1); // Stop with a 1-second delay to allow current requests to finish
            httpServer = null;
            logInfo("HTTP server stopped.");
        }
    }

    /**
     * Initiates the Microsoft login process by opening a browser window.
     * This method will start a local HTTP server to listen for the redirect.
     *
     * @return true if the login process is successfully initiated (browser opened), false otherwise.
     */
    public static boolean loginMicrosoft() {
        startLocalHttpServer(); // Ensure the server is running before opening the browser
        if (httpServer == null) {
            logError("HTTP server failed to start. Cannot proceed with browser login.");
            return false;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("client_id", CLIENT_ID);
            params.put("response_type", "code");
            params.put("redirect_uri", REDIRECT_URI);
            params.put("scope", "XboxLive.signin offline_access");

            String microsoftAuthUrl = buildUrl("https://login.live.com/oauth20_authorize.srf", params);
            logInfo("Opening browser for Microsoft authentication: " + microsoftAuthUrl);
            Desktop.getDesktop().browse(URI.create(microsoftAuthUrl));
            return true;
        } catch (IOException e) {
            logError("Failed to open browser for Microsoft login: " + e.getMessage());
            setStep("Login failed. (failed to open browser for Microsoft login)");
            stopLocalHttpServer(); // Stop server if browser can't be opened
            return false;
        }
    }

    /**
     * Continues the Minecraft authentication process using an Xbox Live access token.
     * This method covers steps 2-5 of the original login flow:
     * Xbox Live authentication, XSTS authorization, Minecraft authentication, and profile retrieval.
     *
     * @param xboxAccessToken The Xbox Live access token obtained from the initial OAuth flow or refresh token.
     * @throws IOException If any HTTP request fails or authentication fails.
     */
    private static void continueMinecraftAuthentication(String xboxAccessToken) throws IOException {
        logInfo("Continuing Minecraft authentication flow...");

        // 2. Authenticate with Xbox Live
        setStep("Step 2/5: Authenticating with Xbox Live...");
        Map<String, Object> xboxAuthProperties = new HashMap<>();
        xboxAuthProperties.put("AuthMethod", "RPS");
        xboxAuthProperties.put("SiteName", "user.auth.xboxlive.com");
        xboxAuthProperties.put("RpsTicket", "d=" + xboxAccessToken);

        JsonObject xboxAuthPayload = new JsonObject();
        xboxAuthPayload.add("Properties", HttpRequest.gson().toJsonTree(xboxAuthProperties));
        xboxAuthPayload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xboxAuthPayload.addProperty("TokenType", "JWT");

        String xboxAuthResponse = HttpRequest.postJson("https://user.auth.xboxlive.com/user/authenticate", xboxAuthPayload).getBody();
        logDebug("Xbox Auth Response: " + xboxAuthResponse);
        JsonObject xboxAuthJson = HttpRequest.gson().fromJson(xboxAuthResponse, JsonObject.class);
        String xblToken = getJsonString(xboxAuthJson, "Token", "XBL Token");
        String xblUserhash = xboxAuthJson.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui").get(0).getAsJsonObject()
                .get("uhs").getAsString();
        logInfo("Xbox Live Token and Userhash obtained.");

        // 3. Authorize with XSTS
        setStep("Step 3/5: Authorizing with XSTS service...");
        JsonObject xstsProperties = new JsonObject();
        xstsProperties.addProperty("SandboxId", "RETAIL");
        xstsProperties.add("UserTokens", HttpRequest.gson().toJsonTree(new String[]{xblToken}));

        JsonObject xstsPayload = new JsonObject();
        xstsPayload.add("Properties", xstsProperties);
        xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsPayload.addProperty("TokenType", "JWT");

        String xstsResponse = HttpRequest.postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsPayload).getBody();
        logDebug("XSTS Response: " + xstsResponse);
        JsonObject xstsJson = HttpRequest.gson().fromJson(xstsResponse, JsonObject.class);

        if (xstsJson.has("XErr")) {
            long xErrCode = xstsJson.get("XErr").getAsLong();
            String message = xstsJson.has("Message") ? xstsJson.get("Message").getAsString() : "Unknown XSTS error.";
            logError("XSTS Error: " + message + " (Code: " + xErrCode + ")");
            if (xErrCode == 2148916064L) {
                logError("This typically means the account is a child account and requires adult verification.");
            } else if (xErrCode == 2148916065L) {
                logError("This usually means the account has not accepted the Xbox Live terms of service.");
            }
            throw new IOException("XSTS authorization failed: " + message);
        }

        String xstsToken = getJsonString(xstsJson, "Token", "XSTS Token");
        String xstsUserhash = xstsJson.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui").get(0).getAsJsonObject()
                .get("uhs").getAsString();
        logInfo("XSTS Token and Userhash obtained.");

        // 4. Authenticate with Minecraft
        setStep("Step 4/5: Authenticating with Minecraft services...");
        JsonObject minecraftAuthPayload = new JsonObject();
        minecraftAuthPayload.addProperty("identityToken", "XBL3.0 x=" + xstsUserhash + ";" + xstsToken);

        String minecraftAuthResponse = HttpRequest.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", minecraftAuthPayload).getBody();
        logDebug("Minecraft Auth Response: " + minecraftAuthResponse);
        JsonObject minecraftAuthJson = HttpRequest.gson().fromJson(minecraftAuthResponse, JsonObject.class);
        String mcAccessToken = getJsonString(minecraftAuthJson, "access_token", "Minecraft access token");
        String mcUsername = getJsonString(minecraftAuthJson, "username", "Minecraft username"); // This is often the UUID, not the display name
        logInfo("Minecraft Access Token obtained. Username: " + mcUsername);


        // 5. Get Minecraft Profile
        setStep("Step 5/5: Retrieving Minecraft profile...");
        Map<String, String> profileHeaders = new HashMap<>();
        profileHeaders.put("Authorization", "Bearer " + mcAccessToken);

        String profileResponse = HttpRequest.get("https://api.minecraftservices.com/minecraft/profile", profileHeaders).getBody();
        logDebug("Minecraft Profile Response: " + profileResponse);
        JsonObject profileJson = HttpRequest.gson().fromJson(profileResponse, JsonObject.class);

        String uuid = getJsonString(profileJson, "id", "Minecraft UUID");
        String name = getJsonString(profileJson, "name", "Minecraft Username");
        boolean hasBoughtGame = profileJson.has("name") && profileJson.has("id"); // Simple check

        if (!hasBoughtGame) {
            logError("Minecraft profile indicates game not owned or profile not found.");
            throw new IOException("Minecraft account does not own the game or profile not found.");
        }

        logInfo("Successfully retrieved Minecraft profile - Name: " + name + ", UUID: " + uuid);

        // Set Minecraft Session
        logInfo("Setting Minecraft session...");
        Wrappers.minecraft().setSession(new Session(name, uuid, mcAccessToken, "mojang"));
        setStep("Minecraft session updated successfully!");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mc.displayGuiScreen(new MainMenu());
    }

    /**
     * Converts a map of parameters to a URL-encoded string.
     *
     * @param params The map of parameters.
     * @return A URL-encoded string.
     */
    private static String paramsToUrlEncoded(Map<String, String> params) {
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sj.add(urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()));
        }
        return sj.toString();
    }

    /**
     * Builds a URL with query parameters from a base URL and a map of parameters.
     *
     * @param baseUrl The base URL.
     * @param params  The map of parameters.
     * @return The constructed URL.
     */
    private static String buildUrl(String baseUrl, Map<String, String> params) {
        if (params.isEmpty()) {
            return baseUrl;
        }
        return baseUrl + "?" + paramsToUrlEncoded(params);
    }

    /**
     * URL-encodes a string.
     *
     * @param value The string to encode.
     * @return The URL-encoded string.
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            logError("Failed to URL encode: " + value + " - " + e.getMessage());
            return value; // Fallback to unencoded if encoding fails
        }
    }

    /**
     * Safely retrieves a string value from a JsonObject.
     *
     * @param jsonObject The JsonObject.
     * @param key        The key to retrieve.
     * @param fieldName  A user-friendly name for the field (for logging).
     * @return The string value.
     * @throws IOException If the key is not found or is not a string.
     */
    private static String getJsonString(JsonObject jsonObject, String key, String fieldName) throws IOException {
        if (jsonObject == null || !jsonObject.has(key) || !jsonObject.get(key).isJsonPrimitive()) {
            logError("Missing or invalid field in JSON response: " + fieldName + " (key: " + key + ")");
            throw new IOException("Missing or invalid field in JSON response: " + fieldName);
        }
        return jsonObject.get(key).getAsString();
    }

    // --- Logging Utilities ---

    private static void logInfo(String message) {
        System.out.println("[MicrosoftLogin INFO] " + message);
    }

    private static void logDebug(String message) {
        if (DEBUG_MODE) {
            System.out.println("[MicrosoftLogin DEBUG] " + message);
        }
    }

    private static void logError(String message) {
        System.err.println("[MicrosoftLogin ERROR] " + message);
    }



    public static void loginViaBrowser() {
        try {
            logInfo("Starting Microsoft login process...");
            setStep("Step 1/5: Retrieving accessToken from browser...");
            boolean initiated = loginMicrosoft();
            if (initiated) {
                logInfo("Browser opened. Waiting for login completion...");
                long startTime = System.currentTimeMillis();
                long timeout = 120 * 1000; // 2 minutes timeout
                while (!loginCompleted.get() && (System.currentTimeMillis() - startTime < timeout)) {
                    try {
                        Thread.sleep(1000); // Wait 1 second
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logError("Login wait interrupted: " + e.getMessage());
                        setStep("Login wait interrupted");
                        break;
                    }
                }
                if (loginCompleted.get()) {
                    logInfo("Login process finished.");
                } else {
                    logError("Login timed out or did not complete.");
                }
            } else {
                logError("Failed to initiate browser login.");
            }
        } catch (Exception e) {
            logError("An unexpected error occurred in main: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stopLocalHttpServer(); // Ensure server is stopped on exit
        }
    }

    public static void setStep(String step) {
        logInfo(step);
        loginProgressMessage = step;
    }
}