package top.fpsmaster.forge;

import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import top.fpsmaster.FPSMaster;

@net.minecraftforge.fml.common.Mod(modid = "fpsmaster", useMetadata = true)
public class Mod {
    /**
     * Forge reads this flag in its own preInit and, when false, contacts files.minecraftforge.net.
     * The client is meant to make no update requests at all, and construction is the last phase
     * that runs before Forge's preInit.
     */
    @net.minecraftforge.fml.common.Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        ForgeModContainer.disableVersionCheck = true;
    }

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FPSMaster.INSTANCE.initialize();
    }

}
