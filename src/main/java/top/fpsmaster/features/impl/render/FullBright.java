package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

public class FullBright extends Module {
    private float oldGamma;
    public FullBright(){
        super("FullBright", Category.RENDER);
    }

    @Override
    public void onEnable(){
        oldGamma = Minecraft.getMinecraft().gameSettings.gammaSetting;
        Minecraft.getMinecraft().gameSettings.gammaSetting = 100f;
        super.onEnable();
    }

    @Subscribe
    public void onUpdate(EventUpdate e){
        Minecraft.getMinecraft().gameSettings.gammaSetting = 100f;
    }

    @Override
    public void onDisable(){
        Minecraft.getMinecraft().gameSettings.gammaSetting = oldGamma;
        super.onDisable();
    }
}



