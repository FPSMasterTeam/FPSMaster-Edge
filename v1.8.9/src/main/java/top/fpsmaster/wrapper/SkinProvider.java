package top.fpsmaster.wrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import top.fpsmaster.forge.api.INetworkPlayerInfo;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.api.wrapper.SkinWrap;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.os.HttpRequest;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class SkinProvider implements SkinWrap {
    public void updateSkin(String name, String uuid, String skin) {
        if (Wrappers.minecraft().getPlayer() != null && !skin.isEmpty()) {
            Minecraft mc = Minecraft.getMinecraft();
            NetworkPlayerInfo info = new NetworkPlayerInfo(new GameProfile(UUID.fromString(uuid), name));

            for (NetworkPlayerInfo player : mc.getNetHandler().getPlayerInfoMap()) {
                if (player.getGameProfile().getName().equals(name) && player.getGameProfile().getId().toString().equals(uuid)) {
                    info = player;
                }
            }

            HttpRequest.HttpResponseResult httpResponseResult;
            try {
                httpResponseResult = HttpRequest.get("https://api.mojang.com/users/profiles/minecraft/" + skin);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!httpResponseResult.isSuccess()) {
                ClientLogger.error("Failed to get skin " + skin + " for player " + name + " " + httpResponseResult.getStatusCode() + " " + httpResponseResult.getBody());
                return;
            }

            String json = httpResponseResult.getBody();
            Gson gson = new GsonBuilder().create();
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            if (jsonObject != null && jsonObject.has("id")) {
                String raw = jsonObject.getAsJsonPrimitive("id").getAsString();
                GameProfile gameProfile = new GameProfile(toUUID(raw), jsonObject.getAsJsonPrimitive("name").getAsString());
                gameProfile = mc.getSessionService().fillProfileProperties(gameProfile, false);
                NetworkPlayerInfo finalInfo = info;
                mc.getSkinManager().loadProfileTextures(gameProfile, (typeIn, location, profileTexture) -> {
                    if (Objects.requireNonNull(typeIn) == MinecraftProfileTexture.Type.SKIN) {
                        ((INetworkPlayerInfo) finalInfo).setSkin(location);
                        ((INetworkPlayerInfo) finalInfo).setType(profileTexture.getMetadata("model"));
                        if (finalInfo.getSkinType() == null) {
                            ((INetworkPlayerInfo) finalInfo).setType("default");
                        }
                    }
                }, false);
            }
        }
    }

    private static UUID toUUID(String uuid) {
        return UUID.fromString(uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" +
                uuid.substring(16, 20) + "-" + uuid.substring(20, 32));
    }
}
