package top.fpsmaster.modules.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import top.fpsmaster.exception.AccountException;
import top.fpsmaster.utils.io.HttpRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

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

    /**
     * 和启动器 (`microsoft_auth.rs`) 用的是同一个回调地址。改这里就得同步改 Azure 应用注册里
     * 的重定向 URI，否则 Microsoft 会直接拒掉授权请求。
     */
    public static final String DEFAULT_REDIRECT_URL = "http://localhost:3389/oauth";

    private static final String AUTHORIZE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";

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

    public static String redirectUrl() {
        String env = firstNonEmpty(
                System.getenv("FPSMASTER_MINECRAFT_REDIRECT_URL"),
                System.getProperty("fpsmaster.minecraft.redirectUrl"));
        return env != null ? env : DEFAULT_REDIRECT_URL;
    }

    /**
     * 授权码 + PKCE 的浏览器登录，与 FPSMaster 启动器 (`microsoft_auth.rs`) 走同一条路径。
     *
     * <p>这是目前唯一可用的路径：设备码流程会被 Microsoft 以
     * {@code AADSTS70002 - The client application must be marked as 'mobile'} 拒掉
     * （Azure 应用没有注册成公共/移动客户端），见 {@link #startDeviceLogin()} 上的说明。
     *
     * <p>用法：先拿到 session，再打开 {@link BrowserSession#authorizeUrl}，然后在别的线程上
     * {@link BrowserSession#await}。监听器在拿到 session 时就已经绑好端口了 —— 顺序不能反，
     * 否则浏览器可能比监听器先到。
     */
    public static BrowserSession beginBrowserLogin() throws IOException, AccountException {
        String redirect = redirectUrl();
        URI uri;
        try {
            uri = URI.create(redirect);
        } catch (RuntimeException exception) {
            throw new AccountException("Invalid Minecraft redirect URL");
        }
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        String state = randomToken(24);
        String verifier = randomToken(64);
        String challenge = pkceChallenge(verifier);
        ServerSocket socket = null;
        try {
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(host, port));
            socket.setSoTimeout(200);
        } catch (IOException exception) {
            // bind 成功、setSoTimeout 失败时也得把端口还回去，否则这个进程再也绑不上它。
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
            // 抛错误码而不是硬编码中文：文案在 lang 文件里，英文客户端才不会看到中文。
            throw new AccountException("PORT_IN_USE:" + host + ":" + port);
        }
        return new BrowserSession(buildAuthorizeUrl(redirect, state, challenge), socket, path, state, verifier, redirect);
    }

    static String buildAuthorizeUrl(String redirect, String state, String challenge) {
        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId())
                + "&response_type=code"
                + "&redirect_uri=" + encode(redirect)
                + "&response_mode=query"
                + "&scope=" + encode(SCOPE)
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(challenge)
                + "&code_challenge_method=S256";
    }

    static String pkceChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return base64Url(digest.digest(verifier.getBytes("US-ASCII")));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is required for Microsoft PKCE", exception);
        }
    }

    static String randomToken(int byteCount) {
        byte[] bytes = new byte[byteCount];
        new SecureRandom().nextBytes(bytes);
        return base64Url(bytes);
    }

    /**
     * RFC 4648 §5 的 base64url（无填充）。Edge 跑在 Java 8 上，{@code java.util.Base64} 是有的，
     * 但这里手写是为了不依赖 {@code getUrlEncoder().withoutPadding()} 在某些老 Android/移植 JVM
     * 上的缺席 —— 输出与 Nova 侧逐字节一致。
     */
    private static String base64Url(byte[] data) {
        final char[] table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
        StringBuilder out = new StringBuilder(((data.length + 2) / 3) * 4);
        int i = 0;
        while (i + 3 <= data.length) {
            int chunk = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8) | (data[i + 2] & 0xFF);
            out.append(table[(chunk >>> 18) & 0x3F]).append(table[(chunk >>> 12) & 0x3F])
                    .append(table[(chunk >>> 6) & 0x3F]).append(table[chunk & 0x3F]);
            i += 3;
        }
        int rest = data.length - i;
        if (rest == 1) {
            int chunk = (data[i] & 0xFF) << 16;
            out.append(table[(chunk >>> 18) & 0x3F]).append(table[(chunk >>> 12) & 0x3F]);
        } else if (rest == 2) {
            int chunk = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8);
            out.append(table[(chunk >>> 18) & 0x3F]).append(table[(chunk >>> 12) & 0x3F])
                    .append(table[(chunk >>> 6) & 0x3F]);
        }
        return out.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is required", exception);
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is required", exception);
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private static MinecraftProfile exchangeAuthorizationCode(String code, String verifier, String redirect)
            throws IOException, AccountException {
        Map<String, String> form = new HashMap<String, String>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", clientId());
        form.put("code", code);
        form.put("redirect_uri", redirect);
        form.put("code_verifier", verifier);
        form.put("scope", SCOPE);
        HttpRequest.HttpResponseResult response = HttpRequest.postForm(TOKEN_URL, form);
        if (!response.isSuccess()) {
            throw new AccountException(parseError(response.getBody(), "Microsoft browser login failed"));
        }
        JsonObject token = asObject(response.getBody());
        String access = stringField(token, "access_token");
        String refresh = optionalString(token, "refresh_token");
        if (access.isEmpty()) {
            throw new AccountException("Microsoft token response did not include an access token");
        }
        return complete(access, refresh);
    }

    /**
     * HTML 转义。{@code message} 可能来自回调 URL 的 {@code error_description}，
     * 也就是任何网页都能塞进来的内容，原样拼进 HTML 等于在回环 origin 上执行别人的脚本。
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    private static void writeCallbackPage(Socket socket, int status, String title, String message) {
        try {
            String safeTitle = escapeHtml(title);
            String safeMessage = escapeHtml(message);
            String body = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + safeTitle
                    + "</title></head><body style=\"font-family:sans-serif;background:#10161c;color:#eef3f7;"
                    + "display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;\">"
                    + "<div style=\"max-width:480px;padding:32px;border-radius:20px;background:rgba(255,255,255,0.05);"
                    + "border:1px solid rgba(255,255,255,0.08);\"><h1 style=\"margin:0 0 12px;font-size:22px;\">"
                    + safeTitle + "</h1><p style=\"margin:0;font-size:14px;line-height:1.7;color:#c7d2de;\">"
                    + safeMessage + "</p></div></body></html>";
            byte[] bytes = body.getBytes("UTF-8");
            String header = "HTTP/1.1 " + status + " " + reasonPhrase(status)
                    + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
                    + bytes.length + "\r\nConnection: close\r\n\r\n";
            OutputStream out = socket.getOutputStream();
            out.write(header.getBytes("US-ASCII"));
            out.write(bytes);
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private static String reasonPhrase(int status) {
        return status == 200 ? "OK" : status == 400 ? "Bad Request" : "Error";
    }

    /**
     * 一次浏览器登录的生命周期：构造时端口已绑好，{@link #await} 阻塞到回调到达（或取消/超时），
     * 无论成败都会关闭监听端口。
     */
    public static final class BrowserSession {
        /** 单条连接的读超时。回调请求行几个字节就到了，3 秒足够宽松。 */
        private static final int CLIENT_READ_TIMEOUT_MS = 3000;

        public final String authorizeUrl;
        private final ServerSocket socket;
        private final String expectedPath;
        private final String expectedState;
        private final String verifier;
        private final String redirect;
        private volatile boolean closed;

        BrowserSession(String authorizeUrl, ServerSocket socket, String expectedPath, String expectedState,
                       String verifier, String redirect) {
            this.authorizeUrl = authorizeUrl;
            this.socket = socket;
            this.expectedPath = expectedPath;
            this.expectedState = expectedState;
            this.verifier = verifier;
            this.redirect = redirect;
        }

        /**
         * 当前正在读的那条连接。取消登录时要把它一起关掉，否则它还占着我们的线程。
         * 只在 waitForCode 的循环里赋值，close() 里读，所以是 volatile。
         */
        private volatile Socket activeClient;

        public MinecraftProfile await(BooleanSupplier cancelled) throws IOException, AccountException {
            try {
                String code = waitForCode(cancelled);
                if (cancelled != null && cancelled.getAsBoolean()) {
                    throw new AccountException("cancelled");
                }
                return exchangeAuthorizationCode(code, verifier, redirect);
            } finally {
                close();
            }
        }

        private String waitForCode(BooleanSupplier cancelled) throws IOException, AccountException {
            long deadline = System.currentTimeMillis() + 5L * 60L * 1000L;
            while (System.currentTimeMillis() < deadline) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    throw new AccountException("cancelled");
                }
                Socket client;
                try {
                    client = socket.accept();
                } catch (SocketTimeoutException timeout) {
                    // accept 上挂了 200ms 超时，为的就是能定期回来看一眼 cancelled。
                    continue;
                } catch (IOException exception) {
                    if (closed || (cancelled != null && cancelled.getAsBoolean())) {
                        throw new AccountException("cancelled");
                    }
                    throw exception;
                }
                activeClient = client;
                try {
                    // 这个端口上什么都可能连进来（局域网资产扫描、终端管理 agent、RDP 探活——
                    // 3389 就是远程桌面端口）。没有读超时的话，一条「连上但不发数据」的连接
                    // 就能让线程永久停在 readLine()，真正的回调排在 backlog 里没人读，
                    // 而 close() 只关 ServerSocket、唤不醒它 → 本次登录彻底废掉。
                    client.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), "UTF-8"));
                    String requestLine = reader.readLine();
                    while (true) {
                        String header = reader.readLine();
                        if (header == null || header.isEmpty()) {
                            break;
                        }
                    }
                    Map<String, String> query = parseCallback(requestLine);

                    // state 校验必须在 error 分支之前：任何网页都能让浏览器导航到
                    // http://localhost:3389/oauth?error=...，先看 error 就等于让外部
                    // 无凭据中止玩家这次登录。
                    String state = query.get("state");
                    if (state == null || !expectedState.equals(state)) {
                        // 不匹配就当没看见（这才是注释一直想表达的语义）：可能是别的程序恰好
                        // 敲了这个端口，也可能是 CSRF。都不该杀掉这次登录，继续等下一个连接。
                        if (!query.isEmpty()) {
                            writeCallbackPage(client, 400, "Microsoft login failed",
                                    "Microsoft login callback state did not match.");
                        }
                        continue;
                    }

                    if (query.containsKey("error")) {
                        String message = firstNonEmpty(query.get("error_description"), query.get("error"),
                                "Microsoft login was cancelled.");
                        writeCallbackPage(client, 400, "Microsoft login failed", message);
                        throw new AccountException(message);
                    }
                    String code = query.get("code");
                    if (code == null || code.trim().isEmpty()) {
                        writeCallbackPage(client, 400, "Microsoft login failed",
                                "Microsoft login callback did not contain an authorization code.");
                        throw new AccountException("Microsoft login callback did not contain an authorization code");
                    }
                    writeCallbackPage(client, 200, "Microsoft login completed",
                            "You can return to FPSMaster now.");
                    return code.trim();
                } catch (SocketTimeoutException timeout) {
                    // 连上了但没在超时内发完请求行，不是我们要的回调，等下一个。
                    continue;
                } finally {
                    activeClient = null;
                    try {
                        client.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            throw new AccountException("Timed out waiting for Microsoft browser login");
        }

        private Map<String, String> parseCallback(String requestLine) {
            Map<String, String> query = new HashMap<String, String>();
            if (requestLine == null) {
                return query;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return query;
            }
            String pathAndQuery = parts[1];
            int q = pathAndQuery.indexOf('?');
            String path = q < 0 ? pathAndQuery : pathAndQuery.substring(0, q);
            // 只认精确路径和它的斜杠变体。原先那句 startsWith 会把 /oauthXYZ 也当成回调，
            // 前两个条件本来就被它包含，纯属冗余。
            if (!path.equals(expectedPath) && !(expectedPath + "/").equals(path)) {
                return query;
            }
            if (q < 0 || q + 1 >= pathAndQuery.length()) {
                return query;
            }
            String[] pairs = pathAndQuery.substring(q + 1).split("&");
            for (int i = 0; i < pairs.length; i++) {
                String pair = pairs[i];
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                query.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
            return query;
        }

        public void close() {
            closed = true;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            // 光关监听端口不够：已经 accept 到的那条连接得单独关，读操作才会立刻抛出来。
            Socket client = activeClient;
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * @deprecated Azure 应用未注册为公共/移动客户端，Microsoft 会以
     *         {@code AADSTS70002 "The client application must be marked as 'mobile'"} 拒绝
     *         设备码请求。保留是为了「哪天应用注册改了」还能直接用，不要接到 UI 上。
     *         正常路径走 {@link #beginBrowserLogin()}。
     */
    @Deprecated
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
