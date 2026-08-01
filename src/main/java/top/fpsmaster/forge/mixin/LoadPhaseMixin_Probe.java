package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureUtil;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;

/**
 * Prices the load-time work the frame-based instrumentation cannot see.
 *
 * <p>Roadmap §4.6 named four costs — upload, mipmap generation, atlas stitching, image decode — and
 * only the first was ever implemented, let alone measured. Nothing here happens inside a frame, so
 * no profiler section reaches it: the measured window opens after the world is up and warm.
 *
 * <p>Upload turned out to be 160ms before its optimisation and 36.5ms after, which is enough to
 * make the other three worth a number rather than an opinion. This produces the number. Whether
 * anything should be done about them is a separate decision and is meant to be made from these
 * figures rather than ahead of them.
 *
 * <p>Timed unconditionally rather than behind {@code BenchmarkMode}: each of these runs a handful of
 * times during a load, so a {@code nanoTime} pair either side is free, and gating it would mean the
 * numbers only exist in runs nobody looks at.
 */
@Mixin(TextureUtil.class)
public class LoadPhaseMixin_Probe {

    @Unique
    private static long edge$mipmapStarted;

    @Unique
    private static long edge$decodeStarted;

    /**
     * PNG decode, which is most of what the atlas build spends its time on.
     *
     * <p>The atlas is 757ms of a load and its two known parts — mipmap generation at 96.5ms and the
     * uploads it drives at 37.8ms — leave about 620ms unaccounted. This says how much of that
     * remainder is {@code ImageIO} and how much is the stitcher itself, which decides which of the
     * two is worth looking at.
     */
    @Inject(method = "readBufferedImage", at = @At("HEAD"))
    private static void edge$beginDecode(InputStream stream,
                                         CallbackInfoReturnable<BufferedImage> cir) {
        edge$decodeStarted = System.nanoTime();
    }

    @Inject(method = "readBufferedImage", at = @At("RETURN"))
    private static void edge$endDecode(InputStream stream,
                                       CallbackInfoReturnable<BufferedImage> cir) {
        BenchCounters.imageDecodeNanos += System.nanoTime() - edge$decodeStarted;
        BenchCounters.imageDecodeCalls++;
    }

    @Inject(method = "generateMipmapData", at = @At("HEAD"))
    private static void edge$beginMipmap(int level, int width, int[][] data,
                                         CallbackInfoReturnable<int[][]> cir) {
        edge$mipmapStarted = System.nanoTime();
    }

    @Inject(method = "generateMipmapData", at = @At("RETURN"))
    private static void edge$endMipmap(int level, int width, int[][] data,
                                       CallbackInfoReturnable<int[][]> cir) {
        BenchCounters.mipmapNanos += System.nanoTime() - edge$mipmapStarted;
        BenchCounters.mipmapCalls++;
    }
}
