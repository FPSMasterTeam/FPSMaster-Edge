package top.fpsmaster.forge.mixin;

import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Counts framebuffer allocation and release. See {@link GLAllocationMixin_ResourceTracking}.
 *
 * <p>Every offscreen target in the client — blur chain, minimap, panorama, motion blur, splash —
 * goes through this class, so one bracket here covers all of them without touching their code.
 */
@Mixin(Framebuffer.class)
public class FramebufferMixin_ResourceTracking {

    @Shadow
    public int framebufferObject;

    @Inject(method = "createFramebuffer", at = @At("RETURN"))
    private void fpsmasterCountAllocation(int width, int height, CallbackInfo ci) {
        // Without FBO support vanilla only records the size, and no GL object is created.
        if (BenchmarkMode.ACTIVE && framebufferObject > -1) {
            BenchCounters.framebuffersAllocated++;
        }
    }

    @Inject(method = "deleteFramebuffer", at = @At("HEAD"))
    private void fpsmasterCountRelease(CallbackInfo ci) {
        // Idempotent by design and called twice on several paths (createBindFramebuffer, then an
        // explicit release); only the call that still owns an object is a release.
        if (BenchmarkMode.ACTIVE && framebufferObject > -1) {
            BenchCounters.framebuffersReleased++;
        }
    }
}
