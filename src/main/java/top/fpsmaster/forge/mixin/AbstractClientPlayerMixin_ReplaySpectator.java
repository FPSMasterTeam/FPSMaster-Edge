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
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin_ReplaySpectator {

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void replaySpectator(CallbackInfoReturnable<Boolean> callback) {
        if (ReplayPlayer.instance().isCameraEntity(this)) {
            callback.setReturnValue(Boolean.TRUE);
        }
    }
}
