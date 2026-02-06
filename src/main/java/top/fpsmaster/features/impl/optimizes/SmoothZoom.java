package top.fpsmaster.features.impl.optimizes;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.utils.core.Utility;

public class SmoothZoom extends Module {

    private final BindSetting zoomBind = new BindSetting("ZoomBind", Keyboard.KEY_C);
    private final BooleanSetting smoothMouse = new BooleanSetting("SmoothMouse", false);

    public SmoothZoom() {
        super("SmoothZoom", Category.OPTIMIZE);
        addSettings(smoothCamera, speed, zoomBind, smoothMouse);
        set(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (Utility.mc.currentScreen != null) return;

        if (Keyboard.isKeyDown(zoomBind.getValue())) {
            zoom = true;
            if (smoothMouse.getValue()) {
                Utility.mc.gameSettings.smoothCamera = true;
            }
        } else {
            zoom = false;
            if (smoothMouse.getValue()) {
                Utility.mc.gameSettings.smoothCamera = false;
            }
        }
    }

    public static boolean using = false;

    public static boolean zoom = false;

    public static BooleanSetting smoothCamera = new BooleanSetting("smoothZoom", false);

    public static NumberSetting speed = new NumberSetting("Speed", 4.0, 0.1, 10.0, 0.1, () -> smoothCamera.getValue());
}



