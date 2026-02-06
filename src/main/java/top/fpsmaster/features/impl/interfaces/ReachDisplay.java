package top.fpsmaster.features.impl.interfaces;

import net.minecraft.entity.Entity;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class ReachDisplay extends InterfaceModule {
    public static double reach = 0.0;
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));
    public static double distance;

    public ReachDisplay() {
        super("ReachDisplay", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, textColor, bg, rounded, roundRadius);
    }

    @Subscribe
    public void onAttack(EventAttack e) {
        Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.theWorld != null) {
            if (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)
                return;
            reach = Double.parseDouble(String.format("%.2f", distance));
        }
    }
}



