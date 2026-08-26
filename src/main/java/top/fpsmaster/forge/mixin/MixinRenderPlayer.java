package top.fpsmaster.forge.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.cosmetic.CosmeticManager;
import top.fpsmaster.features.impl.render.CosmeticBackLayer;

/**
 * Attaches the back cosmetic layer to both player models.
 *
 * <p>Registering from the constructor covers the default and slim skin renderers without having to
 * know which one a given player uses, and keeps the layer in the same ordering as vanilla's own.
 */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

    @Shadow
    protected abstract boolean addLayer(LayerRenderer<AbstractClientPlayer> layer);

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/RenderManager;Z)V", at = @At("RETURN"))
    private void addCosmeticBackLayer(RenderManager renderManager, boolean useSmallArms, CallbackInfo ci) {
        addLayer(new CosmeticBackLayer(CosmeticManager.getInstance().wingsRenderer()));
    }
}
