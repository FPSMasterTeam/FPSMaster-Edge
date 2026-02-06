package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.BetterFishingRod;

@Mixin(RenderItem.class)
public class MixinRenderItem {
    @Inject(method = "renderItemModelForEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelResourceLocation;<init>(Ljava/lang/String;Ljava/lang/String;)V", ordinal = 0))
    public void onRenderItem(ItemStack stack, EntityLivingBase entityToRenderFor, ItemCameraTransforms.TransformType cameraTransformType, CallbackInfo ci) {
        if (BetterFishingRod.using) {
            GL11.glLineWidth(BetterFishingRod.stringWidth.getValue().floatValue());
        }
    }
}
