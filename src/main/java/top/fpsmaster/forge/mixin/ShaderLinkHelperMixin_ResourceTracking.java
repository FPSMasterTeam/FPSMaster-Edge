package top.fpsmaster.forge.mixin;

import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Counts GL program allocation and release for vanilla shader passes.
 *
 * <p>This is the churn that matters: a {@code ShaderGroup} is rebuilt whenever MotionBlur is
 * toggled or the window is resized, and each pass in it holds a program. The client's own
 * {@code ShaderUtil} programs are counted where they are created, since they never go through here.
 */
@Mixin(ShaderLinkHelper.class)
public class ShaderLinkHelperMixin_ResourceTracking {

    @Inject(method = "createProgram", at = @At("RETURN"))
    private void fpsmasterCountAllocation(CallbackInfoReturnable<Integer> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.shaderProgramsAllocated++;
        }
    }

    @Inject(method = "deleteShader", at = @At("RETURN"))
    private void fpsmasterCountRelease(ShaderManager manager, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.shaderProgramsReleased++;
        }
    }
}
