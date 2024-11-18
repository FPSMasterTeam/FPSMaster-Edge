package top.fpsmaster.features.impl.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.render.Render3DUtils;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;
import top.fpsmaster.wrapper.util.WrapperAxisAlignedBB;

import java.awt.*;

public class BlockOverlay extends Module {
    private BooleanSetting fill = new BooleanSetting("Fill", true);
    private BooleanSetting outline = new BooleanSetting("Outline", true);
    private BooleanSetting throughBlock = new BooleanSetting("ThroughBlock", false);
    private NumberSetting width = new NumberSetting("Width", 1, 0.1, 10, 0.1, ()->outline.getValue());
    private ColorSetting color1 = new ColorSetting("FillColor", new Color(255, 255, 255, 50), ()->fill.getValue());
    private ColorSetting color2 = new ColorSetting("OutlineColor", new Color(255, 255, 255, 255), ()->outline.getValue());
    public static boolean using = false;

    public BlockOverlay(){
        super("BlockOverlay", Category.RENDER);
        addSettings(fill, color1, outline, width, color2, throughBlock);
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

    @Subscribe
    public void onRender3D(EventRender3D e) {
        if (Minecraft.getMinecraft().objectMouseOver != null) {
            if (ProviderManager.mcProvider.isHoveringOverBlock()) {
                WrapperBlockPos pos = new WrapperBlockPos(Companion.getMc().objectMouseOver.getBlockPos());
                IBlockState state = ProviderManager.worldClientProvider.getBlockState(pos);
                Block block = ProviderManager.worldClientProvider.getBlock(pos);
                double x = pos.getX() - Minecraft.getMinecraft().getRenderManager().viewerPosX;
                double y = pos.getY() - Minecraft.getMinecraft().getRenderManager().viewerPosY;
                double z = pos.getZ() - Minecraft.getMinecraft().getRenderManager().viewerPosZ;
                GL11.glPushMatrix();
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glEnable(2848);
                if (throughBlock.getValue()) {
                    GL11.glDisable(2929);
                }
                GL11.glDepthMask(false);
                WrapperAxisAlignedBB blockBoundingBox = ProviderManager.worldClientProvider.getBlockBoundingBox(pos, state);
                double minX = block instanceof BlockStairs ? 0.0 : blockBoundingBox.minX();
                double minY = block instanceof BlockStairs ? 0.0 : blockBoundingBox.minY();
                double minZ = block instanceof BlockStairs ? 0.0 : blockBoundingBox.minZ();
                if (fill.getValue()) {
                    Color color = color1.getValue().getColor();
                    GL11.glPushMatrix();
                    GlStateManager.color(
                            color.getRed() / 255.0f,
                            color.getGreen() / 255.0f,
                            color.getBlue() / 255.0f,
                            color.getAlpha() / 255.0f
                    );
                    Render3DUtils.drawBoundingBox(
                            new WrapperAxisAlignedBB(
                                    x + minX - 0.01,
                                    y + minY - 0.01,
                                    z + minZ - 0.01,
                                    x + blockBoundingBox.maxX() + 0.01,
                                    y + blockBoundingBox.maxY() + 0.01,
                                    z + blockBoundingBox.maxZ() + 0.01
                            )
                    );
                    GL11.glPopMatrix();
                }
                if (outline.getValue()) {
                    Color color = color2.getValue().getColor();
                    GL11.glPushMatrix();
                    GlStateManager.color(
                            color.getRed() / 255.0f,
                            color.getGreen() / 255.0f,
                            color.getBlue() / 255.0f,
                            color.getAlpha() / 255.0f
                    );
                    GL11.glLineWidth(width.getValue().floatValue());
                    Render3DUtils.drawBoundingBoxOutline(
                            new WrapperAxisAlignedBB(
                                    x + minX - 0.005,
                                    y + minY - 0.005,
                                    z + minZ - 0.005,
                                    x + blockBoundingBox.maxX() + 0.005,
                                    y + blockBoundingBox.maxY() + 0.005,
                                    z + blockBoundingBox.maxZ() + 0.005
                            )
                    );
                    GL11.glPopMatrix();
                }
                GL11.glDisable(2848);
                GL11.glEnable(3553);
                if (throughBlock.getValue()) {
                    GL11.glEnable(2929);
                }
                GL11.glDepthMask(true);
                GL11.glLineWidth(1.0f);
                GL11.glPopMatrix();
            }
        }
    }
}
