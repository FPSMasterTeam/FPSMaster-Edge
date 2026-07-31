package top.fpsmaster.features.impl.render;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.fpsmaster.utils.core.Utility.mc;

public class ChatAvatarCache {
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");
    private static final Gson GSON = new Gson();
    private static final int MAX_CACHE_SIZE = 256;
    private static final int REQUEST_TIMEOUT_MS = 2500;
    private static final long IN_GAME_TTL_MS = 30_000L;
    private static final long MOJANG_TTL_MS = 60L * 60L * 1000L;
    private static final long MISS_TTL_MS = 2L * 60L * 1000L;
    private static final long MIN_REQUEST_INTERVAL_MS = 750L;
    private static final int MAX_PARALLEL_REQUESTS = 2;
    private static final int MAX_SENDER_SEPARATOR_DISTANCE = 16;
    private static final int MIN_DISPLAY_NAME_SUFFIX_LENGTH = 3;

    private static final LinkedHashMap<String, AvatarEntry> CACHE = new LinkedHashMap<String, AvatarEntry>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AvatarEntry> eldest) {
            boolean remove = size() > MAX_CACHE_SIZE;
            if (remove) {
                deleteTexture(eldest.getValue());
            }
            return remove;
        }
    };

    private static final LinkedHashMap<String, SenderEntry> SENDER_CACHE = new LinkedHashMap<String, SenderEntry>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SenderEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static int activeRequests;
    private static long lastRequestAt;

    private enum State {
        READY,
        LOADING,
        MISS
    }

    private static class AvatarEntry {
        private State state;
        private ResourceLocation texture;
        private long expireAt;
        private boolean dynamicTexture;

        private AvatarEntry(State state, ResourceLocation texture, long expireAt) {
            this(state, texture, expireAt, false);
        }

        private AvatarEntry(State state, ResourceLocation texture, long expireAt, boolean dynamicTexture) {
            this.state = state;
            this.texture = texture;
            this.expireAt = expireAt;
            this.dynamicTexture = dynamicTexture;
        }
    }

    private static class SenderEntry {
        private String playerName;
        private ResourceLocation texture;
        private long expireAt;

        private SenderEntry(String playerName, ResourceLocation texture, long expireAt) {
            this.playerName = playerName;
            this.texture = texture;
            this.expireAt = expireAt;
        }
    }

    private static class PlayerCandidate {
        private String realName;
        private String matchName;
        private ResourceLocation texture;

        private PlayerCandidate(String realName, String matchName, ResourceLocation texture) {
            this.realName = realName;
            this.matchName = matchName;
            this.texture = texture;
        }
    }

    public static ResourceLocation getAvatar(IChatComponent chatComponent, boolean mojangFallback) {
        SenderEntry sender = findSender(chatComponent);
        if (sender == null || sender.playerName == null || sender.playerName.trim().isEmpty()) {
            return null;
        }
        String playerName = sender.playerName;
        long now = System.currentTimeMillis();
        AvatarEntry entry = get(playerName);
        if (sender.texture != null) {
            if (entry == null || entry.state != State.READY || entry.texture != sender.texture || entry.expireAt <= now) {
                put(playerName, new AvatarEntry(State.READY, sender.texture, now + IN_GAME_TTL_MS));
            }
            return sender.texture;
        }

        if (entry != null && entry.expireAt > now && entry.state == State.READY) {
            return entry.texture;
        }
        if (entry != null && entry.expireAt > now && entry.state != State.READY) {
            return null;
        }

        ResourceLocation inGameSkin = getInGameSkin(playerName);
        if (inGameSkin != null) {
            put(playerName, new AvatarEntry(State.READY, inGameSkin, now + IN_GAME_TTL_MS));
            return inGameSkin;
        }

        if (mojangFallback && isValidPlayerName(playerName)) {
            queueMojangLoad(playerName);
        } else {
            put(playerName, new AvatarEntry(State.MISS, null, now + MISS_TTL_MS));
        }
        return null;
    }

    public static void drawHead(ResourceLocation skin, int x, int y, int size, int alpha) {
        if (skin == null || alpha <= 3) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getTextureManager() == null) {
            return;
        }

        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha / 255.0F)));
        minecraft.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (depthEnabled) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }
    }

    private static AvatarEntry get(String playerName) {
        synchronized (CACHE) {
            return CACHE.get(cacheKey(playerName));
        }
    }

    private static void put(String playerName, AvatarEntry entry) {
        synchronized (CACHE) {
            String key = cacheKey(playerName);
            AvatarEntry previous = CACHE.put(key, entry);
            if (previous != null && previous != entry && previous.texture != entry.texture) {
                deleteTexture(previous);
            }
        }
    }

    private static SenderEntry findSender(IChatComponent chatComponent) {
        if (chatComponent == null) {
            return null;
        }
        String text = chatComponent.getUnformattedText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String cacheKey = senderCacheKey(text);
        long now = System.currentTimeMillis();
        synchronized (SENDER_CACHE) {
            SenderEntry entry = SENDER_CACHE.get(cacheKey);
            if (entry != null && entry.expireAt > now) {
                return entry;
            }
        }

        PlayerCandidate player = findOnlinePlayer(text);
        SenderEntry sender = player != null
                ? new SenderEntry(player.realName, player.texture, now + 1_000L)
                : new SenderEntry(extractLikelySenderName(text), null, now + 1_000L);
        synchronized (SENDER_CACHE) {
            SENDER_CACHE.put(cacheKey, sender);
        }
        return sender;
    }

    private static PlayerCandidate findOnlinePlayer(String text) {
        List<PlayerCandidate> candidates = getOnlinePlayers();
        if (candidates.isEmpty()) {
            return null;
        }

        String head = text.substring(0, Math.min(text.length(), 96));
        PlayerCandidate best = findBestOnlinePlayer(head, candidates, true);
        if (best != null) {
            return best;
        }
        best = findBestDisplayNameSuffix(head, candidates);
        if (best != null) {
            return best;
        }
        return firstDelimiter(head) < 0 ? findBestOnlinePlayer(head, candidates, false) : null;
    }

    private static PlayerCandidate findBestDisplayNameSuffix(String text, List<PlayerCandidate> candidates) {
        String senderName = extractSenderDisplayName(text);
        if (senderName.isEmpty()) {
            return null;
        }

        PlayerCandidate best = null;
        int bestLength = 0;
        for (PlayerCandidate candidate : candidates) {
            if (candidate == null || candidate.matchName == null || candidate.matchName.isEmpty()) {
                continue;
            }
            int suffixLength = commonSuffixLength(senderName, candidate.matchName);
            if (suffixLength >= MIN_DISPLAY_NAME_SUFFIX_LENGTH && suffixLength > bestLength) {
                best = candidate;
                bestLength = suffixLength;
            }
        }
        return best;
    }

    private static PlayerCandidate findBestOnlinePlayer(String text, List<PlayerCandidate> candidates, boolean requireChatSeparator) {
        PlayerCandidate best = null;
        int bestIndex = -1;
        int bestLength = -1;
        for (PlayerCandidate candidate : candidates) {
            if (candidate == null || candidate.matchName == null || candidate.matchName.isEmpty()) {
                continue;
            }
            int index = indexOfSenderName(text, candidate.matchName, requireChatSeparator);
            if (index < 0) {
                continue;
            }
            int length = candidate.matchName.length();
            if (index > bestIndex || (index == bestIndex && length > bestLength)) {
                bestIndex = index;
                bestLength = length;
                best = candidate;
            }
        }
        return best;
    }

    private static List<PlayerCandidate> getOnlinePlayers() {
        ArrayList<PlayerCandidate> candidates = new ArrayList<>();
        try {
            if (mc != null && mc.getNetHandler() != null) {
                Collection<NetworkPlayerInfo> infos = mc.getNetHandler().getPlayerInfoMap();
                if (infos != null) {
                    for (NetworkPlayerInfo info : infos) {
                        if (info == null || info.getGameProfile() == null || info.getGameProfile().getName() == null) {
                            continue;
                        }
                        String realName = info.getGameProfile().getName();
                        addCandidate(candidates, realName, realName, info.getLocationSkin());
                        if (info.getDisplayName() != null) {
                            addCandidate(candidates, realName, info.getDisplayName().getUnformattedText(), info.getLocationSkin());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Net handler data can disappear while changing servers.
        }

        try {
            if (mc != null && mc.theWorld != null && mc.theWorld.playerEntities != null) {
                for (Object object : mc.theWorld.playerEntities) {
                    if (object instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) object;
                        String realName = player.getName();
                        ResourceLocation texture = player instanceof AbstractClientPlayer ? ((AbstractClientPlayer) player).getLocationSkin() : null;
                        addCandidate(candidates, realName, realName, texture);
                        if (player.getDisplayName() != null) {
                            addCandidate(candidates, realName, player.getDisplayName().getUnformattedText(), texture);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // World player list can be mutated by the client thread during shutdown.
        }
        return candidates;
    }

    private static void addCandidate(List<PlayerCandidate> candidates, String realName, String matchName, ResourceLocation texture) {
        if (realName == null || matchName == null || matchName.trim().isEmpty()) {
            return;
        }
        String trimmed = matchName.trim();
        for (PlayerCandidate candidate : candidates) {
            if (candidate.realName.equalsIgnoreCase(realName) && candidate.matchName.equalsIgnoreCase(trimmed)) {
                return;
            }
        }
        candidates.add(new PlayerCandidate(realName, trimmed, texture));
    }

    private static int indexOfSenderName(String text, String playerName, boolean requireChatSeparator) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        int best = -1;
        int from = 0;
        while (from < lowerText.length()) {
            int index = lowerText.indexOf(lowerName, from);
            if (index < 0) {
                break;
            }
            int before = index - 1;
            int after = index + lowerName.length();
            boolean beforeOk = before < 0 || !isNameChar(lowerText.charAt(before));
            boolean afterOk = after >= lowerText.length() || !isNameChar(lowerText.charAt(after));
            if (beforeOk && afterOk && (!requireChatSeparator || hasChatSeparatorAfter(text, after))) {
                best = index;
            }
            from = index + 1;
        }
        return best;
    }

    private static boolean hasChatSeparatorAfter(String text, int from) {
        int limit = Math.min(text.length(), from + MAX_SENDER_SEPARATOR_DISTANCE);
        for (int i = from; i < limit; i++) {
            if (isChatDelimiter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String extractSenderDisplayName(String text) {
        int delimiter = firstDelimiter(text);
        if (delimiter < 0) {
            return "";
        }
        int start = delimiter;
        while (start > 0 && Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }
        int nameStart = start;
        while (nameStart > 0 && !Character.isWhitespace(text.charAt(nameStart - 1)) && text.charAt(nameStart - 1) != ']') {
            nameStart--;
        }
        return text.substring(nameStart, start).trim();
    }

    private static int commonSuffixLength(String first, String second) {
        int firstIndex = first.length() - 1;
        int secondIndex = second.length() - 1;
        int length = 0;
        while (firstIndex >= 0 && secondIndex >= 0) {
            char firstChar = Character.toLowerCase(first.charAt(firstIndex));
            char secondChar = Character.toLowerCase(second.charAt(secondIndex));
            if (firstChar != secondChar) {
                break;
            }
            length++;
            firstIndex--;
            secondIndex--;
        }
        return length;
    }

    private static String extractLikelySenderName(String text) {
        int delimiter = firstDelimiter(text);
        if (delimiter < 0) {
            return null;
        }
        String prefix = text.substring(0, Math.min(delimiter, 96));
        String[] tokens = prefix.split("[^A-Za-z0-9_]+");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            if (isValidPlayerName(token)) {
                return token;
            }
        }
        return null;
    }

    private static int firstDelimiter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (isChatDelimiter(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isChatDelimiter(char character) {
        return character == ':' || character == '：' || character == '>' || character == '»' || character == '›';
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static boolean isValidPlayerName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (!isNameChar(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static ResourceLocation getInGameSkin(String playerName) {
        try {
            if (mc != null && mc.getNetHandler() != null) {
                Collection<NetworkPlayerInfo> infos = mc.getNetHandler().getPlayerInfoMap();
                if (infos != null) {
                    for (NetworkPlayerInfo info : infos) {
                        if (info == null || info.getGameProfile() == null) {
                            continue;
                        }
                        GameProfile profile = info.getGameProfile();
                        if (profile.getName() != null && profile.getName().equalsIgnoreCase(playerName)) {
                            return info.getLocationSkin();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to world lookup.
        }

        try {
            if (mc != null && mc.theWorld != null) {
                EntityPlayer player = mc.theWorld.getPlayerEntityByName(playerName);
                if (player instanceof AbstractClientPlayer) {
                    return ((AbstractClientPlayer) player).getLocationSkin();
                }
            }
        } catch (Exception ignored) {
            // Runtime player objects can be null while changing worlds.
        }
        return null;
    }

    private static ResourceLocation getDefaultSkin(String playerName) {
        UUID uuid = null;
        if (playerName != null && !playerName.trim().isEmpty()) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        }
        if (uuid == null) {
            return STEVE_SKIN;
        }
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    private static void queueMojangLoad(String playerName) {
        long now = System.currentTimeMillis();
        synchronized (CACHE) {
            AvatarEntry entry = CACHE.get(cacheKey(playerName));
            if (entry != null && entry.state == State.LOADING) {
                return;
            }
            if (activeRequests >= MAX_PARALLEL_REQUESTS || now - lastRequestAt < MIN_REQUEST_INTERVAL_MS) {
                deleteTexture(CACHE.put(cacheKey(playerName), new AvatarEntry(State.MISS, null, now + 5_000L)));
                return;
            }
            activeRequests++;
            lastRequestAt = now;
            deleteTexture(CACHE.put(cacheKey(playerName), new AvatarEntry(State.LOADING, null, now + REQUEST_TIMEOUT_MS * 4L)));
        }

        FPSMaster.async.runnable(() -> {
            try {
                ResourceLocation skin = loadMojangSkin(playerName);
                if (skin != null) {
                    put(playerName, new AvatarEntry(State.READY, skin, System.currentTimeMillis() + MOJANG_TTL_MS, true));
                } else {
                    put(playerName, new AvatarEntry(State.MISS, null, System.currentTimeMillis() + MISS_TTL_MS));
                }
            } catch (Exception exception) {
                ClientLogger.warn("Failed to load chat avatar from Mojang API");
                put(playerName, new AvatarEntry(State.MISS, null, System.currentTimeMillis() + MISS_TTL_MS));
            } finally {
                synchronized (CACHE) {
                    activeRequests = Math.max(0, activeRequests - 1);
                }
            }
        });
    }

    private static ResourceLocation loadMojangSkin(String playerName) throws Exception {
        String encodedName = URLEncoder.encode(playerName, "UTF-8");
        JsonObject userProfile = readJson("https://api.mojang.com/users/profiles/minecraft/" + encodedName);
        if (userProfile == null || !userProfile.has("id") || userProfile.get("id").isJsonNull()) {
            return null;
        }
        String playerId = userProfile.get("id").getAsString();
        if (playerId == null || playerId.trim().isEmpty()) {
            return null;
        }
        String skinUrl = readSkinUrl(playerId.replace("-", ""));
        if (skinUrl == null || skinUrl.trim().isEmpty()) {
            return null;
        }
        BufferedImage image = readImage(skinUrl);
        if (image == null || image.getWidth() < 64 || image.getHeight() < 32) {
            return null;
        }
        BufferedImage skinImage = normalizeSkin(image);
        final ResourceLocation[] result = new ResourceLocation[1];
        AtomicBoolean accepted = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                ResourceLocation texture = Minecraft.getMinecraft()
                        .getTextureManager()
                        .getDynamicTextureLocation("fpsmaster_chat_avatar_" + cacheKey(playerName), new DynamicTexture(skinImage));
                if (accepted.get()) {
                    result[0] = texture;
                } else {
                    deleteTexture(texture);
                }
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            accepted.set(false);
            return null;
        }
        return result[0];
    }

    private static String readSkinUrl(String playerId) throws Exception {
        JsonObject profile = readJson("https://sessionserver.mojang.com/session/minecraft/profile/" + playerId);
        if (profile == null) {
            return null;
        }
        JsonArray properties = profile.getAsJsonArray("properties");
        if (properties == null) {
            return null;
        }
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(property.get("name").getAsString()) || !property.has("value")) {
                continue;
            }
            String decoded = new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8);
            JsonObject decodedJson = GSON.fromJson(decoded, JsonObject.class);
            if (decodedJson == null || !decodedJson.has("textures") || !decodedJson.get("textures").isJsonObject()) {
                continue;
            }
            JsonObject textures = decodedJson.getAsJsonObject("textures");
            if (textures.has("SKIN") && textures.get("SKIN").isJsonObject()) {
                JsonObject skin = textures.getAsJsonObject("SKIN");
                if (skin.has("url") && !skin.get("url").isJsonNull()) {
                    return skin.get("url").getAsString();
                }
            }
        }
        return null;
    }

    private static JsonObject readJson(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(REQUEST_TIMEOUT_MS);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static BufferedImage readImage(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(REQUEST_TIMEOUT_MS);
        try (InputStream inputStream = connection.getInputStream()) {
            return ImageIO.read(inputStream);
        }
    }

    private static BufferedImage normalizeSkin(BufferedImage source) {
        BufferedImage converted = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, Math.min(source.getWidth(), 64), Math.min(source.getHeight(), 64), null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private static void deleteTexture(AvatarEntry entry) {
        if (entry != null && entry.dynamicTexture) {
            deleteTexture(entry.texture);
        }
    }

    private static void deleteTexture(ResourceLocation texture) {
        if (texture == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getTextureManager() == null) {
            return;
        }
        TextureManager textureManager = minecraft.getTextureManager();
        minecraft.addScheduledTask(() -> textureManager.deleteTexture(texture));
    }

    private static String cacheKey(String playerName) {
        return playerName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static String senderCacheKey(String text) {
        return text.substring(0, Math.min(text.length(), 96)).toLowerCase(Locale.ROOT);
    }
}
