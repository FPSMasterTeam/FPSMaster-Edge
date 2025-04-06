package top.fpsmaster.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.ModMetadata;
import top.fpsmaster.FPSMaster;

@Mod(
    modid = "fpsmaster",
    name = "FPSMaster", 
    version = "v4",
    useMetadata = true
)
public class Mod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModMetadata meta = event.getModMetadata();
        meta.logoFile = "fpsmaster:textures/icon.png"; 
        
        System.out.println("[FPSMaster] 图标路径: " + meta.logoFile);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ForgeEventAPI());
        FPSMaster.INSTANCE.initialize();
    }
}
