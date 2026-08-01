package top.fpsmaster.features.impl.render;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.fpsmaster.utils.core.Utility.mc;

public class ChatAvatarCache {
    private static final int MAX_CACHE_SIZE = 256;
    private static final long IN_GAME_TTL_MS = 30_000L;
    private static final long MISS_TTL_MS = 2L * 60L * 1000L;
    private static final int MAX_SENDER_SEPARATOR_DISTANCE = 16;
    private static final String[] WHISPER_COMMAND_PREFIXES = {"/tell ", "/msg ", "/whisper ", "/w ", "/t "};
    private static final Pattern TIMESTAMP_PREFIX =
            Pattern.compile("^(?:\\[\\d\\d:\\d\\d(?::\\d\\d)?(?: [AP]M)?]|<\\d\\d:\\d\\d>)\\s*");

    // Every texture here is owned by the vanilla skin manager (tab list / world entities), so the
    // cache never allocates or frees GL textures of its own — eviction is a plain map removal.
    private static final LinkedHashMap<String, AvatarEntry> CACHE = new LinkedHashMap<String, AvatarEntry>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AvatarEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static final LinkedHashMap<String, SenderEntry> SENDER_CACHE = new LinkedHashMap<String, SenderEntry>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SenderEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private enum State {
        READY,
        MISS
    }

    private static class AvatarEntry {
        private State state;
        private ResourceLocation texture;
        private long expireAt;

