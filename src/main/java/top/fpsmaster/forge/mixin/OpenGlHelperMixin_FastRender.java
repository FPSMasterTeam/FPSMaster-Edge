package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.utils.render.FastRender;

/**
 * Makes the game render straight to the back buffer.
 *
 * <p>This one method is the whole switch. Everything that would otherwise use the framebuffer asks
 * here first — {@code Framebuffer} guards its own create, bind, unbind and delete on it, and the
 * game loop's blit to the screen goes through those — so answering no here is enough to remove the
 * pass, without touching any of the call sites or the player's saved options.
 */
@Mixin(OpenGlHelper.class)
public class OpenGlHelperMixin_FastRender {

    @Inject(method = "isFramebufferEnabled", at = @At("HEAD"), cancellable = true)
    private static void edge$bypassFramebuffer(CallbackInfoReturnable<Boolean> callback) {
        if (FastRender.isActive()) {
            callback.setReturnValue(Boolean.FALSE);
        }
    }
}
