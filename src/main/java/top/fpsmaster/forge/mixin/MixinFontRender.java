package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.modules.client.GlobalTextFilter;

/**
 * Applies the client's text filter to vanilla's width calculation.
 *
 * <p>This also used to route string rendering through a display-list cache. It was removed: on a
 * recorded lobby it cost 58% of the frame rate, and timing inside it showed the whole cost was
 * glCallList itself — 3440us a frame against 21us for the key, the lookup and the state
 * invalidation together. Display lists were fast when drivers compiled them into GPU command
 * buffers; on a current one running a compatibility profile they are emulated, and replaying
 * hundreds of tiny ones per frame costs far more than drawing the text.
 *
 * <p>Batching the glyphs into one draw was tried next and does not help either. It was correct — a
 * static vanilla screen came out byte-identical — and it did what it claimed, cutting 435 draw calls
 * a frame to 102, but two scenarios measured it at -1.2% and -2.3% frame rate. Timing the loop in
 * place says why: vanilla spends 362us a frame inside renderStringAtPos for 766 characters, about
 * half a microsecond each, and that is the character loop rather than the submission. Nothing that
 * only changes how the quads are submitted can reach it.
 *
 * <p>What does reach it is not drawing the text this way at all. Routing chat through the client's
 * own renderer (BetterChat's BetterFont, off by default) measures 705us to 398us on the HUD section,
 * three separated runs each way.
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRender {

    @Shadow
    public abstract int getCharWidth(char character);

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    public void getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Integer.valueOf(edge$measure(GlobalTextFilter.filter(text))));
    }

    /**
     * Vanilla's width calculation, kept here because the mixin cancels the original.
     *
     * <p>A negative char width marks a formatting code: the following character selects a style and
     * contributes nothing itself, except that bold adds a pixel to every subsequent glyph.
     */
    @Unique
    private int edge$measure(String text) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            int charWidth = this.getCharWidth(c);
            if (charWidth < 0 && i < text.length() - 1) {
                c = text.charAt(++i);
                if (c == 'l' || c == 'L') {
                    bold = true;
                } else if (c == 'r' || c == 'R') {
                    bold = false;
                }
                charWidth = 0;
            }
            width += charWidth;
            if (bold && charWidth > 0) {
                ++width;
            }
        }
        return width;
    }
}
