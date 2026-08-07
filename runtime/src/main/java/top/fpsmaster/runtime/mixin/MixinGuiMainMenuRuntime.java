package top.fpsmaster.runtime.mixin;

import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spec target: rewrite the yellow splash string on the main menu — visible proof that an
 * MCP-named Mixin applied with no Forge on the classpath.
 */
@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenuRuntime {

    @Shadow
    private String splashText;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void fpsmaster$runtimeSplash(CallbackInfo ci) {
        this.splashText = "FPSMaster Runtime — no Forge";
        System.out.println("[FPSMaster Runtime] GuiMainMenu.initGui — splashText rewritten (Mixin hit)");
    }
}
