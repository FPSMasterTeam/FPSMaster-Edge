package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;

/**
 * Times the atlas build, which is stitching and every sprite's upload together.
 *
 * <p>Separate class from the mipmap probe because a mixin with two targets has to resolve every
 * injection against both, and {@code loadTextureAtlas} exists on only one of them.
 *
 * <p>Kept as one bracket rather than split: the stitcher and the uploads it drives are not
 * separable without instrumenting the sprite loop, and the first question is whether the atlas is a
 * large enough share of loading to be worth that.
 */
@Mixin(TextureMap.class)
public class TextureMapMixin_StitchProbe {

    @Unique
    private static long edge$stitchStarted;

    @Inject(method = "loadTextureAtlas", at = @At("HEAD"))
    private void edge$beginStitch(IResourceManager manager, CallbackInfo ci) {
        edge$stitchStarted = System.nanoTime();
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"))
    private void edge$endStitch(IResourceManager manager, CallbackInfo ci) {
        BenchCounters.atlasStitchNanos += System.nanoTime() - edge$stitchStarted;
    }
}
