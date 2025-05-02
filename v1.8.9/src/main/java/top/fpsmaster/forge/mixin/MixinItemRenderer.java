package top.fpsmaster.forge.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventAnimation;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.render.FireModifier;

import java.awt.*;

import static top.fpsmaster.utils.Utility.mc;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {


    @Shadow
    private ItemStack itemToRender;
    @Shadow
    private float equippedProgress;
    @Shadow
    private float prevEquippedProgress;

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Shadow
    protected abstract void rotateArroundXAndY(float angle, float angleY);

    @Shadow
    protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks);

    @Shadow
    protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

    @Shadow
    protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

    @Shadow
    protected abstract void doBlockTransformations();

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    public void renderFireInFirstPerson(CallbackInfo ci) {
        if (FireModifier.using) {
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
            GlStateManager.depthFunc(519);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            float f = 1.0F;

            for (int i = 0; i < 2; ++i) {
                GlStateManager.pushMatrix();
                TextureAtlasSprite textureatlassprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
                mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
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


    private void drawBlocking(float swingProgress, float equippedProgress) {
        GL11.glTranslated(OldAnimations.x.getValue().floatValue(), OldAnimations.y.getValue().floatValue(), OldAnimations.z.getValue().floatValue());
//        GL11.glScaled(OldAnimations.scale.getValue().floatValue(), OldAnimations.scale.getValue().floatValue(), 0);
        if (OldAnimations.animationMode.isMode("Sigma")) {
            this.transformFirstPersonItem(equippedProgress, 0.0f);
            float swong = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
            GlStateManager.rotate(-swong * 55 / 2.0F, -8.0F, -0.0F, 9.0F);
            GlStateManager.rotate(-swong * 45, 1.0F, swong / 2, -0.0F);
            this.doBlockTransformations();
            GL11.glTranslated(1.2, 0.3, 0.5);
            GL11.glTranslatef(-1, mc.thePlayer.isSneaking() ? -0.1F : -0.2F, 0.2F);
        } else if (OldAnimations.animationMode.isMode("Debug")) {
            this.transformFirstPersonItem(0.2f, equippedProgress);
            this.doBlockTransformations();
            GlStateManager.translate(-0.5, 0.2, 0.0);
        } else if (OldAnimations.animationMode.isMode("Luna")) {
            this.transformFirstPersonItem(swingProgress, 0.0F);
            this.doBlockTransformations();
            final float sin2 = MathHelper.sin((float) (MathHelper.sqrt_float(equippedProgress) * Math.PI));
            GlStateManager.scale(1.0f, 1.0f, 1.0f);
            GlStateManager.translate(-0.2f, 0.45f, 0.25f);
            GlStateManager.rotate(-sin2 * 20.0f, -5.0f, -5.0f, 9.0f);
        } else if (OldAnimations.animationMode.isMode("1.7")) {
            this.transformFirstPersonItem(swingProgress - 0.3F, equippedProgress);
            this.doBlockTransformations();
        } else if (OldAnimations.animationMode.isMode("Swang")) {
            this.transformFirstPersonItem(swingProgress / 2.0F, equippedProgress);
            float var15;
            var15 = MathHelper.sin((float) (MathHelper.sqrt_float(equippedProgress) * Math.PI));
            GlStateManager.rotate(var15 * 30.0F / 2.0F, -var15, -0.0F, 9.0F);
            GlStateManager.rotate(var15 * 40.0F, 1.0F, -var15 / 2.0F, -0.0F);

            this.doBlockTransformations();
        } else if (OldAnimations.animationMode.isMode("Swank")) {
            this.transformFirstPersonItem(swingProgress / 2.0F, equippedProgress);
            float var15;
            var15 = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
            GlStateManager.rotate(var15 * 30.0F, -var15, -0.0F, 9.0F);
            GlStateManager.rotate(var15 * 40.0F, 1.0F, -var15, -0.0F);

            this.doBlockTransformations();
        } else if (OldAnimations.animationMode.isMode("Swong")) {
            this.transformFirstPersonItem(swingProgress / 2.0F, 0.0F);
            float var151 = MathHelper.sin((float) (MathHelper.sqrt_float(equippedProgress) * Math.PI));
            GlStateManager.rotate(-var151 * 40.0F / 2.0F, var151 / 2.0F, -0.0F, 9.0F);
            GlStateManager.rotate(-var151 * 30.0F, 1.0F, var151 / 2.0F, -0.0F);

            this.doBlockTransformations();
        } else if (OldAnimations.animationMode.isMode("Jigsaw")) {
            this.transformFirstPersonItem(0.1f, equippedProgress);
            this.doBlockTransformations();
            GlStateManager.translate(-0.5, 0, 0);
        } else if (OldAnimations.animationMode.isMode("Jello")) {
            GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
            GlStateManager.translate(0.0F, 0 * -0.6F, 0.0F);
            GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
            float var3 = MathHelper.sin((float) (0.0F * 0.0F * Math.PI));
            float var4 = MathHelper.sin((float) (MathHelper.sqrt_float(0.0F) * Math.PI));
            GlStateManager.rotate(var3 * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(var4 * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(var4 * -80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(0.4F, 0.4F, 0.4F);

            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
            GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
            int alpha = (int) Math.min(255,
                    ((System.currentTimeMillis() % 255) > 255 / 2
                            ? (Math.abs(Math.abs(System.currentTimeMillis()) % 255 - 255))
                            : System.currentTimeMillis() % 255) * 2);
            GlStateManager.translate(0.3f, -0.0f, 0.40f);
            GlStateManager.rotate(0.0f, 0.0f, 0.0f, 1.0f);
            GlStateManager.translate(0, 0.5f, 0);

            GlStateManager.rotate(90, 1.0f, 0.0f, -1.0f);
            GlStateManager.translate(0.6f, 0.5f, 0);
            GlStateManager.rotate(-90, 1.0f, 0.0f, -1.0f);

            GlStateManager.rotate(-10, 1.0f, 0.0f, -1.0f);
            GlStateManager.rotate(mc.thePlayer.isSwingInProgress ? -alpha / 5f : 1, 1.0f, -0.0f, 1.0f);
        } else if (OldAnimations.animationMode.isMode("Push")) {
            this.transformFirstPersonItem(swingProgress, 0.0F);
            this.doBlockTransformations();
            GlStateManager.rotate(-MathHelper.sin((float) (MathHelper.sqrt_float(equippedProgress) * Math.PI)) * 35.0F, -8.0F, -0.0F, 9.0F);
            GlStateManager.rotate(-MathHelper.sin((float) (MathHelper.sqrt_float(equippedProgress) * Math.PI)) * 10.0F, 1.0F, -0.4F, -0.5F);
        }else{
            this.transformFirstPersonItem(swingProgress - 0.3F, equippedProgress);
            this.doBlockTransformations();
        }
    }


    /**
     * @author SuperSkidder
     * @reason animation
     */
    @Overwrite
    public void renderItemInFirstPerson(float partialTicks) {
        float f = 1.0F - (this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTicks);
        AbstractClientPlayer abstractclientplayer = mc.thePlayer;
        float f1 = abstractclientplayer.getSwingProgress(partialTicks);
        float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
        float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
        this.rotateArroundXAndY(f2, f3);
        this.setLightMapFromPlayer(abstractclientplayer);
        this.rotateWithPlayerRotations((EntityPlayerSP) abstractclientplayer, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        if (this.itemToRender != null) {
            if (OldAnimations.oldRod.getValue() && itemToRender.getItem() instanceof ItemCarrotOnAStick) {
                GlStateManager.translate(0.08F, -0.027F, -0.33F);
                GlStateManager.scale(0.93F, 1.0F, 1.0F);
            }
            if (OldAnimations.oldRod.getValue() && itemToRender.getItem() instanceof ItemFishingRod) {
                GlStateManager.translate(0.08F, -0.027F, -0.33F);
                GlStateManager.scale(0.93F, 1.0F, 1.0F);
            }
            if (OldAnimations.oldSwing.getValue() && f1 != 0.0F && !mc.thePlayer.isBlocking() && !mc.thePlayer.isEating() && !mc.thePlayer.isUsingItem()) {
                GlStateManager.scale(0.85F, 0.85F, 0.85F);
                GlStateManager.translate(-0.06F, 0.003F, 0.05F);
            }
            if (this.itemToRender.getItem() instanceof ItemMap) {
                this.renderItemMap(abstractclientplayer, f2, f, f1);
            } else if (abstractclientplayer.getItemInUseCount() > 0) {
                EnumAction enumaction = this.itemToRender.getItemUseAction();
                switch (enumaction) {
                    case NONE:
                        EventAnimation none = new EventAnimation(EventAnimation.Type.NONE, f, f1);
                        EventDispatcher.dispatchEvent(none);
                        if (!none.isCanceled())
                            this.transformFirstPersonItem(f, 0.0F);
                        break;
                    case EAT:
                    case DRINK:
                        EventAnimation use = new EventAnimation(EventAnimation.Type.USE, f, f1);
                        EventDispatcher.dispatchEvent(use);
                        if (!use.isCanceled()) {
                            this.performDrinking(mc.thePlayer, partialTicks);
                            this.transformFirstPersonItem(f, f1);
                        }
                        break;

                    case BLOCK:
                        EventAnimation block = new EventAnimation(EventAnimation.Type.USE, f, f1);
                        EventDispatcher.dispatchEvent(block);
                        if (!block.isCanceled()) {
                            if (OldAnimations.oldBlock.getValue()) {
                                this.drawBlocking(f, f1);
                            } else {
                                this.transformFirstPersonItem(f, 0.0F);
                                this.doBlockTransformations();
                            }
                        }
                        break;

                    case BOW:
                        EventAnimation bow = new EventAnimation(EventAnimation.Type.USE, f, f1);
                        EventDispatcher.dispatchEvent(bow);
                        if (!bow.isCanceled()) {
                            if (OldAnimations.oldBow.getValue()) {
                                this.transformFirstPersonItem(f, f1);
                            } else {
                                this.transformFirstPersonItem(f, 0.0F);
                            }
                            this.doBowTransformations(partialTicks, mc.thePlayer);
                        }
                }
            } else {
                this.doItemUsedTransformations(f1);
                this.transformFirstPersonItem(f, f1);
            }

            this.renderItem(abstractclientplayer, this.itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        } else if (!abstractclientplayer.isInvisible()) {
            this.renderPlayerArm(abstractclientplayer, f, f1);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }
}
