package top.fpsmaster.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.wrapper.GuiIngameWrap;

import java.util.Random;

import static net.minecraft.client.gui.Gui.icons;
import static top.fpsmaster.utils.Utility.mc;

public class GuiIngameProvider implements GuiIngameWrap {
    protected static final Random rand = new Random();
    protected static int playerHealth = 0;
    protected static int lastPlayerHealth = 0;
    protected static long lastSystemTime = 0L;
    protected static long healthUpdateCounter = 0L;

    public void drawHealth(@NotNull Entity entity) {
        if (!(entity instanceof EntityPlayer))
            return;
        mc.getTextureManager().bindTexture(icons);
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        EntityPlayer entityPlayer = (EntityPlayer) entity;
        int i = MathHelper.ceiling_float_int(entityPlayer.getHealth());
        boolean bl = healthUpdateCounter > (long) mc.ingameGUI.getUpdateCounter() && (healthUpdateCounter - (long) mc.ingameGUI.getUpdateCounter()) / 3L % 2L == 1L;
        if (i < playerHealth && entityPlayer.hurtResistantTime > 0) {
            lastSystemTime = Minecraft.getSystemTime();
            healthUpdateCounter = mc.ingameGUI.getUpdateCounter() + 20;
        } else if (i > playerHealth && entityPlayer.hurtResistantTime > 0) {
            lastSystemTime = Minecraft.getSystemTime();
            healthUpdateCounter = mc.ingameGUI.getUpdateCounter() + 10;
        }

        if (Minecraft.getSystemTime() - lastSystemTime > 1000L) {
            playerHealth = i;
            lastPlayerHealth = i;
            lastSystemTime = Minecraft.getSystemTime();
        }

        playerHealth = i;
        int j = lastPlayerHealth;
        rand.setSeed(mc.ingameGUI.getUpdateCounter() * 312871L);
        IAttributeInstance iAttributeInstance = entityPlayer.getEntityAttribute(SharedMonsterAttributes.maxHealth);
        int m = -45;
        int o = -10;
        float f = (float) iAttributeInstance.getAttributeValue();
        float g = entityPlayer.getAbsorptionAmount();
        int p = MathHelper.ceiling_float_int((f + g) / 2.0F / 10.0F);
        int q = Math.max(10 - (p - 2), 3);
        int r = o - (p - 1) * q - 10;
        float h = g;
        int s = entityPlayer.getTotalArmorValue();
        int t = -1;
        if (entityPlayer.isPotionActive(Potion.regeneration)) {
            t = mc.ingameGUI.getUpdateCounter() % MathHelper.ceiling_float_int(f + 5.0F);
        }

        int u;
        int v;
        for (u = 0; u < 10; ++u) {
            if (s > 0) {
                v = m + u * 8;
                if (u * 2 + 1 < s) {
                    drawTexturedModalRect(v, r, 34, 9, 9, 9);
                }

                if (u * 2 + 1 == s) {
                    drawTexturedModalRect(v, r, 25, 9, 9, 9);
                }

                if (u * 2 + 1 > s) {
                    drawTexturedModalRect(v, r, 16, 9, 9, 9);
                }
            }
        }

        int w;
        int x;
        int y;
        int z;
        int aa;
        for (u = MathHelper.ceiling_float_int((f + g) / 2.0F) - 1; u >= 0; --u) {
            v = 16;
            if (entityPlayer.isPotionActive(Potion.poison)) {
                v += 36;
            } else if (entityPlayer.isPotionActive(Potion.wither)) {
                v += 72;
            }

            w = 0;
            if (bl) {
                w = 1;
            }

            x = MathHelper.ceiling_float_int((float) (u + 1) / 10.0F) - 1;
            y = m + u % 10 * 8;
            z = o - x * q;
            if (i <= 4) {
                z += rand.nextInt(2);
            }

            if (u == t) {
                z -= 2;
            }

            aa = 0;
            if (entityPlayer.worldObj.getWorldInfo().isHardcoreModeEnabled()) {
                aa = 5;
            }

            drawTexturedModalRect(y, z, 16 + w * 9, 9 * aa, 9, 9);
            if (bl) {
                if (u * 2 + 1 < j) {
                    drawTexturedModalRect(y, z, v + 54, 9 * aa, 9, 9);
                }

                if (u * 2 + 1 == j) {
                    drawTexturedModalRect(y, z, v + 63, 9 * aa, 9, 9);
                }
            }

            if (h > 0.0F) {
                if (h == g && g % 2.0F == 1.0F) {
                    drawTexturedModalRect(y, z, v + 153, 9 * aa, 9, 9);
                } else {
                    drawTexturedModalRect(y, z, v + 144, 9 * aa, 9, 9);
                }

                h -= 2.0F;
            } else {
                if (u * 2 + 1 < i) {
                    drawTexturedModalRect(y, z, v + 36, 9 * aa, 9, 9);
                }

                if (u * 2 + 1 == i) {
                    drawTexturedModalRect(y, z, v + 45, 9 * aa, 9, 9);
                }
            }
        }
        GlStateManager.disableTexture2D();
    }

    public static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        float f = 0.00390625F;
        float g = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(x, y + height, 0.0).tex((float) (textureX) * f, (float) (textureY + height) * g).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0).tex((float) (textureX + width) * f, (float) (textureY + height) * g).endVertex();
        worldRenderer.pos(x + width, y, 0.0).tex((float) (textureX + width) * f, (float) (textureY) * g).endVertex();
        worldRenderer.pos(x, y, 0.0).tex((float) (textureX) * f, (float) (textureY) * g).endVertex();
        tessellator.draw();
    }
}
