package top.fpsmaster.features.impl.interfaces;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.interfaces.ProviderManager;

import java.awt.*;

public class TargetDisplay extends InterfaceModule {
    private final ModeSetting targetESP = new ModeSetting("TargetESP", 0, "glow", "none");
    private final ColorSetting espColor = new ColorSetting("EspColor", new Color(255, 255, 255, 255), () -> !targetESP.isMode("none"));
    public static ModeSetting targetHUD = new ModeSetting("TargetHUD", 0, "simple", "none");
    public static BooleanSetting omit = new BooleanSetting("OmitName", true);
    public static EntityPlayer target;
    public static long lastHit;

    public TargetDisplay() {
        super("TargetDisplay", Category.Interface);
        addSettings(targetESP, targetHUD, espColor, omit);
    }

    @Subscribe
    public void onRender(EventRender3D e) {
        if (target != null && target.getHealth() > 0 && target.isEntityAlive() && System.currentTimeMillis() - lastHit < 3000) {
            if (targetESP.getMode() == 0) {
                drawCircle(target, 0.55, true);
            }
        }
    }

    private void drawCircle(Entity entity, double rad, boolean shade) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0f);
        if (shade) GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * ProviderManager.timerProvider.getRenderPartialTicks() - ProviderManager.renderManagerProvider.renderPosX();
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * ProviderManager.timerProvider.getRenderPartialTicks() - ProviderManager.renderManagerProvider.renderPosY() + Math.sin(System.currentTimeMillis() / 2E+2) + 1;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * ProviderManager.timerProvider.getRenderPartialTicks() - ProviderManager.renderManagerProvider.renderPosZ();
        Color c;
        GL11.glColor4f(1f, 0f, 0f, 0f);
        float i = 0f;
        while (i < Math.PI * 2) {
            double vecX = x + rad * Math.cos(i);
            double vecZ = z + rad * Math.sin(i);
            c = espColor.getColor();
            if (shade) {
                GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 0f);
                GL11.glVertex3d(vecX, y - Math.cos(System.currentTimeMillis() / 2E+2) / 2.0f, vecZ);
                GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 0.75f);
            }
            GL11.glVertex3d(vecX, y, vecZ);
            i += (Math.PI * 2 / 64f);
        }
        GL11.glEnd();
        if (shade) GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glColor3f(1f, 1f, 1f);
    }

    @Subscribe
    public void onAttack(EventAttack e) {
        if (e.target instanceof EntityPlayer) {
            target = (EntityPlayer) e.target;
            lastHit = System.currentTimeMillis();
        }
    }
}
