package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@code GuiContainer.keyTyped}, which is protected.
 *
 * <p>原版只把键盘事件送进 keyTyped；鼠标绑定的按键要在背包里换位/丢弃/选取方块，就得自己按一次。
 */
@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {

    @Invoker("keyTyped")
    void fpsmaster$keyTyped(char typedChar, int keyCode);
}
