package top.fpsmaster.forge;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.api.provider.IMinecraftProviderImpl;
import top.fpsmaster.api.provider.ProviderRegistry;
import top.fpsmaster.wrapper.MinecraftProvider;

@net.minecraftforge.fml.common.Mod(modid = "fpsmaster", useMetadata = true)
public class Mod {
    @net.minecraftforge.fml.common.Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // register providers
        ProviderRegistry.setMinecraftProvider(new IMinecraftProviderImpl());
        FPSMaster.INSTANCE.initialize();
    }
}
