package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.PlayerDisplay;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.custom.Component;

import java.awt.*;

import static top.fpsmaster.utils.core.Utility.mc;

public class PlayerDisplayComponent extends Component {

    public PlayerDisplayComponent() {
        super(PlayerDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        width = 40f;
        int i = 0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && !entity.isInvisible()) {
                if (i > 10 || entity == mc.thePlayer) continue;
                EntityPlayer player = (EntityPlayer) entity;
                String name = entity.getDisplayName().getFormattedText();
                String healthText = (int) (player.getHealth() * 10 / 10) + " hp";
                // Logical (unscaled) widths — drawRect/drawString apply scale themselves.
                float hX = getStringWidth(16, healthText);
                float nX = getStringWidth(16, name);
                float rowWidth = 10 + hX + nX;
                // Row offsets are positions, so they scale here; sizes scale inside drawRect.
                float rowY = y + i * 16 * scale;

                drawRect(x, rowY, rowWidth, 14, mod.backgroundColor.getColor());
                drawRect(x, rowY, rowWidth * player.getHealth() / player.getMaxHealth(), 14,
                        mod.backgroundColor.getColor());

                if (width < rowWidth) {
                    width = rowWidth;
                }

                float health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                Color color = health >= maxHealth * 0.8f ? new Color(50, 255, 55) :
                        health > maxHealth * 0.5f ? new Color(255, 255, 55) :
                                new Color(255, 55, 55);

                drawString(16, name, x + 2 * scale, rowY + 2 * scale, -1);
                drawString(16, healthText, x + (8 + nX) * scale, rowY + 2 * scale, color.getRGB());

                i++;
            }
        }

        height = (18 * i);
    }

    @Override
    public void measurePreview() {
        width = getStringWidth(16, "Player 20 hp") + 12;
        height = 36f;
    }

    @Override
    public void drawPreview(float x, float y) {
        String[] names = {"Player", "Teammate"};
        String[] health = {"20 hp", "16 hp"};
        for (int index = 0; index < names.length; index++) {
            float rowY = y + index * 18 * scale;
            drawRect(x, rowY, width, 14f, mod.backgroundColor.getColor());
            drawString(16, names[index], x + 2 * scale, rowY + 2 * scale, -1);
            drawString(16, health[index], x + (8 + getStringWidth(16, names[index])) * scale,
                    rowY + 2 * scale, index == 0 ? 0xFF32FF37 : 0xFFFFFF37);
        }
    }
}



