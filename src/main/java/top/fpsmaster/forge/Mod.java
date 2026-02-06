package top.fpsmaster.forge;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import top.fpsmaster.FPSMaster;

@net.minecraftforge.fml.common.Mod(modid = "fpsmaster", useMetadata = true)
public class Mod {
    @net.minecraftforge.fml.common.Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FPSMaster.INSTANCE.initialize();
    }
}



