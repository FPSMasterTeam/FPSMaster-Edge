package top.fpsmaster.modules.client.api;

import com.google.gson.JsonObject;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.client.api.exception.ApiException;
import top.fpsmaster.modules.client.api.model.*;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.HttpRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FPSMasterApiClient {
    /**
     * 饿汉式：懒加载那版的 {@code getInstance()} 没有同步，两条线程同时第一次调用会各造一个
     * 实例，各自持有一份 {@code currentUser} 缓存和一个 {@code profileRefreshing} 闸门。
     * 构造函数什么都不做，类加载时建掉没有代价。
     */
    private static final FPSMasterApiClient INSTANCE = new FPSMasterApiClient();

    private volatile UserInfo currentUser;

    /** 防止「界面每帧调 refreshUserInfoAsync」变成每帧一个请求。 */
    private final AtomicBoolean profileRefreshing = new AtomicBoolean(false);

    private FPSMasterApiClient() {
        // Tokens are loaded by AuthService.initialize()
    }

    public static FPSMasterApiClient getInstance() {
        return INSTANCE;
    }

    // ================== Authentication ================== //

    /**
     * Login with username and password
     */
    public CompletableFuture<ApiResponse<LoginResponse>> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("usernameOrEmail", username);
                payload.addProperty("password", password);

                ClientLogger.info("Attempting login to: " + FPSMasterConstants.Endpoints.LAUNCHER_LOGIN);

                HttpRequest.HttpResponseResult response = HttpRequest.postJson(
                        FPSMasterConstants.Endpoints.LAUNCHER_LOGIN,
                        payload,
                        getDefaultHeaders()
                );

                ClientLogger.info("Login response status: " + response.getStatusCode());

                // Try to parse as JSON for detailed error info
                String responseBody = response.getBody();
                if (responseBody != null && !responseBody.isEmpty()) {
                    try {
                        JsonObject jsonResponse = FPSMasterGson.getInstance().fromJson(responseBody, JsonObject.class);
                        ApiResponse<LoginResponse> apiResponse = ApiResponse.fromJson(
                                jsonResponse, LoginResponse.class, response.getStatusCode());

                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            LoginResponse loginData = apiResponse.getData();
                            AuthService.getInstance().saveTokens(loginData.getToken(), null);
                            setCurrentUserFromView(loginData.getCurrentUserView());
                            // 单独兜住：token 已经存下了，饰品列表刷新失败不该把这次登录
                            // 报成「Parse error」——外面那个 catch 会把它一起吞进去。
                            try {
                                top.fpsmaster.cosmetic.CosmeticManager.getInstance().refreshOwned();
                            } catch (Exception cosmeticEx) {
                                ClientLogger.warn("Cosmetic refresh after login failed: " + cosmeticEx.getMessage());
                            }
                            ClientLogger.info("Login successful for user: " + username);
                        } else {
                            ClientLogger.warn("Login failed: " + apiResponse.getMessage());
                            // 带上状态码，界面才能把「密码错」和「连不上」分流到不同文案；
                            // serverMessage 也要跟着走，否则界面无法判断这句话是不是后端原话。
                            return ApiResponse.error(response.getStatusCode(),
                                    apiResponse.getMessage(), null, apiResponse.hasServerMessage());
                        }

                        return apiResponse;
                    } catch (Exception parseEx) {
                        ClientLogger.error("Failed to parse login response: " + parseEx.getMessage());
                        return ApiResponse.error(response.getStatusCode(), "Parse error", responseBody);
                    }
                }

                return ApiResponse.error(response.getStatusCode(), "Empty response", "Server returned empty response");
            } catch (IOException e) {
                ClientLogger.error("Login request failed: " + e.getMessage());
                return ApiResponse.error(ApiResponse.NETWORK_ERROR, "Network error", e.getMessage());
            } catch (Exception e) {
                ClientLogger.error("Login error: " + e.getMessage());
                return ApiResponse.error(ApiResponse.NETWORK_ERROR, "Unknown error", e.getMessage());
            }
        });
    }

    /**
     * Login with callback
     */
    public void login(String username, String password, Consumer<ApiResponse<LoginResponse>> callback) {
        login(username, password).thenAccept(callback);
    }

    /**
     * Logout current user
     */
    public CompletableFuture<ApiResponse<Void>> logout() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> headers = getAuthHeaders();
                HttpRequest.HttpResponseResult response = HttpRequest.post(
                        FPSMasterConstants.Endpoints.LOGOUT,
                        null,
                        new HashMap<>(headers)
                );

                signOutLocally();
                ClientLogger.info("Logged out successfully");

                return ApiResponse.fromJson(parseResponse(response), response.getStatusCode());
            } catch (Exception e) {
                ClientLogger.error("Logout error: " + e.getMessage());
                signOutLocally();
                return ApiResponse.error(-1, "Logout error", e.getMessage());
            }
        });
    }

    // ================== User Info ================== //

    /**
     * Get current user information
     */
    public CompletableFuture<ApiResponse<UserInfo>> getUserInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 这次请求属于哪个 token 必须在发出前钉住：isLoggedIn() 只回答「现在有没有
                // 有效 token」，答不了「还是不是同一个账号」，换号之后旧响应照样会被写回缓存。
                String tokenAtRequest = AuthService.getInstance().getAccessToken();
                HttpRequest.HttpResponseResult response = HttpRequest.get(
                        FPSMasterConstants.Endpoints.USER_INFO,
                        getAuthHeaders()
                );

                JsonObject jsonResponse = parseResponse(response);
                // /api/v1/me 返回的是后端的 UserView：id 是 UUID 字符串、createdAt 是 ISO 时间串，
                // 而 UserInfo 把这两个字段声明成 Long，直接按 UserInfo 解析会在 Gson 层抛异常。
                // 登录响应里的 CurrentUserView 就是同一个形状，复用它再走同一套映射。
                ApiResponse<CurrentUserView> viewResponse = ApiResponse.fromJson(
                        jsonResponse, CurrentUserView.class, response.getStatusCode());
                if (!viewResponse.isSuccess() || viewResponse.getData() == null) {
                    return ApiResponse.error(response.getStatusCode(),
                            viewResponse.getMessage(), "Profile payload missing",
                            viewResponse.hasServerMessage());
                }

                UserInfo profile = toUserInfo(viewResponse.getData());
                // 这次请求发出之后玩家可能已经登出、或者换了个账号登进来：不判一下就会把
                // 旧账号的用户名写回缓存，界面继续显示他的名字直到重启。
                if (tokenStillCurrent(tokenAtRequest)) {
                    this.currentUser = profile;
                }
                // 返回刚构造的那个而不是共享字段：并发刷新/并发登出时共享字段可能已经
                // 被别人换掉或清空，却还声称 success。
                return ApiResponse.success(viewResponse.getMessage(), profile);
            } catch (IOException e) {
                ClientLogger.error("Get user info failed: " + e.getMessage());
                return ApiResponse.error(ApiResponse.NETWORK_ERROR, "Network error", e.getMessage());
            } catch (ApiException e) {
                ClientLogger.error("Get user info failed: " + e.getMessage());
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    /**
     * 当前登录用户的缓存快照，**绝不发起请求、绝不阻塞**。
     *
     * <p>界面每帧都会问名字，所以这里必须是纯读。想让缓存变新用
     * {@link #refreshUserInfoAsync()}。
     *
     * @return 缓存里的用户，未登录或还没拉到时返回 null
     */
    public UserInfo cachedUser() {
        return currentUser;
    }

    /**
     * 已登录但缓存是空的时候异步补一次 profile；正在补的时候不重复发。
     *
     * <p>玩家上次登录留下的 token 还有效时，重启后 {@link #currentUser} 是空的，
     * 界面只能显示「未知账号」，所以打开相关界面时调一次这个。
     *
     * <p>缓存已经有值就直接返回：调用点在 {@code initGui()} 里，而 MC 每次改变窗口大小
     * 都会重跑 {@code initGui()}，不判空的话拖一下窗口就是好几个白发的请求。
     */
    public void refreshUserInfoAsync() {
        if (!isLoggedIn() || currentUser != null
                || !profileRefreshing.compareAndSet(false, true)) {
            return;
        }
        // 被拒的是「这一个」token，不是「当前」token。服务端卡住时这个请求能挂满 15 秒，
        // 而玩家恰恰在服务端抽风时才会退出重登——不钉住 token 的话，迟到的 401 会把
        // 刚刚登录成功的新凭据连同 auth.json 一起抹掉，界面上不给任何提示。
        final String tokenAtRequest = AuthService.getInstance().getAccessToken();
        getUserInfo().whenComplete(new java.util.function.BiConsumer<ApiResponse<UserInfo>, Throwable>() {
            @Override
            public void accept(ApiResponse<UserInfo> response, Throwable error) {
                try {
                    if (error != null) {
                        ClientLogger.warn("Profile refresh failed: " + error);
                        return;
                    }
                    // token 已被服务端吊销时 isLoggedIn() 仍是 true（它只看本地过期时间），
                    // 于是界面一直显示「未知账号」、每次开界面都再打一次注定 401 的请求。
                    // 这里把本地凭据清掉，让界面自然回落到未登录态。
                    //
                    // 只认 401。/api/v1/me 的 403 只有「账号被封」一条出口（CurrentUser.requireUser），
                    // 而封禁可能是临时的；再加上 CDN/WAF 的挑战页也是 403，把凭据销毁掉等于
                    // 让玩家莫名其妙掉线、封禁到期还得重新输密码。凭据只在服务端明确说
                    // 「这个 token 不认」时才丢。
                    if (response != null && !response.isSuccess()
                            && response.getCode() == 401
                            && tokenStillCurrent(tokenAtRequest)) {
                        signOutLocally();
                        ClientLogger.warn("Stored credentials rejected by server, signed out locally");
                    }
                } finally {
                    profileRefreshing.set(false);
                }
            }
        });
    }

    /**
     * 发起请求时钉住的 token 是不是仍然是当前 token。
     *
     * <p>换号、登出、乃至续签都会让它变，此时那条在途请求的结果就不再属于「现在这个人」。
     */
    private boolean tokenStillCurrent(String tokenAtRequest) {
        // 空串也得挡掉：未登录时 getAccessToken() 可能给空串，两个空串一比就"仍然当前"，
        // 迟到的响应会被当成有效结果写回缓存。（Nova 侧用的是 isNullOrBlank，对齐。）
        return tokenAtRequest != null && !tokenAtRequest.isEmpty()
                && tokenAtRequest.equals(AuthService.getInstance().getAccessToken());
    }

    /**
     * 只清本地会话，不发登出请求。
     *
     * <p>{@code logout()} 和「服务端已吊销 token」两条路必须做完全一样的清理，否则账号
     * UI 回到未登录态、饰品却还挂着上一个账号的已拥有列表，后续同步全是无 token 的 401。
     */
    private void signOutLocally() {
        AuthService.getInstance().clearTokens();
        this.currentUser = null;
        top.fpsmaster.cosmetic.CosmeticManager.getInstance().refreshOwned();
    }

    /**
     * @deprecated 名字有误导性：它曾经会同步阻塞等一次 HTTP。用 {@link #cachedUser()}
     *             读缓存、用 {@link #refreshUserInfoAsync()} 刷新。
     */
    @Deprecated
    public UserInfo getCurrentUser() {
        return cachedUser();
    }

    public CompletableFuture<ApiResponse<OwnedItemView[]>> getOwnedItems() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.get(
                        FPSMasterConstants.Endpoints.OWNED_ITEMS,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), OwnedItemView[].class, response.getStatusCode());
            } catch (IOException e) {
                ClientLogger.error("Get owned items failed: " + e.getMessage());
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                ClientLogger.error("Get owned items failed: " + e.getMessage());
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    public CompletableFuture<ApiResponse<CosmeticItem[]>> getCatalogItems() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.get(
                        FPSMasterConstants.Endpoints.CATALOG_ITEMS,
                        getDefaultHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), CosmeticItem[].class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    public CompletableFuture<ApiResponse<JsonObject>> purchaseItem(long itemId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("itemId", itemId);
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(
                        FPSMasterConstants.Endpoints.PURCHASES,
                        payload,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), JsonObject.class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    // ================== Cosmetic Loadout ================== //

    public CompletableFuture<ApiResponse<CosmeticLoadoutView>> getCosmeticLoadout() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.get(
                        FPSMasterConstants.Endpoints.COSMETIC_LOADOUT,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), CosmeticLoadoutView.class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    /**
     * @param capeItemId null clears the cape slot; {@code backItemId} and {@code builtinWingsEnabled}
     *                   are mutually exclusive, which the backend also enforces
     * @param wingScale  a real scale such as 1.20, never a 0..1 slider position
     */
    public CompletableFuture<ApiResponse<CosmeticLoadoutView>> putCosmeticLoadout(
            String capeItemId, String backItemId, boolean builtinWingsEnabled,
            float wingScale, boolean capeAnimationEnabled) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                if (capeItemId == null) {
                    payload.add("capeItemId", com.google.gson.JsonNull.INSTANCE);
                } else {
                    payload.addProperty("capeItemId", capeItemId);
                }
                if (backItemId == null) {
                    payload.add("backItemId", com.google.gson.JsonNull.INSTANCE);
                } else {
                    payload.addProperty("backItemId", backItemId);
                }
                payload.addProperty("builtinWingsEnabled", builtinWingsEnabled);
                payload.addProperty("wingScale", wingScale);
                payload.addProperty("capeAnimationEnabled", capeAnimationEnabled);
                HttpRequest.HttpResponseResult response = HttpRequest.putJson(
                        FPSMasterConstants.Endpoints.COSMETIC_LOADOUT,
                        payload,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), CosmeticLoadoutView.class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    /**
     * Batch-resolves other players' loadouts. Players who are unknown or have not linked a Minecraft
     * account are absent from the response rather than present with a null loadout.
     *
     * @param minecraftUuids canonical dashed lowercase, at most 200; anything else is rejected 400
     */
    public CompletableFuture<ApiResponse<ResolvedLoadoutView[]>> resolveLoadouts(java.util.Collection<String> minecraftUuids) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                com.google.gson.JsonArray uuids = new com.google.gson.JsonArray();
                for (String uuid : minecraftUuids) {
                    uuids.add(new com.google.gson.JsonPrimitive(uuid));
                }
                JsonObject payload = new JsonObject();
                payload.add("minecraftUuids", uuids);
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(
                        FPSMasterConstants.Endpoints.RESOLVE_LOADOUTS,
                        payload,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), ResolvedLoadoutView[].class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    // ================== Minecraft account link ================== //

    public CompletableFuture<ApiResponse<MinecraftLinkChallenge>> requestMinecraftLinkChallenge() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(
                        FPSMasterConstants.Endpoints.MINECRAFT_LINK_CHALLENGE,
                        new JsonObject(),
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), MinecraftLinkChallenge.class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    /** Only the challenge id and the public username travel here — never a Minecraft access token. */
    public CompletableFuture<ApiResponse<JsonObject>> confirmMinecraftLink(String challengeId, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("challengeId", challengeId);
                payload.addProperty("username", username);
                HttpRequest.HttpResponseResult response = HttpRequest.postJson(
                        FPSMasterConstants.Endpoints.MINECRAFT_LINK_CONFIRM,
                        payload,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), JsonObject.class, response.getStatusCode());
            } catch (IOException e) {
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    // ================== Token Management ================== //

    public boolean isLoggedIn() {
        return AuthService.getInstance().isLoggedIn();
    }

    public String getAccessToken() {
        return AuthService.getInstance().getAccessToken();
    }

    private void setCurrentUser(UserInfo user) {
        this.currentUser = user;
    }

    private void setCurrentUserFromView(CurrentUserView view) {
        UserInfo mapped = toUserInfo(view);
        if (mapped != null) {
            this.currentUser = mapped;
        }
    }

    /** 纯映射，不碰缓存。后端的 id 是 UUID 串，装不进 {@code Long}，只能留空。 */
    private UserInfo toUserInfo(CurrentUserView view) {
        if (view == null) {
            return null;
        }
        UserInfo userInfo = new UserInfo();
        // 后端 UserView.id 是 UUID 字符串（见 ViewMapper.kt）。以前这里 Long.parseLong
        // 必然抛异常再兜底成 0L，等于把「不存在的 id」写成一个看着像真的值。留 null 更诚实。
        userInfo.setId(null);
        userInfo.setUsername(view.getUsername());
        userInfo.setEmail(view.getEmail());
        userInfo.setDisplayName(view.getUsername());
        userInfo.setAvatar(view.getAvatarUrl());
        userInfo.setLevel(view.getLevel());
        userInfo.setExp((long) view.getExperience());
        userInfo.setEmailVerified(view.isEmailVerified());
        return userInfo;
    }

    // ================== Utility Methods ================== //

    private JsonObject parseResponse(HttpRequest.HttpResponseResult response) throws ApiException {
        if (!response.isSuccess()) {
            throw new ApiException(response.getStatusCode(), "HTTP error: " + response.getStatusCode());
        }

        String body = response.getBody();
        if (body == null || body.isEmpty()) {
            throw new ApiException(-1, "Empty response body");
        }

        try {
            return FPSMasterGson.getInstance().fromJson(body, JsonObject.class);
        } catch (Exception e) {
            throw new ApiException(-1, "Failed to parse JSON response: " + e.getMessage());
        }
    }

    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", FPSMasterConstants.USER_AGENT);
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = getDefaultHeaders();
        String token = AuthService.getInstance().getAccessToken();
        if (token != null && !token.isEmpty()) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }
}
