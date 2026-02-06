package top.fpsmaster.forge.api;

import net.minecraft.util.ResourceLocation;

public interface INetworkPlayerInfo {
    ResourceLocation getSkin();
    ResourceLocation getCape();
    String getType();

    void setSkin(ResourceLocation skin);
    void setCape(ResourceLocation cape);
    void setType(String skinType);
}
