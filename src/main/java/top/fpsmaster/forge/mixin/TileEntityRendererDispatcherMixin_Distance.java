package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Brings the block-entity render distance in from vanilla's own limit.
 *
 * <p>Chests, enchanting tables, banners and skulls are drawn one at a time in immediate mode every
 * frame — matrix setup, a light lookup, a texture bind and a small model each — which measured at
 * roughly 2us apiece. There is no invisible saving left in that: Forge already frustum-tests every
 * block entity before dispatching it, and vanilla already skips anything past
 * {@code getMaxRenderDistanceSquared}. The only remaining lever is to draw fewer of them, and that
 * is visible — a chest at forty blocks stops having a lid.
 *
 * <p>So this is a knob rather than an optimisation, and it is off by default. It is worth having
 * because the cost is entirely proportional to how many are in view: on the recorded Hypixel
 * matches the whole pass is under 3us a frame and this would change nothing, while on a build with
 * a thousand of them in sight it is most of the frame.
 *
 * <p>Sign text is not covered here and has its own setting, because that cost is the text rather
 * than the block, and it can be dropped at a distance where it is unreadable rather than at a
 * distance where the block visibly disappears.
 */
@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin_Distance {

    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$cullByDistance(TileEntity tileEntity, float partialTicks,
                                          int destroyStage, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.blockEntitiesAttempted++;
        }
        if (!Performance.using || !Performance.blockEntityCulling.getValue()) {
            return;
        }
        double limit = Performance.blockEntityDistance.getValue().doubleValue();
        double distanceSq = tileEntity.getDistanceSq(TileEntityRendererDispatcher.staticPlayerX,
                TileEntityRendererDispatcher.staticPlayerY,
                TileEntityRendererDispatcher.staticPlayerZ);
        if (distanceSq <= limit * limit) {
            return;
        }
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.blockEntitiesCulled++;
        }
        ci.cancel();
    }
}
