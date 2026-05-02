package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.font.FontRendererHook;
import top.fpsmaster.modules.client.GlobalTextFilter;

@Mixin(FontRenderer.class)
public abstract class MixinFontRender {

    @Shadow
    protected abstract void resetStyles();

    @Shadow
    protected abstract int renderString(String text, float x, float y, int color, boolean dropShadow);

    @Shadow
    public abstract int getCharWidth(char character);

    @Unique
    private final FontRendererHook patcher$fontRendererHook = new FontRendererHook((FontRenderer) (Object) this);

    @Unique
    private FontRenderer fpsmaster$self() {
        return (FontRenderer) (Object) this;
    }

    @Unique
    private int fpsmaster$getVanillaStringWidth(String text) {
        if (text == null) {
            return 0;
        }

        int width = 0;
        boolean bold = false;

        for (int i = 0; i < text.length(); ++i) {
            char character = text.charAt(i);
            int charWidth = this.getCharWidth(character);

            if (charWidth < 0 && i < text.length() - 1) {
                ++i;
                character = text.charAt(i);

                if (character == 'l' || character == 'L') {
                    bold = true;
                } else if (character == 'r' || character == 'R') {
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

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    public void getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        text = GlobalTextFilter.filter(text);

        if (text == null || text.isEmpty()) {
            cir.setReturnValue(0);
            return;
        }

        if (Performance.shouldUseFontOptimize(fpsmaster$self(), text)) {
            cir.setReturnValue(this.patcher$fontRendererHook.getStringWidth(text));
            return;
        }

        cir.setReturnValue(fpsmaster$getVanillaStringWidth(text));
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"), cancellable = true)
    private void patcher$useOptimizedRendering(String text, boolean shadow, CallbackInfo ci) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (!Performance.shouldUseFontOptimize(fpsmaster$self(), text)) {
            return;
        }

        if (this.patcher$fontRendererHook.renderStringAtPos(text, shadow)) {
            ci.cancel();
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void patcher$markFontRefresh(CallbackInfo ci) {
        FontRendererHook.forceRefresh = true;
    }

    /**
     * @author SuperSkidder
     * @reason NameProtect
     */
    @Overwrite
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        text = GlobalTextFilter.filter(text);

        if (text == null) {
            return 0;
        }

        GlStateManager.enableAlpha();
        this.resetStyles();

        int width;

        if (dropShadow) {
            width = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
            width = Math.max(width, this.renderString(text, x, y, color, false));
        } else {
            width = this.renderString(text, x, y, color, false);
        }

        return width;
    }
}
