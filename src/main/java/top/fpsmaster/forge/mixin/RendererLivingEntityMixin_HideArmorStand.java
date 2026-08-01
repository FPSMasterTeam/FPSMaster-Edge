package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.mixin.accessor.RenderAccessor;

/**
 * Drops an armour stand's body while keeping whatever is written above it.
 *
 * <p>Servers use armour stands for two unrelated things. Some are furniture — a posed model wearing
 * armour, a display holding an item — and those are the ones with a cost worth removing. The rest
 * are invisible and exist only to carry a line of text: shop labels, kill feeds, region markers,
 * every piece of floating writing in the world. Hiding the model and the label together would delete
 * the second kind entirely, which is the mistake {@code IgnoreStands} made when it was on by default.
 *
 * <p>So this cancels the render and draws the name itself. {@code ArmorStandRenderer} does not
 * override {@code doRender}, so the injection is on {@code RendererLivingEntity} with a type check
 * rather than on the armour stand's own renderer.
 */
@Mixin(RendererLivingEntity.class)
public class RendererLivingEntityMixin_HideArmorStand {

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideArmorStand(EntityLivingBase entity, double x, double y, double z,
                                          float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Performance.using && Performance.hideArmorStands.getValue()
                && entity instanceof EntityArmorStand) {
            ((RenderAccessor) this).fpsmaster$invokeRenderName(entity, x, y, z);
            ci.cancel();
        }
    }
}
