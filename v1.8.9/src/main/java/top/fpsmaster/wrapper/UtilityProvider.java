package top.fpsmaster.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.api.provider.game.IUtilityProvider;

public class UtilityProvider implements IUtilityProvider {
    public String getResourcePath(ResourceLocation resourceLocation){
        return resourceLocation.getResourcePath();
    }

    public double getDistanceToEntity(Entity e1, Entity e2){
        return e1.getDistanceToEntity(e2);
    }

    public boolean isItemEnhancementEmpty(ItemStack i){
        if (i.getEnchantmentTagList() == null)
            return true;
        return i.getEnchantmentTagList().hasNoTags();
    }

    public int getPotionIconIndex(PotionEffect effect){
        Potion potion = Potion.potionTypes[effect.getPotionID()];

        int i = potion.getStatusIconIndex();
        return i;
    }

    public IChatComponent makeChatComponent(String msg) {
        return new ChatComponentText(msg);
    }
}
