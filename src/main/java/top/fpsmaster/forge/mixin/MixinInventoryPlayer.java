package top.fpsmaster.forge.mixin;

import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.SmoothZoom;
import top.fpsmaster.replay.ReplayDirectorIsolation;

@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer {
    @Inject(method = "changeCurrentItem", at = @At("HEAD"), cancellable = true)
    private void onchangeCurrentItem(int direction, CallbackInfo ci) {
        if (ReplayDirectorIsolation.isDirectorView()) {
            ci.cancel();
            return;
        }
        if (SmoothZoom.wheelZoom.getValue() && SmoothZoom.isZoomKeyActive()) {
            ci.cancel();
        }
    }
}
