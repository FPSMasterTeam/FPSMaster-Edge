package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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

    @Inject(method = "uploadTextureImageSubImpl", at = @At("HEAD"), cancellable = true)
    private static void fpsmaster$fastUpload(BufferedImage image, int x, int y, boolean blur,
                                             boolean clamp, CallbackInfo ci) {
        if (!Performance.using || !Performance.fastTextureUpload.getValue()) {
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int rows = Math.max(1, 4194304 / width);
        int needed = rows * width;
        if (fpsmaster$staging.length < needed) {
            fpsmaster$staging = new int[needed];
        }
        int[] staging = fpsmaster$staging;
        int[] direct = fpsmaster$directPixels(image);
        boolean hasAlpha = image.getType() == BufferedImage.TYPE_4BYTE_ABGR;
        byte[] bytes = direct == null ? fpsmaster$directBytes(image) : null;

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
            BenchCounters.textureUploads++;
            BenchCounters.textureUploadPixels += (long) width * height;
            if (direct != null || bytes != null) {
                BenchCounters.textureUploadsDirect++;
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
