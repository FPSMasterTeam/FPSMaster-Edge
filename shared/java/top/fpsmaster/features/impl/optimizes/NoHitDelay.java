package top.fpsmaster.features.impl.optimizes;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

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
        resetClickDelay();
    }

    private void resetClickDelay() {
        try {
            Field field = Minecraft.class.getDeclaredField("leftClickCounter");
            field.setAccessible(true);
            field.setInt(Minecraft.getMinecraft(), 0);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    public static boolean isUsing() {
        return using;
    }

    public static void setUsing(boolean using) {
        NoHitDelay.using = using;
    }
}
