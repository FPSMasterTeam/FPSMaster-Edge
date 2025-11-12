package top.fpsmaster.wrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.forge.api.INetworkPlayerInfo;
import top.fpsmaster.api.wrapper.SkinWrap;
import top.fpsmaster.utils.os.HttpRequest;

import java.util.Map;
import java.util.UUID;

public class SkinProvider implements SkinWrap {
    public void updateSkin(String name, String uuid, String skin) {
        try {
            if (Minecraft.getMinecraft().player != null) {
                Minecraft mc = Minecraft.getMinecraft();
                NetworkPlayerInfo info = new NetworkPlayerInfo(new GameProfile(UUID.fromString(uuid), name));

                for (NetworkPlayerInfo player : mc.getConnection().getPlayerInfoMap()) {
                    if (player.getGameProfile().getName().equals(name) && player.getGameProfile().getId().toString().equals(uuid)) {
                        info = player;
                    }
                }

                String json = HttpRequest.get("https://api.mojang.com/users/profiles/minecraft/" + skin);
                Gson gson = new GsonBuilder().create();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject != null && jsonObject.has("id")) {
                    String raw = jsonObject.getAsJsonPrimitive("id").getAsString();
                    GameProfile gameProfile = new GameProfile(toUUID(raw), jsonObject.getAsJsonPrimitive("name").getAsString());
                    gameProfile = mc.getSessionService().fillProfileProperties(gameProfile, false);
                    NetworkPlayerInfo finalInfo = info;
                    mc.getSkinManager().loadProfileTextures(gameProfile, (typeIn, location, profileTexture) -> {
                        switch (typeIn) {
                            case SKIN:
                                Map<MinecraftProfileTexture.Type, ResourceLocation> textures = ((INetworkPlayerInfo) finalInfo).getTextures();
                                textures.put(MinecraftProfileTexture.Type.SKIN, location);
                                ((INetworkPlayerInfo)finalInfo).setTextures(textures);
                                ((INetworkPlayerInfo)finalInfo).setType(profileTexture.getMetadata("model"));
                                if (finalInfo.getSkinType() == null) {
                                    ((INetworkPlayerInfo) finalInfo).setType("default");
                                }
                                break;
                        }
                    }, false);
                }
            }
        }catch (Exception e){
            
        }
    }

    private UUID toUUID(String uuid) {
        return UUID.fromString(uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" +
                uuid.substring(16, 20) + "-" + uuid.substring(20, 32));
    }
}
