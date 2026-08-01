package top.fpsmaster.forge.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Leaves decorative blocks out of the chunk mesh.
 *
 * <p>This is the only place worth doing it. {@code renderBlock} is what the chunk builder calls for
 * every block in a section, so a block refused here contributes no geometry at all rather than being
 * built and then skipped at draw time — the saving is in the mesh, in the vertex memory and in the
 * per-frame draw, not just in one of them.
 *
 * <p>It also means the switches only take effect on rebuilt chunks, which is why {@code Performance}
 * calls {@code loadRenderers} when one of them changes.
 */
@Mixin(BlockRendererDispatcher.class)
public class BlockRendererDispatcherMixin_HideBlocks {

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess,
                                     WorldRenderer worldRendererIn, CallbackInfoReturnable<Boolean> cir) {
        if (!Performance.using || !Performance.hidingBlocks) {
            return;
        }
        Block block = state.getBlock();
        boolean hidden;
        if (block instanceof BlockDoublePlant) {
            // The upper half of a double plant does not carry its own variant — it is stored on the
            // lower block and filled in by getActualState, which is what the model lookup further
            // down this method does too. Reading VARIANT straight off the state would call every
            // upper half a sunflower, and hiding double grass would leave its top half standing.
            BlockDoublePlant.EnumPlantType variant = block.getActualState(state, blockAccess, pos)
                    .getValue(BlockDoublePlant.VARIANT);
            boolean grass = variant == BlockDoublePlant.EnumPlantType.GRASS
                    || variant == BlockDoublePlant.EnumPlantType.FERN;
            hidden = grass ? Performance.hideDoubleTallGrass.getValue()
                    : Performance.hideDoubleTallFlowers.getValue();
        } else if (block instanceof BlockTallGrass) {
            hidden = Performance.hideTallGrass.getValue();
        } else if (block instanceof BlockFlower) {
            hidden = Performance.hideFlowers.getValue();
        } else if (block instanceof BlockFence) {
            hidden = Performance.hideFences.getValue();
        } else if (block instanceof BlockFenceGate) {
            hidden = Performance.hideFenceGates.getValue();
        } else {
            return;
        }
        if (hidden) {
            cir.setReturnValue(false);
        }
    }
}
