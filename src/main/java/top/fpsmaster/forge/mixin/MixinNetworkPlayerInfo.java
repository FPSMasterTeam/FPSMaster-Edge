package top.fpsmaster.forge.mixin;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fpsmaster.forge.api.INetworkPlayerInfo;

@Mixin(NetworkPlayerInfo.class)
public class MixinNetworkPlayerInfo implements INetworkPlayerInfo {

    @Shadow
    private ResourceLocation locationSkin;
    @Shadow
    private ResourceLocation locationCape;
    @Shadow
    private String skinType;

    @Override
    public ResourceLocation getSkin() {
        return locationSkin;
    }

    @Override
    public ResourceLocation getCape() {
        return locationCape;
    }

    @Override
    public String getType() {
        return skinType;
    }

    @Override
    public void setSkin(ResourceLocation skin) {
        this.locationSkin = skin;
    }

    @Override
    public void setCape(ResourceLocation cape) {
        this.locationCape = cape;
    }

    @Override
    public void setType(String skinType) {
        this.skinType = skinType;
    }
}