        private AvatarEntry(State state, ResourceLocation texture, long expireAt) {
            this.state = state;
            this.texture = texture;
            this.expireAt = expireAt;
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

    public static ResourceLocation getAvatar(IChatComponent chatComponent) {
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

        if (entry != null && entry.expireAt > now) {
            return entry.state == State.READY ? entry.texture : null;
        }

        ResourceLocation inGameSkin = getInGameSkin(playerName);
        if (inGameSkin != null) {
            put(playerName, new AvatarEntry(State.READY, inGameSkin, now + IN_GAME_TTL_MS));
            return inGameSkin;
        }

        // Only players the vanilla skin manager already knows about get a head; an unresolved name
        // is cached as a miss rather than fetched, so chat rendering never touches the network.
        put(playerName, new AvatarEntry(State.MISS, null, now + MISS_TTL_MS));
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
            CACHE.put(cacheKey(playerName), entry);
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

        // Highest-confidence signal first: vanilla and most chat plugins attach a "/msg <name> "
        // suggest-command to the sender's name specifically, which beats any text heuristic.
        PlayerCandidate player = findByWhisperCommand(chatComponent);
        if (player == null) {
            player = findOnlinePlayer(text);
        }
        SenderEntry sender = player != null
                ? new SenderEntry(player.realName, player.texture, now + 1_000L)
                : new SenderEntry(extractLikelySenderName(text), null, now + 1_000L);
        synchronized (SENDER_CACHE) {
            SENDER_CACHE.put(cacheKey, sender);
        }
        return sender;
    }

    /**
     * Resolves the sender from a whisper click event rather than from the rendered text. Vanilla's chat
     * decorator — and most server chat plugins — hang a {@code /msg <name> } suggest-command on the
     * sender's name component, so when it is present it identifies the sender exactly.
     */
    private static PlayerCandidate findByWhisperCommand(IChatComponent chatComponent) {
        String name = findWhisperTarget(chatComponent);
        if (name == null) {
            return null;
        }
        return new PlayerCandidate(name, name, getInGameSkin(name));
    }

    private static String findWhisperTarget(IChatComponent chatComponent) {
        try {
            // IChatComponent iterates itself plus all nested siblings.
            for (IChatComponent part : chatComponent) {
                if (part == null || part.getChatStyle() == null) {
                    continue;
                }
                String name = whisperTargetOf(part.getChatStyle().getChatClickEvent());
                if (name != null) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // Malformed component trees from servers must not break chat rendering.
        }
        return null;
    }

    private static String whisperTargetOf(ClickEvent click) {
        if (click == null || click.getValue() == null) {
            return null;
        }
        if (click.getAction() != ClickEvent.Action.SUGGEST_COMMAND
                && click.getAction() != ClickEvent.Action.RUN_COMMAND) {
            return null;
        }
        String command = click.getValue().trim();
        for (String prefix : WHISPER_COMMAND_PREFIXES) {
            if (!command.regionMatches(true, 0, prefix, 0, prefix.length())) {
                continue;
            }
            String rest = command.substring(prefix.length()).trim();
            int space = rest.indexOf(' ');
            if (space >= 0) {
                rest = rest.substring(0, space);
            }
            return isValidPlayerName(rest) ? rest : null;
        }
        return null;
    }

    private static PlayerCandidate findOnlinePlayer(String text) {
        List<PlayerCandidate> candidates = getOnlinePlayers();
        if (candidates.isEmpty()) {
            return null;
        }

        String head = stripTimestamp(text);
        head = head.substring(0, Math.min(head.length(), 96));

        // A name immediately followed by a chat delimiter is the sender. Prefer the rightmost such
        // match so rank/guild prefixes ("[Guild] MVP Player > hi") don't win over the real name.
        PlayerCandidate best = findBestOnlinePlayer(head, candidates, true, true);
        if (best != null) {
            return best;
        }
        // No delimiter anywhere means a system line ("Player joined the game"). Here the subject
        // comes first, so take the leftmost match — rightmost would pick "Alice" out of
        // "Bob was slain by Alice".
        return firstDelimiter(head) < 0 ? findBestOnlinePlayer(head, candidates, false, false) : null;
    }

    private static PlayerCandidate findBestOnlinePlayer(String text, List<PlayerCandidate> candidates,
                                                       boolean requireChatSeparator, boolean preferRightmost) {
        PlayerCandidate best = null;
        int bestIndex = -1;
        int bestLength = -1;
        for (PlayerCandidate candidate : candidates) {
            if (candidate == null || candidate.matchName == null || candidate.matchName.isEmpty()) {
                continue;
            }
            int index = indexOfSenderName(text, candidate.matchName, requireChatSeparator, preferRightmost);
            if (index < 0) {
                continue;
            }
            int length = candidate.matchName.length();
            boolean better = best == null
                    || (preferRightmost ? index > bestIndex : index < bestIndex)
                    || (index == bestIndex && length > bestLength);
            if (better) {
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

    private static int indexOfSenderName(String text, String playerName, boolean requireChatSeparator, boolean preferRightmost) {
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
            // Word-boundary guards on both sides: "tom" must not match inside "custom" or "tomato".
            boolean beforeOk = before < 0 || !isWordChar(lowerText.charAt(before));
            boolean afterOk = after >= lowerText.length() || !isWordChar(lowerText.charAt(after));
            if (beforeOk && afterOk && (!requireChatSeparator || hasChatSeparatorAfter(text, after))) {
                best = index;
                if (!preferRightmost) {
                    return best;
                }
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

    /**
     * Strips a leading clock stamp that another chat mod may have prepended, e.g. {@code [12:34] } or
     * {@code <12:34> }. Without this the sender name is no longer at the head of the line and the
     * delimiter-anchored match degrades.
     */
    private static String stripTimestamp(String text) {
        Matcher matcher = TIMESTAMP_PREFIX.matcher(text);
        return matcher.lookingAt() ? text.substring(matcher.end()) : text;
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

    /** Minecraft account-name alphabet — deliberately ASCII-only, used to validate a real username. */
    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    /**
     * Word-boundary alphabet for match guarding. Unlike {@link #isNameChar} this is Unicode-aware, so a
     * CJK display name sitting flush against other CJK text is not mistaken for a standalone token.
     */
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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

    private static String cacheKey(String playerName) {
        return playerName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static String senderCacheKey(String text) {
        return text.substring(0, Math.min(text.length(), 96)).toLowerCase(Locale.ROOT);
    }
}
