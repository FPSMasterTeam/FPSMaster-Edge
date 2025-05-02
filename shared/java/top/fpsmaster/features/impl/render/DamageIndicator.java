package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.wrapper.entities.EntityTNTPrimedUtil;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class DamageIndicator extends Module {
    public DamageIndicator() {
        super("DamageIndicator", Category.RENDER);
    }

    static ArrayList<Damage> indicators = new ArrayList<>();

    public static void addIndicator(float x, float y, float z, float damage) {
        indicators.add(new Damage(damage, x, y, z, 0f));
    }

    MathTimer timer = new MathTimer();


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
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(2929);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        float partialTicks = ProviderManager.timerProvider.getRenderPartialTicks();
        double x = indicator.x + 1 - ProviderManager.renderManagerProvider.renderPosX();
        double y = indicator.y - ProviderManager.renderManagerProvider.renderPosY() + 1;
        double z = indicator.z + 1 - ProviderManager.renderManagerProvider.renderPosZ();
        float scale = 0.065f;
        GlStateManager.translate(x, y + 1 + 0.5f - 1 / 2.0f, z);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GL11.glScalef(-scale / 2, -scale, -scale);
        float width = ProviderManager.mcProvider.getFontRenderer().getStringWidth(damage) / 2.0f + 6.0f;
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
        ProviderManager.mcProvider.getFontRenderer().drawStringWithShadow(damage, -width + 5, indicator.animation * 10, color.getRGB());
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glNormal3f(1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }
}

class Damage {
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