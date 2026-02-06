package top.fpsmaster.features.impl.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityTNTPrimed;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.*;
import java.text.DecimalFormat;

public class TNTTimer extends Module {

    private static boolean using = false;
    private static final NumberSetting duration = new NumberSetting("Duration", 4, 1, 10, 0.1);

    public TNTTimer() {
        super("TNTTimer", Category.Utility);
        addSettings(duration);
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

    public static void doRender(EntityTNTPrimed entity) {
        if (!using) return;
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALPHA | GL11.GL_BLEND | GL11.GL_TEXTURE_2D | GL11.GL_LIGHTING | GL11.GL_DEPTH_TEST | GL11.GL_CULL_FACE);
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        float partialTicks = mc.timer.renderPartialTicks;
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - mc.getRenderManager().renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - mc.getRenderManager().renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - mc.getRenderManager().renderPosZ;
        float scale = 0.065f;
        GlStateManager.translate(x, y + entity.height + 0.5f - entity.height / 2.0f, z);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GL11.glScalef(-scale / 2, -scale, -scale);
        double xLeft = -10.0;
        double xRight = 10.0;
        double yUp = -20.0;
        double yDown = -10.0;
        drawRect((float) xLeft, (float) yUp, (float) xRight, (float) yDown, new Color(0, 0, 0, 100).getRGB());
        drawTime(entity);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GlStateManager.disableBlend();
        GL11.glDisable(3042);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glNormal3f(1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static void drawTime(EntityTNTPrimed entity) {
        float width = Minecraft.getMinecraft().fontRendererObj.getStringWidth("0.00") / 2.0f + 6.0f;
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        DecimalFormat df = new DecimalFormat("0.00");
        Color color = new Color(255, 255, 255);
        float time = (float) (entity.fuse / 20.0 + duration.getValue().doubleValue() - 4);
        if (time < 2.5) {
            color = new Color(255, 255, 0);
        }
        if (time < 1.0) {
            color = new Color(255, 0, 0);
        }
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(df.format(time), -width + 5, -20f, color.getRGB());
        GlStateManager.enableLighting();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
    }

    public static void drawRect(float g, float h, float i, float j, int col1) {
        float f = (col1 >> 24 & 0xFF) / 255.0f;
        float f1 = (col1 >> 16 & 0xFF) / 255.0f;
        float f2 = (col1 >> 8 & 0xFF) / 255.0f;
        float f3 = (col1 & 0xFF) / 255.0f;
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glPushMatrix();
        GL11.glColor4f(f1, f2, f3, f);
        GL11.glBegin(7);
        GL11.glVertex2d(i, h);
        GL11.glVertex2d(g, h);
        GL11.glVertex2d(g, j);
        GL11.glVertex2d(i, j);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
    }
}
