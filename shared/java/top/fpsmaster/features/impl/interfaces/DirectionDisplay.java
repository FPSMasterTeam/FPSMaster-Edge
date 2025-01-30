package top.fpsmaster.features.impl.interfaces;

import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.Compass;
import top.fpsmaster.utils.Utility;

public class DirectionDisplay extends Module {

    private Compass compass = new Compass(325f, 325f, 1f, 2, true);

    public DirectionDisplay() {
        super("DirectionDisplay", Category.Interface);
    }

    @Subscribe
    public void onRender(EventRender2D e) {
        ScaledResolution scaledResolution = new ScaledResolution(Utility.mc);
        compass.draw(scaledResolution);
    }
}
