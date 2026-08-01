package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Times the block-entity pass.
 *
 * <p>The entity section already covers this work, but not separately from the mobs and players it
 * runs alongside, so "what do signs and chests cost" could only be answered by subtraction.
 * Bracketing the dispatcher gives it directly, and it accumulates across the whole frame the same
 * way the per-entity brackets do.
 */
@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin_SectionTiming {

    @Inject(method = "renderTileEntity", at = @At("HEAD"))
    private void fpsmasterBeginBlockEntity(TileEntity tileEntity, float partialTicks,
                                           int destroyStage, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_BLOCK_ENTITIES);
        }
    }

    @Inject(method = "renderTileEntity", at = @At("RETURN"))
    private void fpsmasterEndBlockEntity(TileEntity tileEntity, float partialTicks,
                                         int destroyStage, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_BLOCK_ENTITIES);
        }
    }
}
