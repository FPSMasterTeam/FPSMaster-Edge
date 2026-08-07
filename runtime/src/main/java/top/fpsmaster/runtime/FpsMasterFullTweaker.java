package top.fpsmaster.runtime;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-functionality Forge-free tweaker: boots Sponge Mixin under raw LaunchWrapper with no Forge/FML,
 * then loads the <em>main</em> project's mixin config ({@code mixins.fpsmaster.json}) instead of the tiny
 * POC config. This drives the entire FPSMaster Edge client (all ~110 mixins + business code) onto a plain
 * 1.8.9 client.
 *
 * <p>The Forge-only pieces are handled elsewhere so the constant pool never reaches Forge:
 * <ul>
 *   <li>{@code fpsmaster.noforge=true} is set here (and by the Gradle task) so
 *       {@code NoForgeMixinPlugin} drops the three Forge-target mixins (SplashProgress / GuiIngameForge)
 *       and {@code MixinMinecraft} runs {@code FPSMaster.initialize()} at {@code startGame} RETURN.</li>
 *   <li>The refmap is disabled ({@code mixin.env.disableRefMap}) because we run against MCP-named
 *       bytecode (pre-named jar, or runtime deobf after OptiFine), so mixins apply by their written names.</li>
 * </ul>
 *
 * <p>OptiFine (non-Forge): when {@code fpsmaster.withOptifine=true}, Launch must register
 * {@code optifine.OptiFineTweaker} <em>before</em> this tweaker so OF patches notch classes first;
 * this tweaker then registers {@link top.fpsmaster.runtime.remap.RuntimeDeobfTransformer} and Mixin.
 * Launch args are owned by OF — {@link #getLaunchArguments()} returns empty to avoid duplicates.
 *
 * <p>Like {@link FpsMasterTweaker}, this references only LaunchWrapper and Mixin — no
 * {@code net.minecraftforge} import anywhere.
 */
public class FpsMasterFullTweaker implements ITweaker {

    private final List<String> args = new ArrayList<String>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args.addAll(args);
        ensureArg("--version", profile != null ? profile : "1.8.9");
        if (gameDir != null) ensureArg("--gameDir", gameDir.getAbsolutePath());
        if (assetsDir != null) ensureArg("--assetsDir", assetsDir.getAbsolutePath());
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        // Forge-free markers the main project reads at runtime.
        System.setProperty("fpsmaster.noforge", "true");
        // MCP-named bytecode → mixins apply by their source names; the searge refmap must not be used.
        System.setProperty("mixin.env.disableRefMap", "true");

        classLoader.addClassLoaderExclusion("org.spongepowered.asm.");
        classLoader.addTransformerExclusion("top.fpsmaster.runtime.remap.");
        // Do NOT transformer-exclude optifine.* — OF bytecode still has notch Class refs
        // (e.g. Reflector → adg); RuntimeDeobfTransformer must remap those to named.

        boolean withOf = Boolean.getBoolean("fpsmaster.withOptifine");
        // Prefer fpsmaster.runtime.*; accept legacy fpsmaster.poc.* during migration.
        boolean vanilla = Boolean.getBoolean("fpsmaster.runtime.vanilla")
                || Boolean.getBoolean("fpsmaster.poc.vanilla");

        // Notch client.jar (required when OptiFine is present — OF patches official names):
        // register deobf BEFORE Mixin so mixins still see MCP names. OptiFineTweaker (if any)
        // registers OptiFineClassTransformer first; deobf then remaps the OF-patched notch bytes.
        if (vanilla || withOf) {
            classLoader.registerTransformer("top.fpsmaster.runtime.remap.RuntimeDeobfTransformer");
            System.out.println("[FPSMaster FULL] runtime official→named deobf enabled"
                    + (withOf ? " (after OptiFine)" : ""));
        }

        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.fpsmaster.json");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        System.out.println("[FPSMaster FULL] main mixin config loaded (mixins.fpsmaster.json), Forge-free"
                + (withOf ? ", with OptiFine" : ""));
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        // OptiFineTweaker already forwards --gameDir/--assetsDir/--version; returning ours again
        // duplicates them and breaks Main's arg parser.
        if (Boolean.getBoolean("fpsmaster.withOptifine")) {
            return new String[0];
        }
        return this.args.toArray(new String[0]);
    }

    private void ensureArg(String key, String value) {
        if (!this.args.contains(key)) {
            this.args.add(key);
            this.args.add(value);
        }
    }
}
