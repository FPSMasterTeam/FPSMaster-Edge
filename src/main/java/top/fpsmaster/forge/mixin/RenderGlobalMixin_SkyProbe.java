package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.Experiments;

/**
 * Ceiling probe for the sky pass: deletes it and lets the frame time answer what it was worth.
 *
 * <p>The profiler puts a seventh of the frame in renderSky, which is a surprising amount for a dome,
 * a gradient and two quads. Either that is real and worth caching, or the attribution is wrong.
 * Removing the work is the only way to tell which, and it is much cheaper than building the cache to
 * find out.
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_SkyProbe {

    @Inject(method = "renderSky(FI)V", at = @At("HEAD"), cancellable = true)
    private void skipSky(float partialTicks, int pass, CallbackInfo callback) {
        if (Experiments.active(Experiments.NO_SKY)) {
            callback.cancel();
        }
    }
}
