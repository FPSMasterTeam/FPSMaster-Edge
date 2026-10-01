package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.features.settings.impl.BindKeys;
import top.fpsmaster.forge.mixin.accessor.GuiContainerAccessor;
import top.fpsmaster.replay.ReplayDirectorIsolation;

/**
 * 鼠标侧键/中键的按键语义。
 *
 * <p>世界里的原版按键原版已经照顾到了：runTick 会把鼠标码（{@code -100 + 按钮}）喂进
 * {@code KeyBinding.setKeyBindState / onTick}，所以绑在侧键上的热键栏在世界里能切。死的是
 * 容器界面那一路——{@code GuiContainer.keyTyped} 只接键盘事件，背包里按侧键什么都不动，
 * 数字键却照常换位。
 *
 * <p>这里补两条：界面外派 {@link EventKey} 给客户端模块（和键盘路径同一份约定），
 * 容器里则把这次按键按进容器的 {@code keyTyped}，热键栏换位/丢弃/选取方块与键盘完全一致。
 */
@Mixin(Minecraft.class)
public class MinecraftMixin_MouseBinds {

    @Shadow
    public GuiScreen currentScreen;

    /**
     * 挂在 runTick 里 {@code Mouse.getEventButton()} 之前——那正是原版逐个处理鼠标事件的位置，
     * 当前事件（按钮号/按下与否）已经就绪，跟着原版的取法读同一份值就行。
     */
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I"))
    private void edge$mouseBinds(CallbackInfo ci) {
        int button = Mouse.getEventButton();
        // 左/右键是攻击、使用与界面点击，不能当绑定——捕获端也只放行可绑定按钮。
        if (!Mouse.getEventButtonState() || !BindKeys.isBindableButton(button)) {
            return;
        }
        int key = BindKeys.mouseCode(button);
        if (currentScreen == null) {
            if (!ReplayDirectorIsolation.isDirectorView()) {
                EventDispatcher.dispatchEvent(new EventKey(key));
            }
        } else if (currentScreen instanceof GuiContainer) {
            ((GuiContainerAccessor) currentScreen).fpsmaster$keyTyped((char) 0, key);
        }
    }
}
