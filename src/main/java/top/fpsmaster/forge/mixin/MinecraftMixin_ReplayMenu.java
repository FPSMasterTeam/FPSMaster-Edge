package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.ui.screens.replay.ReplayControlScreen;

/**
 * Sends Escape to the replay controls instead of the pause menu while a recording is playing.
 *
 * <p>The vanilla menu has nothing to offer here — Back to Game, Options, and a Disconnect that tears
 * the playback down sideways — while the one thing a viewer reaches for, the scrubber, needs a
 * cursor and so needs a screen. Escape is where a player already looks for that.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin_ReplayMenu {

    @Inject(method = "displayInGameMenu", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$replayControls(CallbackInfo ci) {
        if (ReplayPlayer.instance().isActive()) {
            Minecraft.getMinecraft().displayGuiScreen(new ReplayControlScreen());
            ci.cancel();
        }
    }
}
