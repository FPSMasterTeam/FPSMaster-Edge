package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Stops drawing skulls.
 *
 * <p>{@code BlockSkull} extends {@code BlockContainer}, whose render type is -1, so a skull has no
 * chunk geometry at all — the renderer here draws the whole thing, and cancelling it is the whole
 * feature. Separate from the distance culling on the same method because this is unconditional:
 * a player who turns it on wants skulls gone rather than gone at range.
 *
 * <p>Only the drawing is skipped. The block entity stays in the world, keeps its profile and comes
 * straight back when the switch goes off.
 */
@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin_HideSkulls {

    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideSkulls(TileEntity tileEntity, float partialTicks,
                                      int destroyStage, CallbackInfo ci) {
        if (Performance.using && Performance.hideSkulls.getValue() && tileEntity instanceof TileEntitySkull) {
            ci.cancel();
        }
    }
}
