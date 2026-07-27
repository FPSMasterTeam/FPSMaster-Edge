package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.benchmark.Experiments;

/**
 * Isolates the fixed-function state changes in the sky pass.
 *
 * <p>The pass costs 260us of CPU with every draw removed and 5us of GPU, and the dozen world queries
 * it makes cannot account for that. What is left is the state churn — and this runs the fixed
 * function pipeline through a modern driver's compatibility profile, where toggling texturing, fog,
 * alpha or the shade model can make the driver rebuild the internal program it is emulating that
 * pipeline with. That would explain a cost this shape: large, on the CPU, and invisible to the GPU
 * timer.
 *
 * <p>Skipping the toggles leaves the sky drawn in the wrong state, so this only ever answers what
 * they cost — but if they are the answer, the fix is to stop permuting them rather than to cache
 * anything.
 *
 * <pre>
 *   -Dedge.exp.noSkyStateToggles=true
 * </pre>
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_SkyState {

    private static boolean skip() {
        return Experiments.active(Experiments.NO_SKY_STATE_TOGGLES);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableTexture2D()V"))
    private void skyDisableTexture() {
        if (!skip()) {
            GlStateManager.disableTexture2D();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V"))
    private void skyEnableTexture() {
        if (!skip()) {
            GlStateManager.enableTexture2D();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableFog()V"))
    private void skyEnableFog() {
        if (!skip()) {
            GlStateManager.enableFog();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableFog()V"))
    private void skyDisableFog() {
        if (!skip()) {
            GlStateManager.disableFog();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableAlpha()V"))
    private void skyEnableAlpha() {
        if (!skip()) {
            GlStateManager.enableAlpha();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableAlpha()V"))
    private void skyDisableAlpha() {
        if (!skip()) {
            GlStateManager.disableAlpha();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V"))
    private void skyEnableBlend() {
        if (!skip()) {
            GlStateManager.enableBlend();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableBlend()V"))
    private void skyDisableBlend() {
        if (!skip()) {
            GlStateManager.disableBlend();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;shadeModel(I)V"))
    private void skyShadeModel(int mode) {
        if (!skip()) {
            GlStateManager.shadeModel(mode);
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V"))
    private void skyDepthMask(boolean mask) {
        if (!skip()) {
            GlStateManager.depthMask(mask);
        }
    }
}
