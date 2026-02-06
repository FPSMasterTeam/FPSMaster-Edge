package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.PotionDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.Utility;

import java.awt.*;

import static top.fpsmaster.utils.Utility.mc;

public class PotionDisplayComponent extends Component {

    public PotionDisplayComponent() {
        super(PotionDisplay.class);
        allowScale = true;
    }

    public static final float POTION_HEIGHT = 36f;

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        float dY = y - mod.spacing.getValue().intValue();
        GlStateManager.pushMatrix();
        int index = 0;
        for (PotionEffect effect : mc.thePlayer.getActivePotionEffects()) {
            String title = I18n.format(effect.getEffectName()) + " lv." + (effect.getAmplifier() + 1);
            String duration = (effect.getDuration() / 20 / 60) + "min" + effect.getDuration() / 20 % 60 + "s";
            float width = Math.max(getStringWidth(18, title), getStringWidth(16, duration)) + 36;
            drawRect(x, dY, width + 10, 32f, mod.backgroundColor.getColor());
            drawString(18, title, x + 34 * scale, dY + 5, -1);
            drawString(16, duration, x + 34 * scale, dY + 5 + 13 * scale, new Color(200, 200, 200).getRGB());

            // Draw potion image
            ResourceLocation res = new ResourceLocation("textures/gui/container/inventory.png");
            Utility.mc.getTextureManager().bindTexture(res);

            // Get potion icon index
            int potion = getPotionIconIndex(effect);

            // Draw potion
            GL11.glTranslatef((int) (x + 8), (int) (dY + 8), 0);
            GL11.glScalef(scale, scale, 0);
            Gui.drawModalRectWithCustomSizedTexture(
                    0,
                    0,
                    (potion % 8 * 18) + 1,
                    (198 + (float)(potion / 8) * 18) + 1,
                    16,
                    16,
                    256f,
                    256f
            );
            GL11.glScalef(1 / scale, 1 / scale, 0);
            GL11.glTranslatef(-(int) (x + 8), -(int) (dY + 8), 0);

            dY += (index * mod.spacing.getValue().intValue() * 2 + POTION_HEIGHT) * scale;
            this.width = width + 12;
            index++;
        }

        GlStateManager.popMatrix();
        height = index * (mod.spacing.getValue().intValue() + POTION_HEIGHT);
    }

    private int getPotionIconIndex(PotionEffect effect) {
        Potion p = Potion.potionTypes[effect.getPotionID()];
        return p.getStatusIconIndex();
    }
}
