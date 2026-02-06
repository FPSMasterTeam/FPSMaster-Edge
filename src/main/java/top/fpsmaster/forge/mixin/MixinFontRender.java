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


    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    public void getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        text = GlobalTextFilter.filter(text);
        if (Performance.fontOptimize.getValue()) {
            cir.setReturnValue(this.patcher$fontRendererHook.getStringWidth(text));
        } else {
            int i = 0;
            boolean flag = false;

            for (int j = 0; j < text.length(); ++j) {
                char c0 = text.charAt(j);
                int k = this.getCharWidth(c0);
                if (k < 0 && j < text.length() - 1) {
                    ++j;
                    c0 = text.charAt(j);
                    if (c0 != 'l' && c0 != 'L') {
                        if (c0 == 'r' || c0 == 'R') {
                            flag = false;
                        }
                    } else {
                        flag = true;
                    }

                    k = 0;
                }

                i += k;
                if (flag && k > 0) {
                    ++i;
                }
            }

            cir.setReturnValue(i);
        }
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"), cancellable = true)
    private void patcher$useOptimizedRendering(String text, boolean shadow, CallbackInfo ci) {
        if (Performance.fontOptimize.getValue()) {
            if (this.patcher$fontRendererHook.renderStringAtPos(text, shadow)) {
                ci.cancel();
            }
        }
    }

    /**
     * @author SuperSkidder
     * @reason NameProtect
     */
    @Overwrite
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        text = GlobalTextFilter.filter(text);
        GlStateManager.enableAlpha();
        this.resetStyles();
        int i;
        if (dropShadow) {
            i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
            i = Math.max(i, this.renderString(text, x, y, color, false));
        } else {
            i = this.renderString(text, x, y, color, false);
        }

        return i;
    }
}



