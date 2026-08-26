package top.fpsmaster.features.impl.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.Color;

/**
 * Screen-anchored countdown for the primed TNT nearest to detonating.
 *
 * <p>The world-space overlay in {@code utility.TNTTimer} labels every charge where it sits, which is
 * unreadable in a crowded fight and invisible once the charge is behind the player. This reports the
 * single number that decides whether to run, in a fixed place the player can keep in view.
 */
public class TNTTimerHUD extends InterfaceModule {
    public static NumberSetting duration = new NumberSetting("Duration", 4, 1, 10, 0.1);
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));
    public static ColorSetting warningColor = new ColorSetting("WarningColor", new Color(255, 255, 0));
    public static ColorSetting criticalColor = new ColorSetting("CriticalColor", new Color(255, 60, 60));

    public TNTTimerHUD() {
        super("TNTTimerHUD", Category.Interface);
        addSettings(duration, textColor, warningColor, criticalColor);
    }

    /**
     * Seconds until the nearest detonation, or a negative value when nothing is primed. The fuse is
     * offset by {@link #duration} so servers running a non-vanilla fuse length still read correctly.
     */
    public static float secondsRemaining() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return -1f;
        }
        float soonest = Float.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityTNTPrimed)) {
                continue;
            }
            float remaining = (float) (((EntityTNTPrimed) entity).fuse / 20.0
                    + duration.getValue().doubleValue() - 4);
            if (remaining < soonest) {
                soonest = remaining;
            }
        }
        return soonest == Float.MAX_VALUE ? -1f : soonest;
    }

    /** Urgency colour for a remaining time, shared with the HUD component. */
    public static int colorFor(float seconds) {
        if (seconds < 1.0f) {
            return criticalColor.getRGB();
        }
        if (seconds < 2.5f) {
            return warningColor.getRGB();
        }
        return textColor.getRGB();
    }
}
