package top.fpsmaster.forge.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemCarrotOnAStick;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.render.FireModifier;

import java.awt.*;
import java.util.Objects;

import static top.fpsmaster.features.impl.optimizes.OldAnimations.*;
import static top.fpsmaster.utils.Utility.mc;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow
    private float equippedProgressMainHand;
    @Shadow
    private float prevEquippedProgressMainHand;
    @Shadow
    private float equippedProgressOffHand;
    @Shadow
    private float prevEquippedProgressOffHand;
    @Shadow
    private ItemStack itemStackMainHand;

    /**
     * @author SuperSkidder
     * @reason prevent cooldown animation
     */
    @Overwrite
    public void updateEquippedItem() {
        this.prevEquippedProgressMainHand = this.equippedProgressMainHand;
        this.prevEquippedProgressOffHand = this.equippedProgressOffHand;
        EntityPlayerSP entityplayersp = mc.player;
        ItemStack itemstack = entityplayersp.getHeldItemMainhand();
        ItemStack itemstack1 = entityplayersp.getHeldItemOffhand();
        if (entityplayersp.isRowingBoat()) {
            this.equippedProgressMainHand = MathHelper.clamp(this.equippedProgressMainHand - 0.4F, 0.0F, 1.0F);
            this.equippedProgressOffHand = MathHelper.clamp(this.equippedProgressOffHand - 0.4F, 0.0F, 1.0F);
        } else {
            float f = entityplayersp.getCooledAttackStrength(1.0F);
            boolean requipM = ForgeHooksClient.shouldCauseReequipAnimation(this.itemStackMainHand, itemstack, entityplayersp.inventory.currentItem);
            boolean requipO = ForgeHooksClient.shouldCauseReequipAnimation(this.itemStackOffHand, itemstack1, -1);
            if (!requipM && !Objects.equals(this.itemStackMainHand, itemstack)) {
                this.itemStackMainHand = itemstack;
            }

            if (!requipM && !Objects.equals(this.itemStackOffHand, itemstack1)) {
                this.itemStackOffHand = itemstack1;
            }
            if (OldAnimations.using && oldSwing.getValue()) {
                this.equippedProgressMainHand += MathHelper.clamp((Objects.equals(this.itemStackMainHand, itemstack) ? 1F : 0.0F) - this.equippedProgressMainHand, -0.4F, 0.4F);
            } else {
                this.equippedProgressMainHand += MathHelper.clamp((!requipM ? f * f * f : 0.0F) - this.equippedProgressMainHand, -0.4F, 0.4F);
            }
            this.equippedProgressOffHand += MathHelper.clamp((float)(!requipO ? 1 : 0) - this.equippedProgressOffHand, -0.4F, 0.4F);
        }

        if (this.equippedProgressMainHand < 0.1F) {
            this.itemStackMainHand = itemstack;
        }

        if (this.equippedProgressOffHand < 0.1F) {
            this.itemStackOffHand = itemstack1;
        }

    }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    public void renderFireInFirstPerson(CallbackInfo ci) {
        if (FireModifier.using) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
            GlStateManager.depthFunc(519);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            float f = 1.0F;

            for (int i = 0; i < 2; ++i) {
                GlStateManager.pushMatrix();
                TextureAtlasSprite textureatlassprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
                mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                float f1 = textureatlassprite.getMinU();
                float f2 = textureatlassprite.getMaxU();
                float f3 = textureatlassprite.getMinV();
                float f4 = textureatlassprite.getMaxV();
                float f5 = -0.5F;
                float f6 = 0.5F;
                float f7 = -0.5F;
                float f8 = 0.5F;
                float f9 = -0.5F;
                GlStateManager.translate(0, FireModifier.using ? -FireModifier.height.getValue().floatValue() : 0, 0);
                if (FireModifier.using && FireModifier.customColor.getValue()) {
                    Color color = FireModifier.colorSetting.getColor();
                    GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.9F);
                }
                GlStateManager.translate((float) (-(i * 2 - 1)) * 0.24F, -0.3F, 0.0F);
                GlStateManager.rotate((float) (i * 2 - 1) * 10.0F, 0.0F, 1.0F, 0.0F);
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
                bufferbuilder.pos(-0.5, -0.5, -0.5).tex(f2, f4).endVertex();
                bufferbuilder.pos(0.5, -0.5, -0.5).tex(f1, f4).endVertex();
                bufferbuilder.pos(0.5, 0.5, -0.5).tex(f1, f3).endVertex();
                bufferbuilder.pos(-0.5, 0.5, -0.5).tex(f2, f3).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(515);
            ci.cancel();
        }
    }

    @Shadow
    protected abstract void renderArmFirstPerson(float p_187456_1_, float p_187456_2_, EnumHandSide p_187456_3_);

    @Shadow
    private ItemStack itemStackOffHand;

    @Shadow
    protected abstract void renderMapFirstPersonSide(float p_187465_1_, EnumHandSide hand, float p_187465_3_, ItemStack stack);

    @Shadow
    protected abstract void renderMapFirstPerson(float p_187463_1_, float p_187463_2_, float p_187463_3_);

    @Shadow
    protected abstract void transformSideFirstPerson(EnumHandSide hand, float p_187459_2_);

    @Shadow
    protected abstract void transformEatFirstPerson(float p_187454_1_, EnumHandSide hand, ItemStack stack);

    @Shadow
    protected abstract void transformFirstPerson(EnumHandSide hand, float p_187453_2_);

    @Shadow
    public abstract void renderItemSide(EntityLivingBase entitylivingbaseIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform, boolean leftHanded);

    /**
     * @author SuperSkidder
     * @reason animation
     */
    @Overwrite
    public void renderItemInFirstPerson(AbstractClientPlayer player, float partialTick, float p_187457_3_, EnumHand hand, float swingProgress, ItemStack stack, float equippedProgress) {
        boolean flag = hand == EnumHand.MAIN_HAND;
        EnumHandSide enumhandside = flag ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        GlStateManager.pushMatrix();
        if (stack.isEmpty()) {
            if (flag && !player.isInvisible()) {
                this.renderArmFirstPerson(equippedProgress, swingProgress, enumhandside);
            }
        } else {
            if (OldAnimations.using && OldAnimations.oldRod.getValue() && (stack.getItem() instanceof ItemCarrotOnAStick || stack.getItem() instanceof ItemFishingRod)) {
                GlStateManager.translate(0.08F, -0.027F, -0.33F);
                GlStateManager.scale(0.93F, 1.0F, 1.0F);
            }
            if (OldAnimations.using && OldAnimations.oldSwing.getValue() && swingProgress != 0.0F && !mc.player.isHandActive()) {
                GlStateManager.scale(0.85F, 0.85F, 0.85F);
                GlStateManager.translate(-0.06F, 0.003F, 0.05F);
            }
            if (stack.getItem() instanceof ItemMap) {
                if (flag && this.itemStackOffHand.isEmpty()) {
                    this.renderMapFirstPerson(p_187457_3_, equippedProgress, swingProgress);
                } else {
                    this.renderMapFirstPersonSide(equippedProgress, enumhandside, swingProgress, stack);
                }
            } else {
                boolean flag1 = enumhandside == EnumHandSide.RIGHT;
                float f5;
                float f6;
                if ((player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == hand) || (!PreventBanning.canBlock() && PreventBanning.using && Mouse.isButtonDown(1))) {
                    int j = flag1 ? 1 : -1;
                    switch (stack.getItemUseAction()) {
                        case NONE:
                            this.transformSideFirstPerson(enumhandside, equippedProgress);
                            break;
                        case EAT:
                        case DRINK:
                            if (OldAnimations.using && oldUsing.getValue() && swingProgress != 0.0f) {
                                GlStateManager.translate(j * 0.56, -0.52 + equippedProgress * -0.6, -0.72);
                                GlStateManager.rotate(45.0F, 0, 0, 0);
                                float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                                float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                                GlStateManager.rotate(f * 20.0F, 0, -1, 0);
                                GlStateManager.rotate(f1 * 20.0F, 0, 0, -1);
                                GlStateManager.rotate(f1 * 80.0F, -1, 0, 0);
                                GlStateManager.translate(-0.8f, 0.2f, 0f);
                                GlStateManager.rotate(30f, 0f, 1f, 0f);
                                GlStateManager.rotate(-80f, 1f, 0f, 0f);
                                GlStateManager.rotate(60f, 0f, 1f, 0f);
                                GlStateManager.scale(1.4f, 1.4f, 1.4f);
                                break;
                            }
                            this.transformEatFirstPerson(partialTick, enumhandside, stack);
                            this.transformSideFirstPerson(enumhandside, equippedProgress);
                            if (OldAnimations.using && oldUsing.getValue()) {
                                float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                                float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                                GlStateManager.rotate(f * -20.0F, 0, 1, 0);
                                GlStateManager.rotate(f1 * -20.0F, 0, 0, 1);
                                GlStateManager.rotate(f1 * -80.0F, 1, 0, 0);
                            }
                            break;
                        case BLOCK:
                            if (oldBlock.getValue()) {
                                this.blockAnimation(enumhandside == EnumHandSide.RIGHT ? 1.0F : -1.0F, oldBlock.getValue() ? 0 : equippedProgress, swingProgress);
                            } else {
                                this.transformSideFirstPerson(enumhandside, equippedProgress);
                            }
                            break;
                        case BOW:
                            this.transformSideFirstPerson(enumhandside, equippedProgress);
                            if (oldBow.getValue()) {
                                transformFirstPerson(enumhandside, swingProgress);
                            }
                            GlStateManager.translate((float) j * -0.2785682F, 0.18344387F, 0.15731531F);
                            GlStateManager.rotate(-13.935F, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate((float) j * 35.3F, 0.0F, 1.0F, 0.0F);
                            GlStateManager.rotate((float) j * -9.785F, 0.0F, 0.0F, 1.0F);
                            f5 = (float) stack.getMaxItemUseDuration() - ((float) mc.player.getItemInUseCount() - partialTick + 1.0F);
                            f6 = f5 / 20.0F;
                            f6 = (f6 * f6 + f6 * 2.0F) / 3.0F;
                            if (f6 > 1.0F) {
                                f6 = 1.0F;
                            }

                            if (f6 > 0.1F) {
                                float f7 = MathHelper.sin((f5 - 0.1F) * 1.3F);
                                float f3 = f6 - 0.1F;
                                float f4 = f7 * f3;
                                GlStateManager.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
                            }

                            GlStateManager.translate(f6 * 0.0F, f6 * 0.0F, f6 * 0.04F);
                            GlStateManager.scale(1.0F, 1.0F, 1.0F + f6 * 0.2F);
                            GlStateManager.rotate((float) j * 45.0F, 0.0F, -1.0F, 0.0F);
                    }
                } else {
                    float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                    f5 = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * 6.2831855F);
                    f6 = -0.2F * MathHelper.sin(swingProgress * 3.1415927F);
                    int i = flag1 ? 1 : -1;
                    GlStateManager.translate((float) i * f, f5, f6);
                    this.transformSideFirstPerson(enumhandside, equippedProgress);
                    this.transformFirstPerson(enumhandside, swingProgress);
                }

                this.renderItemSide(player, stack, flag1 ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !flag1);
            }
        }

        GlStateManager.popMatrix();
    }

    private void blockAnimation(float side, float equipProgress, float swingProgress) {
        GlStateManager.translate(x.getValue().floatValue(), y.getValue().floatValue(), z.getValue().floatValue());
        GlStateManager.translate(side * 0.56F, -0.52F + equipProgress * -0.6F, -0.71999997F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(side * (45.0F + f * -20.0F), 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(side * f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(side * -45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.9F, 0.9F, 0.9F);
        GlStateManager.translate(-0.2F, 0.126F, 0.2F);
        GlStateManager.rotate(-102.25F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(side * 15.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(side * 80.0F, 0.0F, 0.0F, 1.0F);
    }
}
