package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.features.settings.impl.BindKeys;
import top.fpsmaster.replay.ReplayDirectorIsolation;

/**
 * 世界里的鼠标中键/侧键：补一个 {@link EventKey}（码 = {@code -100 + 按钮}，见 {@link BindKeys}），
 * 和键盘路径共用一条总线。
 *
 * <p>界面里的按键不走这里——runTick 的鼠标循环在界面打开时已被 handleInput 抽干，改由
 * {@link MixinGuiScreen_MouseBinds} 挂在 handleMouseInput 上。
 */
@Mixin(Minecraft.class)
public class MinecraftMixin_MouseBinds {

    @Shadow
    public GuiScreen currentScreen;

    /**
     * 挂在 runTick 处理鼠标事件的位置：此处的当前事件（按钮号/按下与否）已经就绪。
     */
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I"))
    private void edge$mouseBinds(CallbackInfo ci) {
        if (currentScreen != null || !Mouse.getEventButtonState()) {
            return;
        }
        int button = Mouse.getEventButton();
        if (!BindKeys.isBindableButton(button) || ReplayDirectorIsolation.isDirectorView()) {
            return;
        }
        EventDispatcher.dispatchEvent(new EventKey(BindKeys.mouseCode(button)));
    }
}
