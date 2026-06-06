package top.fpsmaster.ui.custom.impl;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.BlockIndicator;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class BlockIndicatorComponent extends Component {
    private float alpha;
    private TargetBlock lastBlock;

    public BlockIndicatorComponent() {
        super(BlockIndicator.class);
        allowScale = true;
        position = Position.CT;
        x = 0.5f;
        y = 0f;
    }

    @Override
    public void draw(float x, float y) {
        Minecraft mc = Minecraft.getMinecraft();
        TargetBlock target = getTargetBlock(mc);
        if (target != null) {
            lastBlock = target;
        }

        alpha = (float) AnimMath.base(alpha, target == null ? 0.0F : 1.0F, 0.18F);
        if (alpha <= 0.02F || lastBlock == null) {
            updateSize(mc, null);
            return;
        }

        renderIndicator(mc, lastBlock, x, y, alpha, target != null);
    }

    @Override
    public float[] getRealPosition() {
        if (position != Position.CT) {
            return super.getRealPosition();
        }
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
        float scaleFactor = (float) top.fpsmaster.features.impl.interfaces.ClientSettings.getUiScale();
        if (scaleFactor <= 0) {
            scaleFactor = 1.0f;
        }
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        x = 0.5f;
        y = 0f;
        return new float[]{guiWidth / 2f - width * scale / 2f, getModule().yOffset.getValue().floatValue()};
    }

    private TargetBlock getTargetBlock(Minecraft mc) {
        if (mc.theWorld == null || mc.thePlayer == null || mc.objectMouseOver == null) {
            return null;
        }
        if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) {
            return null;
        }

        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.air || block.getMaterial().isReplaceable()) {
            return null;
        }

        ResourceLocation id = (ResourceLocation) Block.blockRegistry.getNameForObject(block);
        int meta = block.getMetaFromState(state);
        return new TargetBlock(
                block.getLocalizedName(),
                id == null ? "minecraft:unknown" : id.toString(),
                meta,
                pos,
                getDisplayStack(block, meta)
        );
    }

    private ItemStack getDisplayStack(Block block, int meta) {
        Item item = getDisplayItem(block);
        if (item == null) {
            return null;
        }
        return new ItemStack(item, 1, item.getHasSubtypes() ? meta : 0);
    }

    private Item getDisplayItem(Block block) {
        if (block == Blocks.standing_sign || block == Blocks.wall_sign) return Items.sign;
        if (block == Blocks.wall_banner || block == Blocks.standing_banner) return Items.banner;
        if (block == Blocks.oak_door) return Items.oak_door;
        if (block == Blocks.spruce_door) return Items.spruce_door;
        if (block == Blocks.birch_door) return Items.birch_door;
        if (block == Blocks.jungle_door) return Items.jungle_door;
        if (block == Blocks.acacia_door) return Items.acacia_door;
        if (block == Blocks.dark_oak_door) return Items.dark_oak_door;
        if (block == Blocks.iron_door) return Items.iron_door;
        if (block == Blocks.brewing_stand) return Items.brewing_stand;
        if (block == Blocks.cauldron) return Items.cauldron;
        if (block == Blocks.flower_pot) return Items.flower_pot;
        if (block == Blocks.reeds) return Items.reeds;
        if (block == Blocks.cake) return Items.cake;
        if (block == Blocks.bed) return Items.bed;
        if (block == Blocks.tripwire) return Items.string;
        if (block == Blocks.fire) return Items.flint_and_steel;
        if (block == Blocks.redstone_wire) return Items.redstone;
        if (block == Blocks.unpowered_repeater || block == Blocks.powered_repeater) return Items.repeater;
        if (block == Blocks.unpowered_comparator || block == Blocks.powered_comparator) return Items.comparator;
        return Item.getItemFromBlock(block);
    }

    private void renderIndicator(Minecraft mc, TargetBlock target, float x, float y, float fade, boolean hasTarget) {
        List<String> details = buildDetails(target);
        updateSize(mc, details);

        float renderY = y - (1.0F - fade) * 8.0F;
        float iconFade = hasTarget ? fade : 0.0F;
        Color bg = withAlpha(mod.backgroundColor.getColor(), fade);
        Color iconBg = withAlpha(getModule().panelColor.getColor(), iconFade);
        Color accent = withAlpha(getModule().accentColor.getColor(), fade);
        Color grid = withAlpha(new Color(255, 255, 255, 42), iconFade);
        Color titleColor = withAlpha(new Color(255, 255, 255), fade);
        Color detailColor = withAlpha(new Color(190, 198, 210), fade);

        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, fade);

            drawRect(x, renderY, width, height, bg);
            Rects.roundedImage(Math.round(x + 7), Math.round(renderY + 7), 3, Math.round(height - 14), 2, accent);

            float blockBox = 34.0F * scale;
            float blockX = x + 15.0F * scale;
            float blockY = renderY + height * scale / 2.0F - blockBox / 2.0F;
            Rects.roundedImage(Math.round(blockX), Math.round(blockY), Math.round(blockBox), Math.round(blockBox), Math.round(7 * scale), iconBg);
            drawGrid(blockX, blockY, blockBox, grid);
            renderBlockIcon(mc, target.stack, blockX + 8.5F * scale, blockY + 8.5F * scale, iconFade);

            float textX = blockX + blockBox + 10.0F * scale;
            float textY = renderY + 9.0F * scale;
            drawString(18, target.name, textX, textY, titleColor.getRGB());
            for (int i = 0; i < details.size(); i++) {
                drawString(16, details.get(i), textX, textY + (13.0F + i * 11.0F) * scale, detailColor.getRGB());
            }
        } finally {
            GL11.glPopAttrib();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private void updateSize(Minecraft mc, List<String> details) {
        FontRenderer font = mc.fontRendererObj;
        int detailWidth = 0;
        if (details != null) {
            for (String detail : details) {
                detailWidth = Math.max(detailWidth, font.getStringWidth(detail));
            }
        }
        String name = lastBlock == null ? "Block Indicator" : lastBlock.name;
        int textWidth = Math.max(font.getStringWidth(name), detailWidth);
        width = Math.max(174.0F, 64.0F + textWidth);
        height = Math.max(46.0F, 22.0F + (details == null ? 2 : details.size()) * 11.0F + 10.0F);
    }

    private List<String> buildDetails(TargetBlock target) {
        List<String> details = new ArrayList<>();
        BlockIndicator module = getModule();
        if (module.showId.getValue()) {
            details.add(target.registryName);
        }
        if (module.showCoords.getValue()) {
            BlockPos pos = target.pos;
            details.add("X " + pos.getX() + "  Y " + pos.getY() + "  Z " + pos.getZ());
        }
        return details;
    }

    private void drawGrid(float x, float y, float size, Color color) {
        for (int i = 1; i < 3; i++) {
            float offset = i * size / 3.0F;
            Rects.fill(x + offset, y + 5.0F * scale, 1.0F, size - 10.0F * scale, color);
            Rects.fill(x + 5.0F * scale, y + offset, size - 10.0F * scale, 1.0F, color);
        }
    }

    private void renderBlockIcon(Minecraft mc, ItemStack stack, float x, float y, float fade) {
        if (stack == null) {
            mc.fontRendererObj.drawStringWithShadow("?", x + 5.0F * scale, y + 4.0F * scale, withAlpha(new Color(255, 255, 255), fade).getRGB());
            return;
        }

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0.0F);
            GlStateManager.scale(1.08F * scale, 1.08F * scale, 1.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, fade);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GL11.glPopAttrib();
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private Color withAlpha(Color color, float fade) {
        int alphaValue = Math.max(0, Math.min(255, Math.round(color.getAlpha() * fade)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaValue);
    }

    private BlockIndicator getModule() {
        return (BlockIndicator) mod;
    }

    private static final class TargetBlock {
        private final String name;
        private final String registryName;
        private final int meta;
        private final BlockPos pos;
        private final ItemStack stack;

        private TargetBlock(String name, String registryName, int meta, BlockPos pos, ItemStack stack) {
            this.name = name;
            this.registryName = registryName;
            this.meta = meta;
            this.pos = pos;
            this.stack = stack;
        }
    }
}
