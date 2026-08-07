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
 * Stage-0 POC tweaker: boots Sponge Mixin under raw LaunchWrapper with no Forge/FML on the
 * classpath, wires our POC mixin config, then hands control to the vanilla client entry point.
 *
 * <p>Deliberately references only LaunchWrapper and Mixin. There is no {@code net.minecraftforge}
 * import anywhere in this module — that is the whole point of the proof.
 */
public class FpsMasterTweaker implements ITweaker {

    private final List<String> args = new ArrayList<String>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args.addAll(args);

        // Fill in the arguments the vanilla Main parser expects but LaunchWrapper does not forward.
        ensureArg("--version", profile != null ? profile : "1.8.9");
        if (gameDir != null) ensureArg("--gameDir", gameDir.getAbsolutePath());
        if (assetsDir != null) ensureArg("--assetsDir", assetsDir.getAbsolutePath());
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        // Keep Mixin itself out of LaunchWrapper's transformer loop (same idea as MixinTweaker).
        classLoader.addClassLoaderExclusion("org.spongepowered.asm.");
        classLoader.addTransformerExclusion("top.fpsmaster.runtime.remap.");

        // Real notch client.jar path: register deobf BEFORE Mixin so mixins see MCP names.
        if (Boolean.getBoolean("fpsmaster.runtime.vanilla") || Boolean.getBoolean("fpsmaster.poc.vanilla")) {
            classLoader.registerTransformer("top.fpsmaster.runtime.remap.RuntimeDeobfTransformer");
            System.out.println("[FPSMaster Runtime] Vanilla notch jar mode — runtime official→named deobf enabled");
        }

        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.fpsmaster-runtime.json");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
    }

    @Override
    public String getLaunchTarget() {
        // Vanilla client entry point — no FMLTweaker, no Forge bootstrap.
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return this.args.toArray(new String[0]);
    }

    private void ensureArg(String key, String value) {
        if (!this.args.contains(key)) {
            this.args.add(key);
            this.args.add(value);
        }
    }
}
