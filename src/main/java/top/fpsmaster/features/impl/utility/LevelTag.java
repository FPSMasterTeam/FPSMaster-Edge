package top.fpsmaster.features.impl.utility;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.utils.render.draw.Images;

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

import java.awt.*;

import static top.fpsmaster.utils.core.Utility.mc;

public class LevelTag extends Module {
    private static final ResourceLocation MATE_ICON = new ResourceLocation("client/textures/mate.png");

    public static final BooleanSetting showSelf = new BooleanSetting("ShowSelf", true);
    public static final BooleanSetting betterFont = new BooleanSetting("BetterFont", false);
    public static final BooleanSetting fontShadow = new BooleanSetting("FontShadow", true);
    public static final BooleanSetting bg = new BooleanSetting("Background", true);
    public static final ColorSetting backgroundColor = new ColorSetting("BackgroundColor", new Color(0, 0, 0, 0), bg::getValue);
    public static boolean using = false;

    public LevelTag() {
        super("Nametags", Category.Utility);
        addSettings(showSelf, backgroundColor, fontShadow, betterFont, bg);
    }

    public static void renderName(Entity entityIn, String str, double x, double y, double z, int maxDistance) {
        if ((!using || !showSelf.getValue()) && entityIn == mc.thePlayer) {
            return;
        }

        double d = entityIn.getDistanceSqToEntity(mc.getRenderManager().livingPlayer);

        if (!(d > (double) (maxDistance * maxDistance))) {
            FontRenderer fontRenderer = betterFont.getValue() ? FPSMaster.fontManager.s18 : mc.fontRendererObj;

            float f = 1.6F;
            float g = 0.016666668F * f;

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);

            /*
             * 原版 1.8.9 nametag 朝向逻辑：
             *
             * playerViewY 控制左右方向，前面需要负号；
             * playerViewX 控制上下俯仰方向，直接使用正值；
             *
             * 不要根据 first person / third person 分支处理，
             * RenderManager.playerViewX / playerViewY 本身就应该代表当前相机方向。
             */
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
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

            if (betterFont.getValue()) {
                i -= 2;
            }

            boolean isMate = entityIn == mc.thePlayer && str.contains(entityIn.getName());
            int textWidth = fontRenderer.getStringWidth(str);
            int textX = -textWidth / 2;
            float iconX = textX - 4f;

            if (isMate) {
                textX += 6;
            }

            float backgroundLeft = isMate ? Math.min(iconX, textX) - 1f : textX - 1f;
            float backgroundRight = textX + textWidth + 1f;

            if (bg.getValue()) {
                GlStateManager.disableTexture2D();

                Color color = backgroundColor.getColor();
                float red = color.getRed() / 255f;
                float green = color.getGreen() / 255f;
                float blue = color.getBlue() / 255f;
                float alpha = color.getAlpha() / 255f;

                worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

                worldRenderer
                        .pos(backgroundLeft, -1 + i, 0.0F)
                        .color(red, green, blue, alpha)
                        .endVertex();

                worldRenderer
                        .pos(backgroundLeft, 8 + i, 0.0F)
                        .color(red, green, blue, alpha)
                        .endVertex();

                worldRenderer
                        .pos(backgroundRight, 8 + i, 0.0F)
                        .color(red, green, blue, alpha)
                        .endVertex();

                worldRenderer
                        .pos(backgroundRight, -1 + i, 0.0F)
                        .color(red, green, blue, alpha)
                        .endVertex();

                tessellator.draw();
            }

            GL11.glColor4f(1, 1, 1, 1);
            GlStateManager.enableTexture2D();

            /*
             * 第一遍绘制：关闭深度时绘制暗色文字。
             */
            if (isMate) {
                Images.draw(
                        MATE_ICON,
                        iconX,
                        i - 1,
                        8,
                        8,
                        -1,
                        true
                );
            }
            fontRenderer.drawString(str, textX, i, 553648127);

            /*
             * 第二遍绘制：开启深度后绘制正常白色文字。
             */
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);

            if (isMate) {
                if (fontShadow.getValue()) {
                    fontRenderer.drawStringWithShadow(
                            str,
                            textX,
                            i,
                            -1
                    );
                } else {
                    fontRenderer.drawString(
                            str,
                            textX,
                            i,
                            -1
                    );
                }
            } else {
                if (fontShadow.getValue()) {
                    fontRenderer.drawStringWithShadow(
                            str,
                            textX,
                            i,
                            -1
                    );
                } else {
                    fontRenderer.drawString(
                            str,
                            textX,
                            i,
                            -1
                    );
                }
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
}