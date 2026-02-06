package top.fpsmaster.utils;

import net.minecraft.client.settings.GameSettings;
import top.fpsmaster.FPSMaster;

import java.lang.reflect.Field;

public class OptifineUtil extends Utility {

    private static Field ofFastRender;

    public static boolean isFastRender() {
        if (!FPSMaster.INSTANCE.hasOptifine) return false;
        try {
            if (ofFastRender == null) {
                Class.forName("Config");
                ofFastRender = GameSettings.class.getDeclaredField("ofFastRender");
            }
            ofFastRender.setAccessible(true);
            return ofFastRender.getBoolean(mc.gameSettings);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
        // Ignore the exceptions
    }
        return false;
    }

    public static void setFastRender(boolean value) {
        if (!FPSMaster.INSTANCE.hasOptifine) return;
        try {
            if (ofFastRender == null) {
                Class.forName("Config");
                ofFastRender = GameSettings.class.getDeclaredField("ofFastRender");
            }
            ofFastRender.setAccessible(true);
            ofFastRender.setBoolean(mc.gameSettings, value);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
        // Ignore the exceptions
    }
    }
}
