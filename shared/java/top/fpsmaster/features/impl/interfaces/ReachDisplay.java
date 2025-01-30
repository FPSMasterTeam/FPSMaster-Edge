package top.fpsmaster.features.impl.interfaces;

import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.wrapper.util.WrapperAxisAlignedBB;
import top.fpsmaster.wrapper.util.WrapperVec3;

import java.awt.Color;
import java.util.List;

import static top.fpsmaster.utils.Utility.mc;

public class ReachDisplay extends InterfaceModule {
    public static double reach = 0.0;
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));

    public ReachDisplay() {
        super("ReachDisplay", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, textColor, bg, rounded, roundRadius);
    }

    @Subscribe
    public void onAttack(EventAttack e) {
        Entity entity = mc.getRenderViewEntity();
        if (entity != null && ProviderManager.mcProvider.getWorld() != null) {
            double d0 = mc.playerController.getBlockReachDistance();
            MovingObjectPosition rayTrace = entity.rayTrace(d0, ProviderManager.timerProvider.getRenderPartialTicks());
            Vec3 vec3d = entity.getPositionEyes(ProviderManager.timerProvider.getRenderPartialTicks());
            double d1 = d0;
            if (mc.playerController.extendedReach()) {
                d1 = 6.0;
                d0 = d1;
            }
            if (rayTrace != null) {
                d1 = rayTrace.hitVec.distanceTo(vec3d);
            }
            WrapperVec3 vec3d1 = new WrapperVec3(entity.getLook(1.0f));
            Vec3 vec3d2 = new WrapperVec3(vec3d).addVector(vec3d1.x() * d0, vec3d1.y() * d0, vec3d1.z() * d0);
            Vec3 vec3d3 = null;
            if (ProviderManager.mcProvider.getWorld() == null)
                return;
            List<Entity> list = ProviderManager.mcProvider.getWorld().getEntitiesInAABBexcluding(
                    entity,
                    new WrapperAxisAlignedBB(entity.getEntityBoundingBox()).addCoord(vec3d1.x() * d0, vec3d1.y() * d0, vec3d1.z() * d0)
                            .expand(1.0, 1.0, 1.0).getAxisAlignedBB(),
                    Predicates.and(EntitySelectors.NOT_SPECTATING, entity1 -> entity1 != null && entity1.canBeCollidedWith())
            );
            double d2 = d1;
            for (int j = 0; j < list.size(); j++) {
                Entity entity1 = list.get(j);
                AxisAlignedBB axisalignedbb = new WrapperAxisAlignedBB(entity1.getEntityBoundingBox()).expand(entity1.getCollisionBorderSize());
                MovingObjectPosition raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
                if (axisalignedbb.isVecInside(vec3d)) {
                    if (d2 >= 0.0) {
                        vec3d3 = raytraceresult.hitVec;
                        d2 = 0.0;
                    }
                } else if (raytraceresult != null) {
                    double d3 = vec3d.distanceTo(raytraceresult.hitVec);
                    if (d3 < d2 || d2 == 0.0) {
                        vec3d3 = raytraceresult.hitVec;
                    }
                }
            }
            double distance = new WrapperVec3(vec3d).distanceTo(vec3d3);
            reach = (distance * 100) / 100;
        }
    }
}
