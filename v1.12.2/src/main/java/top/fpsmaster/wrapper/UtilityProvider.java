package top.fpsmaster.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import top.fpsmaster.api.provider.game.IUtilityProvider;

public class UtilityProvider implements IUtilityProvider {
    public String getResourcePath(ResourceLocation resourceLocation){
        return resourceLocation.getPath();
    }

    public double getDistanceToEntity(Entity e1, Entity e2){
        return e1.getDistance(e2);
    }

    public boolean isItemEnhancementEmpty(ItemStack i){
        return i.getEnchantmentTagList().isEmpty();
    }

    public int getPotionIconIndex(PotionEffect effect){
        return effect.getPotion().getStatusIconIndex();
    }

    public ITextComponent makeChatComponent(String msg) {
        return new TextComponentString(msg);
    }
}
