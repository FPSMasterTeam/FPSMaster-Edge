package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.utils.math.MathTimer;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class DamageIndicator extends Module {
    private EntityLivingBase lastAttack;

    public DamageIndicator() {
        super("DamageIndicator", Category.RENDER);
    }

    static ArrayList<Damage> indicators = new ArrayList<>();

    public static void addIndicator(float x, float y, float z, float damage) {
        indicators.add(new Damage(damage, x, y, z, 0f));
    }

    MathTimer timer = new MathTimer();
    float health = 0;

    @Subscribe
    public void onAttack(EventAttack e) {
        if (e.target instanceof EntityLivingBase) {
            lastAttack = (EntityLivingBase) e.target;
            health = lastAttack.getHealth();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (lastAttack == null)
            return;
        if (health - lastAttack.getHealth() != 0){
            addIndicator((float) (lastAttack.posX), (float) (lastAttack.posY - 1), (float) (lastAttack.posZ), health - lastAttack.getHealth());
            health = lastAttack.getHealth();
        }
    }

    @Subscribe
    public void onRender(EventRender3D event) {
        ArrayList<Damage> indicatorsRemove = new ArrayList<>();
        for (Damage indicator : indicators) {
            doRender(indicator);
        }
        if (timer.delay(20)) {
            for (Damage indicator : indicators) {
                indicator.animation += 0.05f;
                if (indicator.animation > 1) {
                    indicatorsRemove.add(indicator);
                }
            }
            if (!indicatorsRemove.isEmpty()) {
                indicators.removeAll(indicatorsRemove);
            }
        }
    }

    public void doRender(Damage indicator) {
        Minecraft mc = Minecraft.getMinecraft();
        DecimalFormat df = new DecimalFormat("0.00");
        String damage = df.format(-indicator.damage);
        GL11.glPushAttrib(GL11.GL_ALPHA | GL11.GL_BLEND | GL11.GL_TEXTURE_2D | GL11.GL_LIGHTING | GL11.GL_DEPTH_TEST | GL11.GL_CULL_FACE);
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        float partialTicks = Wrappers.timer().getRenderPartialTicks();
        double x = indicator.x + 1 - Wrappers.renderManager().renderPosX();
        double y = indicator.y - Wrappers.renderManager().renderPosY() + 1;
        double z = indicator.z + 1 - Wrappers.renderManager().renderPosZ();
        float scale = 0.065f;
        GlStateManager.translate(x, y + 1 + 0.5f - 1 / 2.0f, z);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GL11.glScalef(-scale / 2, -scale, -scale);
        float width = Wrappers.minecraft().getFontRenderer().getStringWidth(damage) / 2.0f;
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        int alpha = (int) (255 - indicator.animation * 255);
        alpha = Math.max(0, Math.min(255, alpha));

        Color color = new Color(50, 255, 50, alpha);
        if (indicator.damage > 0) {
            color = new Color(224, 41, 41, alpha);
        }
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GlStateManager.enableBlend();
        Wrappers.minecraft().getFontRenderer().drawStringWithShadow(damage, -width + 5, indicator.animation * 10, color.getRGB());
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glNormal3f(1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static class Damage {
        float damage;
        float x, y, z;
        float animation;

        public Damage(float damage, float x, float y, float z, float animation) {
            this.damage = damage;
            this.x = x;
            this.y = y;
            this.z = z;
            this.animation = animation;
        }
    }
}