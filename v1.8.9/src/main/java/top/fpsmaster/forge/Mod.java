package top.fpsmaster.forge;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.api.MinecraftAPI;
import top.fpsmaster.api.provider.IMinecraftProviderImpl;
import top.fpsmaster.api.provider.ProviderRegistry;
import top.fpsmaster.impl.APIProviderImpl;
import top.fpsmaster.wrapper.MinecraftProvider;

@net.minecraftforge.fml.common.Mod(modid = "fpsmaster", useMetadata = true)
public class Mod {
    @net.minecraftforge.fml.common.Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Initialize new MinecraftAPI
        MinecraftAPI.initialize(new APIProviderImpl());
        
        // register old providers (for backward compatibility)
        ProviderRegistry.setMinecraftProvider(new IMinecraftProviderImpl());
        
        FPSMaster.INSTANCE.initialize();
    }
}
