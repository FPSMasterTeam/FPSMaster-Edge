package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.FixedInventory;

import java.util.Iterator;

import static top.fpsmaster.utils.Utility.mc;

@Mixin(InventoryEffectRenderer.class)
public class MixinInventoryEffectRenderer extends MixinGuiContainer{

    @Shadow
    private boolean hasActivePotionEffects;

    @Inject(method = "updateActivePotionEffects", at = @At("RETURN"))
    private void renderPotionEffects(CallbackInfo ci) {
        boolean hasVisibleEffect = false;

        for (PotionEffect potioneffect : mc.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[potioneffect.getPotionID()];
            if (potion.shouldRender(potioneffect)) {
                hasVisibleEffect = true;
                break;
            }
        }

        if (!mc.thePlayer.getActivePotionEffects().isEmpty() && hasVisibleEffect) {
            if (FixedInventory.using){
                this.guiLeft = (Minecraft.getMinecraft().currentScreen.width - this.xSize) / 2;
            }else {
                this.guiLeft = 160 + (Minecraft.getMinecraft().currentScreen.width - this.xSize - 200) / 2;
            }
            this.hasActivePotionEffects = true;
        } else {
            this.guiLeft = (Minecraft.getMinecraft().currentScreen.width - this.xSize) / 2;
            this.hasActivePotionEffects = false;
        }
    }
}
