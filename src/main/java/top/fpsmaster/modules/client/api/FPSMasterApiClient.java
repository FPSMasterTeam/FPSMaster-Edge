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
import java.util.function.Consumer;

public class FPSMasterApiClient {
    private static FPSMasterApiClient instance;

    private UserInfo currentUser;

    private FPSMasterApiClient() {
        // Tokens are loaded by AuthService.initialize()
    }

    public static FPSMasterApiClient getInstance() {
        if (instance == null) {
            instance = new FPSMasterApiClient();
        }
        return instance;
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
                        ApiResponse<LoginResponse> apiResponse = ApiResponse.fromJson(jsonResponse, LoginResponse.class);

                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            LoginResponse loginData = apiResponse.getData();
                            AuthService.getInstance().saveTokens(loginData.getToken(), null);
                            setCurrentUserFromView(loginData.getCurrentUserView());
                            top.fpsmaster.cosmetic.CosmeticManager.getInstance().refreshOwned();
                            ClientLogger.info("Login successful for user: " + username);
                        } else {
                            ClientLogger.warn("Login failed: " + apiResponse.getMessage());
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
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (Exception e) {
                ClientLogger.error("Login error: " + e.getMessage());
                return ApiResponse.error(-1, "Unknown error", e.getMessage());
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

                AuthService.getInstance().clearTokens();
                this.currentUser = null;
                top.fpsmaster.cosmetic.CosmeticManager.getInstance().refreshOwned();
                ClientLogger.info("Logged out successfully");

                return ApiResponse.fromJson(parseResponse(response));
            } catch (Exception e) {
                ClientLogger.error("Logout error: " + e.getMessage());
                AuthService.getInstance().clearTokens();
                this.currentUser = null;
                top.fpsmaster.cosmetic.CosmeticManager.getInstance().refreshOwned();
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
                HttpRequest.HttpResponseResult response = HttpRequest.get(
                        FPSMasterConstants.Endpoints.USER_INFO,
                        getAuthHeaders()
                );

                JsonObject jsonResponse = parseResponse(response);
                ApiResponse<UserInfo> apiResponse = ApiResponse.fromJson(jsonResponse, UserInfo.class);

                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                    setCurrentUser(apiResponse.getData());
                }

                return apiResponse;
            } catch (IOException e) {
                ClientLogger.error("Get user info failed: " + e.getMessage());
                return ApiResponse.error(-1, "Network error", e.getMessage());
            } catch (ApiException e) {
                ClientLogger.error("Get user info failed: " + e.getMessage());
                return ApiResponse.error(e.getCode(), e.getErrorMessage(), e.getMessage());
            }
        });
    }

    /**
     * Get current user info synchronously
     */
    public UserInfo getCurrentUser() {
        if (currentUser != null) {
            return currentUser;
        }
        if (!isLoggedIn()) {
            return null;
        }

        try {
            ApiResponse<UserInfo> response = getUserInfo().get();
            return response.isSuccess() ? response.getData() : null;
        } catch (Exception e) {
            ClientLogger.error("Failed to get current user: " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<ApiResponse<OwnedItemView[]>> getOwnedItems() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.HttpResponseResult response = HttpRequest.get(
                        FPSMasterConstants.Endpoints.OWNED_ITEMS,
                        getAuthHeaders()
                );
                return ApiResponse.fromJson(parseResponse(response), OwnedItemView[].class);
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
                return ApiResponse.fromJson(parseResponse(response), CosmeticItem[].class);
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
                return ApiResponse.fromJson(parseResponse(response), JsonObject.class);
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
                return ApiResponse.fromJson(parseResponse(response), CosmeticLoadoutView.class);
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
                return ApiResponse.fromJson(parseResponse(response), CosmeticLoadoutView.class);
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
                return ApiResponse.fromJson(parseResponse(response), ResolvedLoadoutView[].class);
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
                return ApiResponse.fromJson(parseResponse(response), MinecraftLinkChallenge.class);
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
                return ApiResponse.fromJson(parseResponse(response), JsonObject.class);
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
        if (view == null) {
            return;
        }
        UserInfo userInfo = new UserInfo();
        try {
            userInfo.setId(Long.parseLong(view.getId()));
        } catch (NumberFormatException e) {
            userInfo.setId(0L);
        }
        userInfo.setUsername(view.getUsername());
        userInfo.setEmail(view.getEmail());
        userInfo.setDisplayName(view.getUsername());
        userInfo.setAvatar(view.getAvatarUrl());
        userInfo.setLevel(view.getLevel());
        userInfo.setExp((long) view.getExperience());
        userInfo.setEmailVerified(view.isEmailVerified());
        this.currentUser = userInfo;
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
