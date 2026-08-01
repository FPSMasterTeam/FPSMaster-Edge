package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.client.GlobalTextFilter;

/**
 * Draws everything vanilla draws with the client's own renderer instead.
 *
 * <p>Every string the game puts on screen — chat, the tab list, the scoreboard, item stack counts,
 * nameplates, tooltips, every GUI label — reaches the screen through {@code renderString}, which
 * both {@code drawString} and {@code drawSplitString} funnel into. Replacing that one method
 * replaces all of them, and does it without swapping {@code fontRendererObj} for another instance,
 * which would leave every method vanilla implements privately still drawing its own glyphs.
 *
 * <p>The reason to do it is measured: vanilla spends about half a microsecond per character inside
 * its glyph loop, 362us a frame on a busy lobby, and that is the loop rather than the drawing —
 * batching its draw calls was tried and bought nothing. The client's renderer lays a whole string
 * out in one pass and submits it once.
 *
 * <p>Character widths have to move with it. Vanilla computes chat wrapping, string trimming and
 * tooltip boxes from {@code getCharWidth}, so leaving that answering for the bitmap font while the
 * glyphs come from a TrueType one would wrap lines in the wrong places. Both come from the same
 * source here, which is what lets the replacement be narrower than what it stands in for without
 * anything landing in the wrong place — every layout is measured with the font it is drawn with.
 *
 * <p>Formatting codes are carried over, including the styles: bold is the glyph drawn twice a pixel
 * apart, italic a sheared quad, obfuscated a stand-in redrawn every frame, and the two bars are
 * emitted after the glyphs so the batch never has to stop being textured. Right-to-left reordering
 * is the one thing left to vanilla, which is why a set bidi flag falls through untouched rather
 * than drawing the text in the wrong order.
 */
@Mixin(FontRenderer.class)
public abstract class FontRendererMixin_CustomFont {

    @Shadow
    private boolean bidiFlag;
    @Shadow
    private boolean randomStyle;
    @Shadow
    private boolean boldStyle;
    @Shadow
    private boolean italicStyle;
    @Shadow
    private boolean underlineStyle;
    @Shadow
    private boolean strikethroughStyle;

    @Unique
    private UFontRenderer edge$font;
    @Unique
    private int edge$fontSize;

    /**
     * The renderer to stand in with, or null to leave this call to vanilla.
     *
     * <p>{@link UFontRenderer} extends {@link FontRenderer}, so this mixin lands on it too. Letting
     * it take effect there would have the replacement call itself.
     */
    @Unique
    private UFontRenderer edge$font() {
        if (!Performance.using || !Performance.customHudFont.getValue()) {
            return null;
        }
        if ((Object) this instanceof UFontRenderer || FPSMaster.fontManager == null) {
            return null;
        }
        // The enchanting table's renderer is another FontRenderer with another texture, and the
        // point of it is that the text is unreadable. Standing in for it would spell the words out.
        if ((Object) this == Minecraft.getMinecraft().standardGalacticFontRenderer) {
            return null;
        }
        int size = Performance.customHudFontSize.getValue().intValue();
        if (edge$font == null || edge$fontSize != size) {
            edge$font = FPSMaster.fontManager.getFont(size);
            edge$fontSize = size;
        }
        return edge$font;
    }

    /**
     * Takes the shadowed form whole, so the two passes become one recording and one draw call.
     *
     * <p>Vanilla issues a shadow as a second {@code renderString} at an offset, and the hook below
     * sees the two as unrelated strings — two recordings of geometry that is identical in every
     * number except the colour a formatting code resolves to, two cache entries, two draw calls.
     * Intercepting the method that issues both lets them share all of it.
     *
     * <p>Falls through to the per-pass hook when the merge is off, or when there is no shadow to
     * merge, so the unshadowed path is untouched either way.
     *
     * <p>The return value is vanilla's and is easy to get wrong: {@code renderString} answers with
     * the pen after the string, the shadow pass starts a pixel further right, and {@code drawString}
     * takes the larger of the two — so a shadowed string reports {@code x + 1 + advance}, not
     * {@code x + advance}. Layout that centres or right-aligns reads this number.
     */
    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void edge$drawMergedShadow(String text, float x, float y, int color, boolean dropShadow,
                                       CallbackInfoReturnable<Integer> callback) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.drawStringCalls++;
            if (dropShadow) {
                BenchCounters.drawStringShadowed++;
            }
        }
        if (!dropShadow || !Performance.mergeTextShadow.getValue()) {
            return;
        }
        UFontRenderer font = edge$font();
        if (font == null || text == null || this.bidiFlag) {
            return;
        }
        GlStateManager.enableAlpha();
        // Vanilla clears these here, before either pass. Cancelling the method takes that with it,
        // and a string that falls through to vanilla's renderer later — bidi text, the galactic
        // font — would inherit whatever the last formatted string left set.
        randomStyle = false;
        boldStyle = false;
        italicStyle = false;
        underlineStyle = false;
        strikethroughStyle = false;
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }
        if (!GL11.glIsEnabled(GL11.GL_BLEND)) {
            color |= 0xFF000000;
        }
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.mergedShadowDraws++;
        }
        float advance = font.drawRawWithShadow(GlobalTextFilter.filter(text), x, y, color);
        callback.setReturnValue(Integer.valueOf((int) (x + 1.0f + advance)));
    }

    /**
     * Vanilla resolves the colour after this point, so the same two rules are applied here: a colour
     * with no alpha set means opaque, and a shadow pass is the same colour at a quarter intensity.
     * The shadow itself needs no special handling — vanilla asks for it as a second call at an
     * offset, and this sees both.
     */
    @Inject(method = "renderString", at = @At("HEAD"), cancellable = true)
    private void edge$renderWithClientFont(String text, float x, float y, int color, boolean dropShadow,
                                           CallbackInfoReturnable<Integer> callback) {
        UFontRenderer font = edge$font();
        if (font == null || text == null || this.bidiFlag) {
            return;
        }
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }
        if (dropShadow) {
            color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
        }
        // Vanilla hands its colour to the fixed-function pipeline and draws with whatever blending
        // the caller left set, so an alpha it never intended to be seen is simply ignored - the
        // scoreboard asks for 0x21 and comes out solid because blending is off there, and item
        // counts do the same. This renderer blends unconditionally, to keep its antialiased edges,
        // so the alpha has to be dropped where vanilla's would not have applied.
        if (!GL11.glIsEnabled(GL11.GL_BLEND)) {
            color |= 0xFF000000;
        }
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.clientFontDraws++;
        }
        float advance = font.drawRaw(GlobalTextFilter.filter(text), x, y, color, dropShadow);
        callback.setReturnValue(Integer.valueOf((int) (x + advance)));
    }

    /**
     * A negative width marks the formatting prefix, and callers read the sign to know the next
     * character is a code rather than a glyph, so that answer is kept exactly.
     */
    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    private void edge$charWidth(char character, CallbackInfoReturnable<Integer> callback) {
        UFontRenderer font = edge$font();
        if (font == null) {
            return;
        }
        callback.setReturnValue(Integer.valueOf(
                character == 167 ? -1 : Math.round(font.advanceOf(character))));
    }
}
