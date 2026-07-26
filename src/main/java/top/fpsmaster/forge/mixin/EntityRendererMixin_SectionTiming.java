package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Brackets the whole world render and the first-person hand.
 *
 * <p>{@code frameTotal} exists to make the other sections accountable: with terrain and entities
 * covering 58% of a frame on entity-dense, the remaining 42% was simply unmeasured, and picking the
 * next optimisation target from that position would have been guesswork.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin_SectionTiming {

    @Inject(method = "renderWorldPass", at = @At("HEAD"))
    private void fpsmasterBeginFrame(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_FRAME_TOTAL);
        }
    }

    @Inject(method = "renderWorldPass", at = @At("RETURN"))
    private void fpsmasterEndFrame(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_FRAME_TOTAL);
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void fpsmasterBeginHand(float partialTicks, int pass, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_HAND);
        }
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void fpsmasterEndHand(float partialTicks, int pass, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_HAND);
        }
    }
}
