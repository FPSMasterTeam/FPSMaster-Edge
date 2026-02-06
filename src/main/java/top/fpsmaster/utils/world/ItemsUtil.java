package top.fpsmaster.utils.world;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import top.fpsmaster.utils.core.Utility;

import java.util.List;

public class ItemsUtil {

    public static ItemStack getItemStack(Item item) {
        return new ItemStack(item);
    }
    public static ItemStack getItemStackWithMetadata(Item item, int metadata) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.setItemDamage(metadata);
        return itemStack;

    }
    public static boolean isSplashPotion(ItemStack stack){
        if (stack == null) {
            return false;
        }
        return stack.getItem() instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata());
    }
    public static boolean isPotionEffect(ItemStack stack,int potionID) {
        if (stack == null || !(stack.getItem() instanceof ItemPotion)) {
            return false;
        }
        List<PotionEffect> effects = ((ItemPotion) stack.getItem()).getEffects(stack);
        if (effects != null && !effects.isEmpty()) {
            for (PotionEffect effect : effects) {
                // 治疗药水的 Potion ID 是 Potion.heal.id
                if (effect.getPotionID() == potionID) {
                    return true;
                }
            }
        }
        return false;
    }
    public static void renderItem(ItemStack itemStack, float x, float y) {
        ItemStack copyItem = itemStack.copy();
        copyItem.stackSize = 1;
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        Utility.mc.getRenderItem().renderItemIntoGUI(copyItem, (int) x, (int) y);
        GlStateManager.popMatrix();
        Utility.mc.getRenderItem().renderItemOverlays(Utility.mc.fontRendererObj, copyItem, (int) x, (int) y);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}


