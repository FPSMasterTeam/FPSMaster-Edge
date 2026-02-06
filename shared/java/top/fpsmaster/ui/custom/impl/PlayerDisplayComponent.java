package top.fpsmaster.ui.custom.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.PlayerDisplay;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

import static top.fpsmaster.utils.Utility.mc;

public class PlayerDisplayComponent extends Component {

    public PlayerDisplayComponent() {
        super(PlayerDisplay.class);
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        width = 40f;
        int i = 0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && !entity.isInvisible()) {
                if (i > 10 || entity == mc.thePlayer) continue;
                UFontRenderer s16 = FPSMaster.fontManager.s16;
                String healthText = (int) (((EntityPlayer) entity).getHealth() * 10 / 10) + " hp";
                float hX = s16.getStringWidth(healthText);
                float nX = s16.getStringWidth(entity.getDisplayName().getFormattedText());

                Render2DUtils.drawOptimizedRoundedRect(x, y + i * 16, 10 + hX + nX, 14f, new Color(0, 0, 0, 60));
                Render2DUtils.drawOptimizedRoundedRect(
                        x,
                        y + i * 16,
                        (10 + hX + nX) * ((EntityPlayer) entity).getHealth() / ((EntityPlayer) entity).getMaxHealth(),
                        14f,
                        new Color(0, 0, 0, 60)
                );

                if (width < 10 + hX + nX) {
                    width = 10 + hX + nX;
                }

                float health = ((EntityPlayer) entity).getHealth();
                float maxHealth = ((EntityPlayer) entity).getMaxHealth();
                Color color = health >= maxHealth * 0.8f ? new Color(50, 255, 55) :
                        health > maxHealth * 0.5f ? new Color(255, 255, 55) :
                                new Color(255, 55, 55);

                s16.drawString(entity.getDisplayName().getFormattedText(), x + 2, y + i * 16 + 2, -1);
                s16.drawString(healthText, x + 8 + nX, y + i * 16 + 2, color.getRGB());

                i++;
            }
        }

        height = (18 * i);
    }
}
