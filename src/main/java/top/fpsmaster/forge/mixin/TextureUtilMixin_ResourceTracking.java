package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

/** Counts GL texture allocation and release. See {@link GLAllocationMixin_ResourceTracking}. */
@Mixin(TextureUtil.class)
public class TextureUtilMixin_ResourceTracking {

    @Inject(method = "glGenTextures", at = @At("RETURN"))
    private static void fpsmasterCountAllocation(CallbackInfoReturnable<Integer> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.texturesAllocated++;
        }
    }

    @Inject(method = "deleteTexture", at = @At("RETURN"))
    private static void fpsmasterCountRelease(int textureId, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.texturesReleased++;
        }
    }
}
