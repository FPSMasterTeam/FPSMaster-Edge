package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.utils.system.HiDpi;

/**
 * Retina hover fix. Click events go through {@code GuiScreen.handleMouseInput} (patched in
 * {@link GuiScreenMixin_HiDpiMouse}), but the per-frame hover position handed to
 * {@code drawScreen} is computed here — {@code Mouse.getX() * scaledWidth / displayWidth} — and
 * mixes point-based mouse values with a pixel-based displayWidth. Scale the cursor to pixels so
 * hover highlights land where the cursor actually is; identity off Retina.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin_HiDpiMouse {

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;getX()I"))
    private int edge$hoverXInPixels() {
        return HiDpi.mouseToPixels(Mouse.getX());
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;getY()I"))
    private int edge$hoverYInPixels() {
        return HiDpi.mouseToPixels(Mouse.getY());
    }
}
