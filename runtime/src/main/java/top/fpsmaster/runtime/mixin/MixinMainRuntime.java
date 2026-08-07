package top.fpsmaster.runtime.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Early proof: fires as soon as LaunchWrapper loads MCP-named {@code Main}, before any UI.
 * Keep this even after the main-menu mixin works — it is the first signal in headless/log-only runs.
 */
@Mixin(Main.class)
public class MixinMainRuntime {

    @Inject(method = "main", at = @At("HEAD"))
    private static void fpsmaster$pocProof(String[] args, CallbackInfo ci) {
        System.out.println("========================================================");
        System.out.println("[FPSMaster Runtime] MCP-named Mixin applied to net.minecraft.client.main.Main");
        System.out.println("[FPSMaster Runtime] No Forge on classpath — LaunchWrapper + Tweaker + Mixin only.");
        System.out.println("========================================================");
    }
}
