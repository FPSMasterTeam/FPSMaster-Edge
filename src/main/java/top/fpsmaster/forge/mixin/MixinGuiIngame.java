package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
}



