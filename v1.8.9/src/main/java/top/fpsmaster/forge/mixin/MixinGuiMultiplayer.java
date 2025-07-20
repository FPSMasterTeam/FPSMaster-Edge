package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer {
    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    public void initGui(CallbackInfo ci) {
        // check ViaVersion
        try {
            Class.forName("com.viaversion.viaversion.api.Via");
            Minecraft.getMinecraft().displayGuiScreen(new GuiMultiplayer(new MainMenu()));
        } catch (ClassNotFoundException e) {
            Minecraft.getMinecraft().displayGuiScreen(new top.fpsmaster.ui.mc.GuiMultiplayer());
            ci.cancel();
        }
    }
}
