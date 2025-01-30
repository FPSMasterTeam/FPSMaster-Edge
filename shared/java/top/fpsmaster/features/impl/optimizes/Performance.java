package top.fpsmaster.features.impl.optimizes;

import net.minecraft.world.World;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.wrapper.mods.WrapperPerformance;

public class Performance extends Module {

    public static boolean using = false;

    public static BooleanSetting ignoreStands = new BooleanSetting("IgnoreStands", true);
    public static BooleanSetting entitiesOptimize = new BooleanSetting("EntitiesOptimize", true);
    public static BooleanSetting fastLoad = new BooleanSetting("FastLoad", true);
    public static NumberSetting fpsLimit = new NumberSetting("FPSLimit", 30, 0, 360, 1);
    public static NumberSetting entityLimit = new NumberSetting("EntityLimit", 200, 0, 800, 1);
    public static NumberSetting particlesLimit = new NumberSetting("ParticlesLimit", 100, 0, 2000, 1);
    public static BooleanSetting blur = new BooleanSetting("Blur", false);

    public Performance() {
        super("Performance", Category.OPTIMIZE);
        addSettings(ignoreStands, entitiesOptimize, fastLoad, entityLimit, fpsLimit, particlesLimit, blur);
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

    public static boolean isUsing() {
        return using;
    }

    public static void setUsing(boolean using) {
        Performance.using = using;
    }

    public static boolean isVisible(CheckEntity entity) {
        return WrapperPerformance.isVisible(entity);
    }

    public static boolean isVisible(
            World world, 
            double minX, double minY, double minZ, 
            double maxX, double maxY, double maxZ, 
            double cameraX, double cameraY, double cameraZ) {
        return WrapperPerformance.isVisible(world, minX, minY, minZ, maxX, maxY, maxZ, cameraX, cameraY, cameraZ);
    }
}
