package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventMotionBlur;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.features.impl.render.Crosshair;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {
    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    protected void showCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Crosshair.using)
            cir.setReturnValue(false);
    }

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    public void scoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (Scoreboard.using)
            ci.cancel();
    }

    // ---- Forge-free HUD event dispatch ----------------------------------------------------------
    // In the Forge build, GuiIngameForge overrides renderGameOverlay/renderTooltip and MixinGuiIngameForge
    // carries these hooks; the base GuiIngame versions are never invoked, so these injections stay dormant
    // there (no double dispatch). In the Forge-free build vanilla GuiIngame is the active HUD, so this is
    // where EventRender2D / EventMotionBlur / HUD benchmark timing must originate.

    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void fpsmaster$beginHud(float partialTicks, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_HUD);
        }
    }

    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    private void fpsmaster$motionBlur(float partialTicks, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_HUD);
        }
        EventDispatcher.dispatchEvent(new EventMotionBlur());
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void fpsmaster$render2D(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        EventDispatcher.dispatchEvent(new EventRender2D(partialTicks));
    }
}



