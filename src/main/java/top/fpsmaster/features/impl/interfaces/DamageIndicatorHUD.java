package top.fpsmaster.features.impl.interfaces;

import net.minecraft.entity.EntityLivingBase;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.Color;

/**
 * Reports the damage of the player's most recent hit at a fixed screen position.
 *
 * <p>Tracking is kept here rather than read out of {@code render.DamageIndicator} so that the HUD
 * works on its own: the floating world numbers are a separate module a player may leave off, and
 * borrowing its state would make this silently blank whenever it was.
 */
public class DamageIndicatorHUD extends InterfaceModule {
    public static NumberSetting holdSeconds = new NumberSetting("HoldTime", 3, 1, 10, 0.5);
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 85, 85));

    private static float lastDamage;
    private static long lastDamageAt;

    private EntityLivingBase target;
    private float health;

    public DamageIndicatorHUD() {
        super("DamageIndicatorHUD", Category.Interface);
        addSettings(holdSeconds, textColor);
    }

    /** Damage of the latest hit, or a negative value once it has aged out or none has landed. */
    public static float recentDamage() {
        if (lastDamageAt == 0L) {
            return -1f;
        }
        long holdMillis = (long) (holdSeconds.getValue().doubleValue() * 1000d);
        return System.currentTimeMillis() - lastDamageAt > holdMillis ? -1f : lastDamage;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        lastDamage = 0f;
        lastDamageAt = 0L;
    }

    @Subscribe
    public void onAttack(EventAttack e) {
        if (e.target instanceof EntityLivingBase) {
            target = (EntityLivingBase) e.target;
            health = target.getHealth();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (target == null) {
            return;
        }
        float dealt = health - target.getHealth();
        if (dealt > 0f) {
            lastDamage = dealt;
            lastDamageAt = System.currentTimeMillis();
        }
        if (dealt != 0f) {
            health = target.getHealth();
        }
    }
}
