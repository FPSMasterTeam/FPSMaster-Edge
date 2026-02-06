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
import top.fpsmaster.forge.api.IKeyBinding;

@Mixin(KeyBinding.class)
@Implements(@Interface(iface = IKeyBinding.class, prefix = "fpsmaster$"))
public class MixinKeybinding implements IKeyBinding {

    @Shadow
    private boolean pressed;

    @Shadow private int keyCode;

    @Override
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    public void keyDown(CallbackInfoReturnable<Boolean> cir) {
        if (Sprint.using && keyCode == net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode())
            cir.setReturnValue(Sprint.sprint);
    }
}



