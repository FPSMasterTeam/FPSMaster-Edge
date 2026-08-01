package top.fpsmaster.forge.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.replay.ReplayPlayer;

/**
 * Makes the replay camera a spectator.
 *
 * <p>Vanilla answers this question by looking the player's own UUID up in the tab list, which needs a
 * server to have put it there. During playback nobody has, and when you watch your own recording the
 * entry under that UUID is the one describing the avatar in whatever mode it was recorded in — so the
 * vanilla answer is either absent or actively wrong.
 *
 * <p>Without this the camera falls: {@code EntityPlayer.onUpdate} derives {@code noClip} from it, and
 * {@code EntityPlayerSP} only grants flight while the controller reports spectator mode.
 *
 * <p>Only while the camera is flying itself. Possession is the other half of the same answer: the
 * view is the recorder's, and the game hides the things worth seeing from a spectator — the held
 * item is not drawn, the hotbar is replaced by the spectator's own, and the crosshair changes. The
 * camera is pinned to the avatar every tick with its motion cleared while possessed, so it has no
 * use for flight or for passing through walls during that time and nothing to fall with.
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin_ReplaySpectator {

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void replaySpectator(CallbackInfoReturnable<Boolean> callback) {
        ReplayPlayer player = ReplayPlayer.instance();
        if (player.isCameraEntity(this)) {
            // Answered either way rather than left to vanilla while possessed. Vanilla decides this
            // from the tab list, which during playback is absent or describes the recorder rather
            // than the camera — and a yes from it hides the held item all over again.
            callback.setReturnValue(Boolean.valueOf(!player.isPossessing()));
        }
    }
}
