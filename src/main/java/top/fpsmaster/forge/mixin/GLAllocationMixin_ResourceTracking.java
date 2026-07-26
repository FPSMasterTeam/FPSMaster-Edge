package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.GLAllocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Counts display-list allocation and release, for leak detection.
 *
 * <p>Heap figures alone cannot see this: display lists, textures and framebuffers live in driver
 * memory, so a client can leak them steadily while the Java heap looks flat. What makes the counters
 * usable as a leak test is the benchmark's steady state — during a measurement window the scene is
 * settled and the camera repeats a fixed loop, so a net rise in live GL objects over that window is
 * a leak rather than normal churn.
 *
 * <p>Both allocation sites are counted, including the batch overload, because a leak that only shows
 * up under one of them would otherwise be invisible.
 */
@Mixin(GLAllocation.class)
public class GLAllocationMixin_ResourceTracking {

    @Inject(method = "generateDisplayLists", at = @At("RETURN"))
    private static void fpsmasterCountAllocation(int count, CallbackInfoReturnable<Integer> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.displayListsAllocated += count;
        }
    }

    @Inject(method = "deleteDisplayLists(II)V", at = @At("RETURN"))
    private static void fpsmasterCountBatchRelease(int list, int count, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.displayListsReleased += count;
        }
    }

    @Inject(method = "deleteDisplayLists(I)V", at = @At("RETURN"))
    private static void fpsmasterCountRelease(int list, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.displayListsReleased++;
        }
    }
}
