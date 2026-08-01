package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.utils.render.VanillaFontBatch;

/**
 * Draws vanilla's font one string at a time instead of one character at a time.
 *
 * <p>{@code renderDefaultChar} binds a texture, opens a {@code glBegin}, writes four vertices and
 * closes it, for every character. This emits the identical four vertices into a single primitive
 * opened once and closed at the end of the string.
 *
 * <p><b>Nothing about the appearance changes.</b> That is the whole reason for it. The client has a
 * replacement font renderer that is faster still, and it is off by default because the face is not
 * Minecraft's — the largest frame rate setting this client has is one people decline on looks. This
 * takes most of the same cost off the text they wanted in the first place.
 *
 * <p>Three things force the primitive closed, all of them illegal between {@code glBegin} and
 * {@code glEnd}: binding a different page, vanilla's underline issuing its own GL through the
 * tessellator, and the end of the string, after which the colour is no longer the one these
 * vertices were meant to be drawn in. The last is the one that matters — the others would be a
 * GL error, that one would silently draw the wrong colour.
 */
@Mixin(FontRenderer.class)
public abstract class FontRendererMixin_BatchVanilla {

    @Shadow
    protected float posX;
    @Shadow
    protected float posY;
    @Shadow
    private int[] charWidth;
    @Shadow
    private byte[] glyphWidth;
    @Shadow
    private ResourceLocation locationFontTexture;
    @Shadow
    private boolean strikethroughStyle;
    @Shadow
    private boolean underlineStyle;

    /** Forge-added, so it carries no obfuscation mapping and must not be looked up through one. */
    @Shadow(remap = false)
    protected abstract void bindTexture(ResourceLocation location);

    /**
     * Vanilla's unicode page locations, rebuilt here rather than shadowed.
     *
     * <p>{@code loadGlyphTexture} and the array behind it have no obfuscation mapping in this
     * environment, so a {@code @Shadow} of either resolves to a literal name that may not exist at
     * runtime — a silently dropped mixin, which this project has already lost an afternoon to once.
     * The format string is vanilla's and the cache means the allocation happens once per page.
     */
    @Unique
    private static final ResourceLocation[] EDGE_UNICODE_PAGES = new ResourceLocation[256];

    private static boolean edge$enabled() {
        return Performance.using && Performance.batchVanillaFont.getValue();
    }

    /**
     * The ASCII page glyph, written into the batch instead of its own {@code glBegin}.
     *
     * <p>Every constant here is vanilla's. The 7.99 and the 0.01 are its bleed guards and moving
     * either by a hundredth changes which texels a glyph samples at the edges.
     */
    @Inject(method = "renderDefaultChar", at = @At("HEAD"), cancellable = true)
    private void edge$batchDefaultChar(int ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (!edge$enabled()) {
            return;
        }
        int column = ch % 16 * 8;
        int row = ch / 16 * 8;
        int shear = italic ? 1 : 0;
        if (VanillaFontBatch.use(locationFontTexture)) {
            bindTexture(locationFontTexture);
        }
        int advance = charWidth[ch];
        float width = advance - 0.01f;
        VanillaFontBatch.glyph(
                posX + shear, posY, column / 128.0f, row / 128.0f,
                posX - shear, posY + 7.99f, column / 128.0f, (row + 7.99f) / 128.0f,
                posX + width - 1.0f + shear, posY, (column + width - 1.0f) / 128.0f, row / 128.0f,
                posX + width - 1.0f - shear, posY + 7.99f,
                (column + width - 1.0f) / 128.0f, (row + 7.99f) / 128.0f);
        cir.setReturnValue(Float.valueOf(advance));
    }

    @Inject(method = "renderUnicodeChar", at = @At("HEAD"), cancellable = true)
    private void edge$batchUnicodeChar(char ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (!edge$enabled()) {
            return;
        }
        if (glyphWidth[ch] == 0) {
            cir.setReturnValue(Float.valueOf(0.0f));
            return;
        }
        int page = ch / 256;
        ResourceLocation location = EDGE_UNICODE_PAGES[page];
        if (location == null) {
            location = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", page));
            EDGE_UNICODE_PAGES[page] = location;
        }
        if (VanillaFontBatch.use(location)) {
            bindTexture(location);
        }
        int left = glyphWidth[ch] >>> 4;
        int right = (glyphWidth[ch] & 15) + 1;
        float u = ch % 16 * 16 + left;
        float v = (ch & 255) / 16 * 16;
        float span = right - left - 0.02f;
        float shear = italic ? 1.0f : 0.0f;
        VanillaFontBatch.glyph(
                posX + shear, posY, u / 256.0f, v / 256.0f,
                posX - shear, posY + 7.99f, u / 256.0f, (v + 15.98f) / 256.0f,
                posX + span / 2.0f + shear, posY, (u + span) / 256.0f, v / 256.0f,
                posX + span / 2.0f - shear, posY + 7.99f,
                (u + span) / 256.0f, (v + 15.98f) / 256.0f);
        cir.setReturnValue(Float.valueOf((right - left) / 2.0f + 1.0f));
    }

    /** A colour change applies to every vertex still in the primitive, so it has to be closed first. */
    @Inject(method = "setColor", at = @At("HEAD"), remap = false)
    private void edge$flushBeforeColor(float red, float green, float blue, float alpha,
                                       CallbackInfo ci) {
        VanillaFontBatch.flush();
    }

    /**
     * Underline and strikethrough draw through the same {@code Tessellator} this batch is holding.
     *
     * <p>{@code doDraw} runs after every character, but only opens the tessellator when one of the
     * two styles is on, so the flush is conditional on the same thing. Unconditional would mean one
     * draw per character — exactly what this exists to avoid.
     */
    @Inject(method = "doDraw", at = @At("HEAD"), remap = false)
    private void edge$flushBeforeDecoration(float advance, CallbackInfo ci) {
        if (strikethroughStyle || underlineStyle) {
            VanillaFontBatch.flush();
        }
    }

    @Inject(method = "renderStringAtPos", at = @At("RETURN"))
    private void edge$flushAtEndOfString(String text, boolean shadow, CallbackInfo ci) {
        VanillaFontBatch.flush();
    }
}
