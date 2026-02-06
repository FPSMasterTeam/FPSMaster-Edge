package top.fpsmaster.features.impl.render;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.utils.render.Render3DUtils;

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
        if (!using) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.0f);
        Color outline = color.getColor();
        GL11.glColor4f(
                outline.getRed() / 255f,
                outline.getGreen() / 255f,
                outline.getBlue() / 255f,
                outline.getAlpha() / 255f
        );
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) {
                continue;
            }
            AxisAlignedBB bb = entity.getEntityBoundingBox()
                    .offset(-mc.getRenderManager().renderPosX, -mc.getRenderManager().renderPosY, -mc.getRenderManager().renderPosZ);
            Render3DUtils.drawBoundingBoxOutline(bb);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }
}
