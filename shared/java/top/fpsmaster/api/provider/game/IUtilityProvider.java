package top.fpsmaster.api.provider.game;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.api.provider.IProvider;

public interface IUtilityProvider extends IProvider {
    String getResourcePath(ResourceLocation resourceLocation);
    double getDistanceToEntity(Entity e1, Entity e2);
    boolean isItemEnhancementEmpty(ItemStack i);
    int getPotionIconIndex(PotionEffect effect);
    Object makeChatComponent(String msg);
}