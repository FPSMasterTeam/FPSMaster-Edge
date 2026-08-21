package top.fpsmaster.forge.mixin;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.features.impl.utility.Sprint;
import top.fpsmaster.features.impl.utility.ToggleSneak;
import top.fpsmaster.forge.api.IKeyBinding;
import top.fpsmaster.replay.ReplayDirectorIsolation;

@Mixin(KeyBinding.class)
@Implements(@Interface(iface = IKeyBinding.class, prefix = "fpsmaster$"))
public class MixinKeybinding implements IKeyBinding {

    @Shadow
    private boolean pressed;

    @Shadow private int keyCode;

    @Shadow
    private int pressTime;

    @Override
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    public void edge$suppressDirectorIsPressed(CallbackInfoReturnable<Boolean> cir) {
        if (ReplayDirectorIsolation.blocksVanillaBind((KeyBinding) (Object) this)) {
            this.pressTime = 0;
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    public void keyDown(CallbackInfoReturnable<Boolean> cir) {
        if (ReplayDirectorIsolation.blocksVanillaBind((KeyBinding) (Object) this)) {
            cir.setReturnValue(false);
            return;
        }
        if (Sprint.using && keyCode == net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode())
            cir.setReturnValue(Sprint.sprint);
        if (ToggleSneak.using && keyCode == net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode())
            cir.setReturnValue(ToggleSneak.sneak);
    }
}



