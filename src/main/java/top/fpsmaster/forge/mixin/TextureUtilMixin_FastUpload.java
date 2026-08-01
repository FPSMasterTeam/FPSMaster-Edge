package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import top.fpsmaster.modules.logger.ClientLogger;
import java.awt.image.SinglePixelPackedSampleModel;
import java.nio.IntBuffer;

/**
 * Uploads a texture without allocating sixteen megabytes to do it, and without going through
 * {@code getRGB} when the image can simply be read.
 *
 * <p>Vanilla's upload sizes its staging array as {@code 4194304 / width * width}, which is four
 * million ints whatever the texture is — a 16MB heap allocation per uploaded image, for a 16x16
 * icon as readily as for an atlas. A resource load is hundreds of images, and a pack switch is all
 * of them again.
 *
 * <p>Then it fills that array with {@code BufferedImage.getRGB}, which resolves every pixel through
 * the image's colour model one at a time. When the image is already 32-bit ARGB backed by an int
 * array — which is what the atlas and most loaded PNGs are — the pixels are in exactly the layout
 * being asked for and can be copied straight out.
 *
 * <p>Both paths are conservative. The array is reused rather than resized down, so a large upload
 * leaves it large; the direct read is only taken when the image is {@code TYPE_INT_ARGB} with a
 * single-bank buffer, no offset and a scanline stride equal to its width, and anything else falls
 * back to {@code getRGB} into the same reused array. Neither changes a single uploaded pixel.
 *
 * <p>Uploads before the client's modules are initialised — the splash screen's own — take vanilla's
 * path, because the setting cannot be read yet. The target is resource reloads, and those happen
 * long after.
 */
@Mixin(TextureUtil.class)
public class TextureUtilMixin_FastUpload {

    @Shadow
    private static IntBuffer dataBuffer;

    @Shadow
    private static void setTextureBlurred(boolean blur) {
        throw new AssertionError();
    }

    @Shadow
    private static void setTextureClamped(boolean clamp) {
        throw new AssertionError();
    }

    @Shadow
    private static void copyToBuffer(int[] pixels, int length) {
        throw new AssertionError();
    }

    /** Reused across uploads. Held at the largest size any upload has needed. */
    private static int[] fpsmaster$staging = new int[0];

    /**
     * When the current upload began, so both paths report into the same counter.
     *
     * <p>Static rather than passed through: uploads happen on the render thread and do not nest.
     * A local would only be visible to the fast path, and then turning the feature off would
     * measure nothing at all — which is the state this whole change exists to end.
     */
    @Unique
    private static long EDGE_UPLOAD_STARTED;

    @Inject(method = "uploadTextureImageSubImpl", at = @At("HEAD"), cancellable = true)
    private static void fpsmaster$fastUpload(BufferedImage image, int x, int y, boolean blur,
                                             boolean clamp, CallbackInfo ci) {
        // Before the enable check, so the vanilla path is timed too and the two can be compared.
        EDGE_UPLOAD_STARTED = System.nanoTime();
        if (!Performance.using || !Performance.fastTextureUpload.getValue()) {
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        // Timed unconditionally rather than under BenchmarkMode: an upload is rare and long, so one
        // nanoTime pair either side is free, and the alternative is a feature that ships on with no
        // number behind it because its work never lands inside a measured window.

        int rows = Math.max(1, 4194304 / width);
        int needed = rows * width;
        if (fpsmaster$staging.length < needed) {
            fpsmaster$staging = new int[needed];
        }
        int[] staging = fpsmaster$staging;
        int[] direct = fpsmaster$directPixels(image);
        boolean hasAlpha = image.getType() == BufferedImage.TYPE_4BYTE_ABGR;
        byte[] bytes = direct == null ? fpsmaster$directBytes(image) : null;
        if (direct == null && bytes == null) {
            fpsmaster$noteFallback(image);
        }

        setTextureBlurred(blur);
        setTextureClamped(clamp);

        for (int offset = 0; offset < width * height; offset += width * rows) {
            int firstRow = offset / width;
            int rowsHere = Math.min(rows, height - firstRow);
            int pixels = width * rowsHere;
            if (direct != null) {
                System.arraycopy(direct, firstRow * width, staging, 0, pixels);
            } else if (bytes != null) {
                fpsmaster$unpackBytes(bytes, firstRow * width, staging, pixels, hasAlpha);
            } else {
                image.getRGB(0, firstRow, width, rowsHere, staging, 0, width);
            }
            copyToBuffer(staging, pixels);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y + firstRow, width, rowsHere,
                    32993, 33639, dataBuffer);
        }

        if (BenchmarkMode.ACTIVE) {
            long elapsed = System.nanoTime() - EDGE_UPLOAD_STARTED;
            BenchCounters.textureUploads++;
            BenchCounters.textureUploadPixels += (long) width * height;
            BenchCounters.textureUploadNanos += elapsed;
            if (direct != null || bytes != null) {
                BenchCounters.textureUploadsDirect++;
                BenchCounters.textureUploadDirectNanos += elapsed;
            }
        }
        ci.cancel();
    }

