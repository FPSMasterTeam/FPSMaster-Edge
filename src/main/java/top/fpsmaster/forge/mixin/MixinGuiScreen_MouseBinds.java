package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.settings.impl.BindKeys;
import top.fpsmaster.forge.mixin.accessor.GuiContainerAccessor;

/**
 * 容器界面里的鼠标中键/侧键：原版只有键盘事件会进 {@code GuiContainer.keyTyped}，背包里按
 * 侧键（在原版控制里绑给热键栏/丢弃）什么都不动。这里把这次按键按进 keyTyped，与键盘一致。
 *
 * <p>挂 handleMouseInput 而不是 runTick 的鼠标循环：界面打开时鼠标事件由
 * {@code GuiScreen.handleInput} 抽干后逐个派到这里，runTick 那条循环收不到。
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreen_MouseBinds {

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    private void edge$mouseKeyTyped(CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        // 按键抬起也带着同一个按钮号，不滤掉的话换位会连做两下、互相抵消。
        if (!Mouse.getEventButtonState() || !(self instanceof GuiContainer)
                || Minecraft.getMinecraft().currentScreen != self) {
            return;
        }
        int button = Mouse.getEventButton();
        if (!BindKeys.isBindableButton(button)) {
            return;
        }
        // 取方块是原版唯一认鼠标按钮的容器键（mouseClicked 里按 getKeyCode() + 100 判），
        // 再喂一次 keyTyped 会双踩同一次取方块。
        Minecraft mc = Minecraft.getMinecraft();
        if (BindKeys.mouseCode(button) == mc.gameSettings.keyBindPickBlock.getKeyCode()) {
            return;
        }
        ((GuiContainerAccessor) self).fpsmaster$keyTyped((char) 0, BindKeys.mouseCode(button));
    }
}
