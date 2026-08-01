package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;

import java.util.List;

/**
 * Measures animated texture updating before deciding whether to optimise it.
 *
 * <p>Vanilla walks every animated sprite in the atlas each tick and uploads the next frame, whether
 * or not anything on screen uses it. Restricting that to sprites the visible chunks actually
 * reference is a known optimisation, but it needs sprite usage tracked through chunk compilation,
 * which is a substantial change. Whether that is worth building depends on what the work costs
 * here, which is a question about this installation's texture set rather than about the technique —
 * so it gets measured first.
 */
@Mixin(TextureMap.class)
public class TextureMapMixin_AnimationTiming {

    @Shadow
    private List<TextureAtlasSprite> listAnimatedSprites;

    @Inject(method = "updateAnimations", at = @At("HEAD"))
    private void edge$beginAnimations(CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_TEXTURE_ANIM);
            BenchCounters.animatedSpritesTotal += listAnimatedSprites.size();
        }
    }

    @Inject(method = "updateAnimations", at = @At("RETURN"))
    private void edge$endAnimations(CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_TEXTURE_ANIM);
        }
    }
}
