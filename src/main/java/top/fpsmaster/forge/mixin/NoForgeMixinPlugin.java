package top.fpsmaster.forge.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Keeps the Forge-only mixins out of the config when there is no Forge on the classpath.
 *
 * <p>{@code MixinSplashScreen} targets {@code net.minecraftforge.fml.client.SplashProgress}, and both
 * {@code MixinGuiIngameForge} and {@code GuiIngameForgeMixin_HudBreakdown} target
 * {@code net.minecraftforge.client.GuiIngameForge}. Those target classes only exist under Forge, so in
 * the Forge-free launch Mixin cannot even resolve them during config prepare. Rather than list them
 * statically (and risk a hard failure resolving a missing target), the config omits them and this plugin
 * re-adds them via {@link #getMixins()} — but only when Forge is actually present. Detection is the
 * {@code fpsmaster.noforge} system property the Forge-free tweaker sets: absent → Forge build → add them;
 * present → Forge-free build → skip them. This leaves the Forge runtime byte-for-byte unchanged.
 */
public class NoForgeMixinPlugin implements IMixinConfigPlugin {

    /** Forge-only mixins (relative to the config package), injected only when Forge is present. */
    private static final List<String> FORGE_ONLY_MIXINS = Arrays.asList(
            "MixinSplashScreen",
            "MixinGuiIngameForge",
            "GuiIngameForgeMixin_HudBreakdown"
    );

    private static boolean forgeFree() {
        return Boolean.getBoolean("fpsmaster.noforge");
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return forgeFree() ? Collections.<String>emptyList() : FORGE_ONLY_MIXINS;
    }

    @Override
    public void preApply(String targetClassName, org.spongepowered.asm.lib.tree.ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.spongepowered.asm.lib.tree.ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
