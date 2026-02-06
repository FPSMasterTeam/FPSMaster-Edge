package top.fpsmaster.features.impl.interfaces;

import net.minecraft.entity.Entity;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;

public class ComboDisplay extends InterfaceModule {

    private Entity target = null;

    public static int combo = 0;
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));

    public ComboDisplay() {
        super("ComboDisplay", Category.Interface);
        addSettings(textColor, backgroundColor, betterFont, fontShadow, rounded, bg, rounded, roundRadius);
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer == null) return;
        net.minecraft.entity.player.EntityPlayer rawPlayer =
            net.minecraft.client.Minecraft.getMinecraft().thePlayer;
        
        if (rawPlayer.hurtTime == 1 || (target != null && rawPlayer.getDistanceToEntity(target) > 7)) {
            combo = 0;
        }
        if (target != null && target.isEntityAlive() && target.hurtResistantTime == 19) {
            combo++;
        }
    }

    @Subscribe
    public void attack(EventAttack e) {
        target = e.target;
    }
}



