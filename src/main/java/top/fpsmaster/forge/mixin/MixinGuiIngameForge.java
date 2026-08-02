package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventMotionBlur;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.impl.interfaces.CustomTitles;
import top.fpsmaster.features.impl.interfaces.SaturationDisplay;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {
    @Shadow(remap = false)
    public static int right_height;

    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void fpsmasterBeginHud(float partialTicks, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_HUD);
        }
    }

    @Inject(method = "renderGameOverlay",at = @At("RETURN"))
    public void motionblur(float partialTicks, CallbackInfo ci){
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_HUD);
        }
        EventDispatcher.dispatchEvent(new EventMotionBlur());
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void renderTooltipPost(ScaledResolution sr, float partialTicks, CallbackInfo callbackInfo) {
        EventDispatcher.dispatchEvent(new EventRender2D(partialTicks));
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideVanillaFood(int width, int height, CallbackInfo callbackInfo) {
        if (SaturationDisplay.using) {
            // renderFood 末尾的 right_height += 10 会在 cancel 时被跳过，
            // 手动补上以免 renderHealthMount/renderAir 整体下移 10px。
            right_height += 10;
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    public void drawString(float x, float y, float z) {
        GlStateManager.translate(x + CustomTitles.getX(), y + CustomTitles.getY(), z);
    }

    @Redirect(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    public void scale(float x, float y, float z) {
        float scale = CustomTitles.getScale();
        GlStateManager.scale(x * scale, y * scale, z * scale);
    }
}



