package top.fpsmaster.forge.mixin;

import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.util.Random;

/**
 * The end portal's half of the same switch. Its display tick does nothing but spawn one smoke
 * particle, so cancelling the method is exactly cancelling the particle.
 */
@Mixin(BlockEndPortal.class)
public class BlockEndPortalMixin_HideParticles {

    @Inject(method = "randomDisplayTick", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$skipPortalParticle(World worldIn, BlockPos pos, IBlockState state,
                                              Random rand, CallbackInfo ci) {
        if (Performance.using && Performance.hidePortalParticles.getValue()) {
            ci.cancel();
        }
    }
}
