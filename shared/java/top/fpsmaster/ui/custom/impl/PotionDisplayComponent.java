package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.PotionDisplay;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.interfaces.ProviderManager;

import java.awt.Color;

public class PotionDisplayComponent extends Component {

    public PotionDisplayComponent() {
        super(PotionDisplay.class);
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        float dY = y;

        GlStateManager.pushMatrix();

        for (net.minecraft.potion.PotionEffect effect : ProviderManager.mcProvider.getPlayer().getActivePotionEffects()) {
            String title = I18n.format(effect.getEffectName()) + " lv." + (effect.getAmplifier() + 1);
            String duration = (effect.getDuration() / 20 / 60) + "min" + effect.getDuration() / 20 % 60 + "s";
            float width = Math.max(getStringWidth(18, title), getStringWidth(16, duration)) + 36;
            drawRect(x, dY, width + 10, 32f, mod.backgroundColor.getColor());
            drawString(18, title, x + 34, dY + 5, -1);
            drawString(16, duration, x + 34, dY + 18, new Color(200, 200, 200).getRGB());

            // Draw potion image
            ResourceLocation res = new ResourceLocation("textures/gui/container/inventory.png");
            Utility.mc.getTextureManager().bindTexture(res);

            // Get potion icon index
            int potion = ProviderManager.utilityProvider.getPotionIconIndex(effect);

            // Draw potion
            Gui.drawModalRectWithCustomSizedTexture(
                    (int) (x + 8),
                    (int) (dY + 8),
                    (potion % 8 * 18),
                    (198 + potion / 8 * 18),
                    16,
                    16,
                    256f,
                    256f
            );

            dY += 36f;
            this.width = width + 12;
        }

        GlStateManager.popMatrix();
        height = dY - y - 4;
    }
}
