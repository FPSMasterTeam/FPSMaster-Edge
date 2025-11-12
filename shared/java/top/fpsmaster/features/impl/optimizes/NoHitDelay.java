package top.fpsmaster.features.impl.optimizes;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.api.Wrappers;

public class NoHitDelay extends Module {

    private static boolean using = false;

    public NoHitDelay() {
        super("NoHitDelay", Category.OPTIMIZE);
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
    public void onTick(EventTick e) {
        Wrappers.minecraft().removeClickDelay();
    }

    public static boolean isUsing() {
        return using;
    }

    public static void setUsing(boolean using) {
        NoHitDelay.using = using;
    }
}
