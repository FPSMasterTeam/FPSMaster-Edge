package top.fpsmaster.features.impl.utility;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.render.Render2DUtils;

import static top.fpsmaster.utils.Utility.mc;

public class LevelTag extends Module {

    public static boolean using = false;
    public static final BooleanSetting showSelf = new BooleanSetting("ShowSelf", true);
    public static final BooleanSetting health = new BooleanSetting("Health", true);

    public LevelTag() {
        super("Nametags", Category.Utility);
        addSettings(showSelf, health);
    }

    public static void renderHealth(Entity entityIn, String str, double x, double y, double z, int maxDistance) {
        double d = entityIn.getDistanceSqToEntity(mc.getRenderManager().livingPlayer);
        if (d < 100) {
            float f = 1.6F;
            float g = 0.016666668F * f;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            if (mc.gameSettings.thirdPersonView == 2)
                GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            else if (mc.gameSettings.thirdPersonView == 1)
                GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

            GlStateManager.scale(-g, -g, g);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            ProviderManager.guiIngameProvider.drawHealth(entityIn);
            GlStateManager.disableTexture2D();
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    public static void renderName(Entity entityIn, String str, double x, double y, double z, int maxDistance) {
        double d = entityIn.getDistanceSqToEntity(mc.getRenderManager().livingPlayer);
        if (!(d > (double)(maxDistance * maxDistance))) {
            FontRenderer fontRenderer = mc.fontRendererObj;
            float f = 1.6F;
            float g = 0.016666668F * f;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)x + 0.0F, (float)y + entityIn.height + 0.5F, (float)z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            if (mc.gameSettings.thirdPersonView == 2)
                GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            else if (mc.gameSettings.thirdPersonView == 1)
                GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

            GlStateManager.scale(-g, -g, g);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            int i = 0;
            if (str.equals("deadmau5")) {
                i = -10;
            }

            boolean isMate = (entityIn == mc.thePlayer) && str.contains(entityIn.getName());
            int j = fontRenderer.getStringWidth(str) / 2;

            if (isMate) {
                j += 6;
            }

            GlStateManager.disableTexture2D();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(-j - 1, -1 + i, 0.0F).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(-j - 1, 8 + i, 0.0F).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(j + 1, 8 + i, 0.0F).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldRenderer.pos(j + 1, -1 + i, 0.0F).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
            if (isMate) {
                Render2DUtils.drawImage(new ResourceLocation("client/textures/mate.png"), -fontRenderer.getStringWidth(str) / 2f - 4f, i - 1, 8, 8, -1, true);
                fontRenderer.drawString(str, -fontRenderer.getStringWidth(str) / 2 + 6, i, 553648127);
            }else{
                fontRenderer.drawString(str, -fontRenderer.getStringWidth(str) / 2, i, 553648127);
            }
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            if (isMate) {
                fontRenderer.drawString(str, -fontRenderer.getStringWidth(str) / 2 + 6, i, -1);
            }else{
                fontRenderer.drawString(str, -fontRenderer.getStringWidth(str) / 2, i, -1);
            }
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
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

    public static boolean isUsing() {
        return using;
    }
}