    /**
     * The image's own bytes, for the two interleaved layouts {@code ImageIO} hands back for PNG.
     *
     * <p>These need rearranging rather than copying, but the rearrangement is a few shifts per
     * pixel against {@code getRGB}'s per-pixel trip through the colour model. Narrow for the same
     * reason as the int path: only the exact two types, one bank, no offset, no per-pixel padding.
     */
    private static byte[] fpsmaster$directBytes(BufferedImage image) {
        int type = image.getType();
        if (type != BufferedImage.TYPE_4BYTE_ABGR && type != BufferedImage.TYPE_3BYTE_BGR) {
            return null;
        }
        if (!(image.getRaster().getDataBuffer() instanceof java.awt.image.DataBufferByte)) {
            return null;
        }
        java.awt.image.DataBufferByte buffer =
                (java.awt.image.DataBufferByte) image.getRaster().getDataBuffer();
        if (buffer.getNumBanks() != 1 || buffer.getOffset() != 0) {
            return null;
        }
        if (!(image.getRaster().getSampleModel() instanceof java.awt.image.ComponentSampleModel)) {
            return null;
        }
        java.awt.image.ComponentSampleModel sampleModel =
                (java.awt.image.ComponentSampleModel) image.getRaster().getSampleModel();
        int channels = type == BufferedImage.TYPE_4BYTE_ABGR ? 4 : 3;
        if (sampleModel.getPixelStride() != channels
                || sampleModel.getScanlineStride() != image.getWidth() * channels) {
            return null;
        }
        if (image.getRaster().getMinX() != 0 || image.getRaster().getMinY() != 0) {
            return null;
        }
        return buffer.getData();
    }

    /** ABGR or BGR bytes into the ARGB ints the upload wants. */
    private static void fpsmaster$unpackBytes(byte[] source, int firstPixel, int[] target,
                                              int pixels, boolean hasAlpha) {
        int channels = hasAlpha ? 4 : 3;
        int at = firstPixel * channels;
        for (int i = 0; i < pixels; i++) {
            if (hasAlpha) {
                target[i] = (source[at] & 0xFF) << 24 | (source[at + 3] & 0xFF) << 16
                        | (source[at + 2] & 0xFF) << 8 | source[at + 1] & 0xFF;
                at += 4;
            } else {
                target[i] = 0xFF000000 | (source[at + 2] & 0xFF) << 16
                        | (source[at + 1] & 0xFF) << 8 | source[at] & 0xFF;
                at += 3;
            }
        }
    }

    /**
     * Times an upload that ran vanilla's way.
     *
     * <p>Only reached when the fast path declined, because cancelling from the head injection
     * returns before the method's own return and this never fires. So the two are exclusive and
     * both land in {@code textureUploadNanos}, which is what makes an A/B of the setting possible.
     */
    @Inject(method = "uploadTextureImageSubImpl", at = @At("RETURN"))
    private static void edge$timeVanillaUpload(BufferedImage image, int x, int y, boolean blur,
                                               boolean clamp, CallbackInfo ci) {
        if (!BenchmarkMode.ACTIVE || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return;
        }
        BenchCounters.textureUploads++;
        BenchCounters.textureUploadPixels += (long) image.getWidth() * image.getHeight();
        BenchCounters.textureUploadNanos += System.nanoTime() - EDGE_UPLOAD_STARTED;
    }

    /**
     * Names each image type that misses the direct path, once per type.
     *
     * <p>Called where the fallback actually happens — after both the int path and the byte path
     * have declined. Placing it inside the int path instead named four types that the byte path
     * goes on to handle, which would have made a case for reimplementing code that already exists.
     *
     * <p>Diagnostic rather than a counter: what is wanted is the set of types, not how often each
     * occurs. Which types they are decides whether the remainder is worth chasing at all — an
     * indexed image can be unpacked through its palette, a {@code TYPE_CUSTOM} one has an arbitrary
     * colour model and no safe shortcut.
     */
    @Unique
    private static final java.util.Set<Integer> EDGE_SEEN_FALLBACKS =
            new java.util.HashSet<Integer>();

    @Unique
    private static void fpsmaster$noteFallback(BufferedImage image) {
        if (!BenchmarkMode.ACTIVE || !EDGE_SEEN_FALLBACKS.add(Integer.valueOf(image.getType()))) {
            return;
        }
        ClientLogger.info("texupload", "no direct path for BufferedImage type " + image.getType()
                + " (" + image.getWidth() + "x" + image.getHeight() + "), falling back to getRGB");
    }

    /**
     * The image's own pixels, when they are already in the layout the upload wants, or null.
     *
     * <p>Deliberately narrow. {@code TYPE_INT_ARGB} fixes the channel order and the packing, and the
     * stride and offset checks are what make a plain {@code arraycopy} equal to what {@code getRGB}
     * would have produced. An image that fails any of them is read the slow way rather than guessed
     * at — a wrong answer here is every texture in the game subtly corrupted.
     */
    private static int[] fpsmaster$directPixels(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            return null;
        }
        if (!(image.getRaster().getDataBuffer() instanceof DataBufferInt)) {
            return null;
        }
        DataBufferInt buffer = (DataBufferInt) image.getRaster().getDataBuffer();
        if (buffer.getNumBanks() != 1 || buffer.getOffset() != 0) {
            return null;
        }
        if (!(image.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel)) {
            return null;
        }
        SinglePixelPackedSampleModel sampleModel =
                (SinglePixelPackedSampleModel) image.getRaster().getSampleModel();
        if (sampleModel.getScanlineStride() != image.getWidth()) {
            return null;
        }
        if (image.getRaster().getMinX() != 0 || image.getRaster().getMinY() != 0) {
            return null;
        }
        return buffer.getData();
    }
}
