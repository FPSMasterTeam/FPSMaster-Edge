package top.fpsmaster.forge.mixin;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.replay.ReplayPlayer;

/**
 * Shows the recorder's interface while their avatar is possessed, and only then.
 *
 * <p>Vanilla gates the whole player overlay — hotbar, health, armour, experience — on the game mode
 * being survival or adventure, which is why a spectator sees none of it. That is the right answer
 * while flying around watching someone: their health is not yours. Looking through their eyes is the
 * moment it becomes worth seeing, and the viewer's game mode cannot be changed to arrange that
 * without also taking away the camera's flight.
 */
@Mixin(PlayerControllerMP.class)
public class PlayerControllerMPMixin_ReplayHud {

    @Inject(method = "shouldDrawHUD", at = @At("HEAD"), cancellable = true)
    private void replayPossessedHud(CallbackInfoReturnable<Boolean> callback) {
        if (ReplayPlayer.instance().isPossessing()) {
            callback.setReturnValue(Boolean.TRUE);
        }
    }

    /**
     * Drawing the overlay is not the same question as which overlay to draw.
     *
     * <p>The controller is asked separately, and a yes here is what puts the spectator's own hotbar
     * on screen instead of the recorder's, changes the crosshair and drops the held-item tooltip.
     * Answering both questions the same way while possessed is what makes the interface the
     * recorder's rather than a mixture of theirs and a spectator's.
     */
    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void replayPossessedController(CallbackInfoReturnable<Boolean> callback) {
        if (ReplayPlayer.instance().isPossessing()) {
            callback.setReturnValue(Boolean.FALSE);
        }
    }
}
