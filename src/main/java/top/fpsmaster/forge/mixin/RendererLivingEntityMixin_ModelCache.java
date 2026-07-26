package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.utils.render.ModelDisplayListCache;

/**
 * Routes a model's geometry through {@link ModelDisplayListCache}.
 *
 * <p>Redirects the {@code mainModel.render} call inside {@code renderModel} rather than injecting
 * around {@code renderModel} itself, which keeps the recorded list to geometry alone: the texture
 * bind has already happened, and the colour and depth-mask handling for invisible entities sits
 * outside this call. Nothing that varies independently of the pose can be baked in.
 */
@Mixin(RendererLivingEntity.class)
public class RendererLivingEntityMixin_ModelCache {

    @Redirect(method = "renderModel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelBase;render"
                    + "(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void edge$renderCachedModel(ModelBase model, Entity entity, float limbSwing,
                                        float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                        float headPitch, float scale) {
        if (!Performance.using || !Performance.cacheModelLists.getValue()) {
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }
        if (ModelDisplayListCache.replay(model, scale)) {
            return;
        }
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        ModelDisplayListCache.endCompile();
    }
}
