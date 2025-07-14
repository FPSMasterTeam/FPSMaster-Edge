package top.fpsmaster.features.impl.render;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.wrapper.mods.WrapperHitboxes;

import java.awt.*;

public class Hitboxes extends Module {
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255, 255));
    public static boolean using = false;
    public Hitboxes(){
        super("HitBoxes", Category.RENDER);
        addSettings(color);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Subscribe
    public void onRender(EventRender3D event) {
        WrapperHitboxes.render(event, color);
    }
}
