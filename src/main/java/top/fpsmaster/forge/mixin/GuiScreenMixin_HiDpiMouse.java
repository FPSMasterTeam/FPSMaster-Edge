package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.utils.system.HiDpi;

/**
 * Retina mouse fix for vanilla screens.
 *
 * <p>{@code handleMouseInput} maps the cursor as {@code Mouse.getEventX() * width / displayWidth}.
 * With the HiDPI backing on, {@code displayWidth} is pixels while LWJGL's mouse stays in window
 * points, which would squeeze every click into the bottom-left quarter. Scaling the event
 * coordinates to pixels restores the ratio; on other platforms {@link HiDpi#scale()} is 1 and this
 * is an identity. The client's own screens ({@code ScaledGuiScreen}) do their own mapping and are
 * fixed separately.
 */
@Mixin(GuiScreen.class)
public abstract class GuiScreenMixin_HiDpiMouse {

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;getEventX()I"))
    private int edge$eventXInPixels() {
        return HiDpi.mouseToPixels(Mouse.getEventX());
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;getEventY()I"))
    private int edge$eventYInPixels() {
        return HiDpi.mouseToPixels(Mouse.getEventY());
    }
}
