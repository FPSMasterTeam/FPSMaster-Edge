package top.fpsmaster.forge.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Quaternion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.optimizes.WavyCape;
import top.fpsmaster.utils.render.PoseStack;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

@Mixin(LayerCape.class)
public abstract class MixinLayerCape implements LayerRenderer<AbstractClientPlayer> {
    @Shadow
    @Final
    private RenderPlayer playerRenderer;


    @Inject(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At("HEAD"), cancellable = true)
    public void renderLayer(AbstractClientPlayer player, float f, float g, float partialTicks, float h, float i, float j, float scale, CallbackInfo ci) {
        if (FPSMaster.moduleManager.getModule(WavyCape.class).isEnabled()) {
            if (player.isInvisible()) return;

            if (!player.hasPlayerInfo() || player.isInvisible() || !player.isWearing(EnumPlayerModelParts.CAPE) || player.getLocationCape() == null) {
                return;
            }
            this.playerRenderer.bindTexture(player.getLocationCape());
            v1_8_9$renderSmoothCape(player, partialTicks);
            ci.cancel();
        }
    }

    @Unique
    public void v1_8_9$renderSmoothCape(AbstractClientPlayer abstractClientPlayer, float delta) {
        WorldRenderer worldrenderer = Tessellator.getInstance().getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();

        Matrix4f oldPositionMatrix = null;
        for (int part = 0; part < 16; part++) {
            v1_8_9$modifyPoseStack(poseStack, abstractClientPlayer, delta, part);

            if (oldPositionMatrix == null) {
                oldPositionMatrix = poseStack.last().pose;
            }

            if (part == 0) {
                v1_8_9$addTopVertex(worldrenderer, poseStack.last().pose, oldPositionMatrix, part);
            } else if (part == 15) {
                v1_8_9$addBottomVertex(worldrenderer, poseStack.last().pose, poseStack.last().pose, (part + 1) * (0.96F / 16), (part + 1) * (0.96F / 16), part);
            }

            v1_8_9$addLeftVertex(worldrenderer, poseStack.last().pose, oldPositionMatrix, (part + 1) * (0.96F / 16), part * (0.96F / 16), part);
            v1_8_9$addRightVertex(worldrenderer, poseStack.last().pose, oldPositionMatrix, (part + 1) * (0.96F / 16), part * (0.96F / 16), part);
            v1_8_9$addBackVertex(worldrenderer, poseStack.last().pose, oldPositionMatrix, (part + 1) * (0.96F / 16), part * (0.96F / 16), part);
            v1_8_9$addFrontVertex(worldrenderer, oldPositionMatrix, poseStack.last().pose, (part + 1) * (0.96F / 16), part * (0.96F / 16), part);
            oldPositionMatrix = poseStack.last().pose;
            poseStack.popPose();
        }
        Tessellator.getInstance().draw();
    }

    @Unique
    private void v1_8_9$modifyPoseStack(PoseStack poseStack, AbstractClientPlayer abstractClientPlayer, float h, int part) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.125D);
        double d = v1_8_9$lerp(h, abstractClientPlayer.prevChasingPosX, abstractClientPlayer.chasingPosX)
                - v1_8_9$lerp(h, abstractClientPlayer.prevPosX, abstractClientPlayer.posX);
        double e = v1_8_9$lerp(h, abstractClientPlayer.prevChasingPosY, abstractClientPlayer.chasingPosY)
                - v1_8_9$lerp(h, abstractClientPlayer.prevPosY, abstractClientPlayer.posY);
        double m = v1_8_9$lerp(h, abstractClientPlayer.prevChasingPosZ, abstractClientPlayer.chasingPosZ)
                - v1_8_9$lerp(h, abstractClientPlayer.prevPosZ, abstractClientPlayer.posZ);
        float n = abstractClientPlayer.prevRenderYawOffset + abstractClientPlayer.renderYawOffset - abstractClientPlayer.prevRenderYawOffset;
        double o = Math.sin(n * 0.017453292F);
        double p = -Math.cos(n * 0.017453292F);
        float height = (float) e * 10.0F;
        height = MathHelper.clamp_float(height, -6.0F, 32.0F);
        float swing = (float) (d * o + m * p) * v1_8_9$easeOutSine(1.0F / 16 * part) * 100;
        swing = MathHelper.clamp_float(swing, 0.0F, 150.0F * v1_8_9$easeOutSine(1F / 16 * part));
        float sidewaysRotationOffset = (float) (d * p - m * o) * 100.0F;
        sidewaysRotationOffset = MathHelper.clamp_float(sidewaysRotationOffset, -20.0F, 20.0F);
        float t = v1_8_9$lerp(h, abstractClientPlayer.prevCameraYaw, abstractClientPlayer.cameraYaw);
        height += (float) (Math.sin(v1_8_9$lerp(h, abstractClientPlayer.prevDistanceWalkedModified, abstractClientPlayer.distanceWalkedModified) * 6.0F) * 32.0F * t);
        if (abstractClientPlayer.isSneaking()) {
            height += 25.0F;
            poseStack.translate(0, 0.15F, 0);
        }

        poseStack.mulPose(v1_8_9$fromDegree(1.0F, 0.0F, 0.0F, 6.0F + swing / 2.0F + height));
        poseStack.mulPose(v1_8_9$fromDegree(0.0F, 0.0F, 1.0F, sidewaysRotationOffset / 2.0F));
        poseStack.mulPose(v1_8_9$fromDegree(0.0F, 1.0F, 0.0F, 180.0F - sidewaysRotationOffset / 2.0F));
    }

    @Unique
    private float v1_8_9$easeOutSine(float x) {
        return (float) Math.sin((x * Math.PI) / 2);
    }

    @Unique
    private Quaternion v1_8_9$fromDegree(float x, float y, float z, float degree) {
        Quaternion quaternion = new Quaternion();
        degree *= 0.017453292F;
        float g = (float) Math.sin(degree / 2.0F);
        quaternion.x = x * g;
        quaternion.y = y * g;
        quaternion.z = z * g;
        quaternion.w = (float) Math.cos(degree / 2.0F);
        return quaternion;
    }

    @Unique
    private float v1_8_9$lerp(float f, float g, float h) {
        return g + f * (h - g);
    }

    @Unique
    private double v1_8_9$lerp(double d, double e, double f) {
        return e + d * (f - e);
    }

    @Unique
    private void v1_8_9$addBackVertex(WorldRenderer worldrenderer, Matrix4f matrix, Matrix4f oldMatrix, float y1, float y2, int part) {
        float i;
        Matrix4f k;
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;

            k = matrix;
            matrix = oldMatrix;
            oldMatrix = k;
        }

        float minU = .015625F;
        float maxU = .171875F;

        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / 16;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        v1_8_9$vertex(worldrenderer, oldMatrix, (float) 0.3, y2, (float) -0.06).tex(maxU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, oldMatrix, (float) -0.3, y2, (float) -0.06).tex(minU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) -0.3, y1, (float) -0.06).tex(minU, maxV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) 0.3, y1, (float) -0.06).tex(maxU, maxV).normal(1, 0, 0).endVertex();

    }

    @Unique
    private void v1_8_9$addFrontVertex(WorldRenderer worldrenderer, Matrix4f matrix, Matrix4f oldMatrix, float y1, float y2, int part) {
        float i;
        Matrix4f k;

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;

            k = matrix;
            matrix = oldMatrix;
            oldMatrix = k;
        }

        float minU = .1875F;
        float maxU = .34375F;

        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / 16;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        v1_8_9$vertex(worldrenderer, oldMatrix, (float) 0.3, y1, (float) 0.0).tex(maxU, maxV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, oldMatrix, (float) -0.3, y1, (float) 0.0).tex(minU, maxV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) -0.3, y2, (float) 0.0).tex(minU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) 0.3, y2, (float) 0.0).tex(maxU, minV).normal(1, 0, 0).endVertex();

    }

    @Unique
    private void v1_8_9$addLeftVertex(WorldRenderer worldrenderer, Matrix4f matrix, Matrix4f oldMatrix, float y1, float y2, int part) {
        float i;
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = 0;
        float maxU = .015625F;

        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / 16;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        v1_8_9$vertex(worldrenderer, oldMatrix, (float) -0.3, y2, (float) -0.06).tex(minU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, oldMatrix, (float) -0.3, y2, (float) 0.0).tex(maxU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) -0.3, y1, (float) 0.0).tex(maxU, maxV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) -0.3, y1, (float) -0.06).tex(minU, maxV).normal(1, 0, 0).endVertex();

    }

    @Unique
    private void v1_8_9$addRightVertex(WorldRenderer worldrenderer, Matrix4f matrix, Matrix4f oldMatrix, float y1, float y2, int part) {
        float i;

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = .171875F;
        float maxU = .1875F;

        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / 16;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        v1_8_9$vertex(worldrenderer, oldMatrix, (float) 0.3, y2, (float) 0.0).tex(maxU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, oldMatrix, (float) 0.3, y2, (float) -0.06).tex(minU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) 0.3, y1, (float) -0.06).tex(minU, maxV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) 0.3, y1, (float) 0.0).tex(maxU, maxV).normal(1, 0, 0).endVertex();

    }

    @Unique
    private void v1_8_9$addBottomVertex(WorldRenderer worldrenderer, Matrix4f matrix, Matrix4f oldMatrix, float y1, float y2, int part) {
        float i;
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = .171875F;
        float maxU = .328125F;

        float minV = 0;
        float maxV = .03125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / 16;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        v1_8_9$vertex(worldrenderer, oldMatrix, (float) 0.3, y2, (float) -0.06).tex(maxU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, oldMatrix, (float) -0.3, y2, (float) -0.06).tex(minU, minV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) -0.3, y1, (float) 0.0).tex(minU, maxV).normal(1, 0, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) 0.3, y1, (float) 0.0).tex(maxU, maxV).normal(1, 0, 0).endVertex();

    }

    @Unique
    private WorldRenderer v1_8_9$vertex(WorldRenderer worldrenderer, Matrix4f matrix4f, float f, float g, float h) {
        Vector4f vector4f = new Vector4f(f, g, h, 1.0F);
        matrix4f.transform(vector4f);
        worldrenderer.pos(vector4f.x, vector4f.y, vector4f.z);
        return worldrenderer;
    }

    @Unique
    private void v1_8_9$addTopVertex(WorldRenderer worldrenderer, Matrix4f matrix, Matrix4f oldMatrix, int part) {
        float minU = .015625F;
        float maxU = .171875F;

        float minV = 0;
        float maxV = .03125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / 16;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        v1_8_9$vertex(worldrenderer, oldMatrix, (float) 0.3, (float) 0, (float) 0.0).tex(maxU, maxV).normal(0, 1, 0).endVertex();
        v1_8_9$vertex(worldrenderer, oldMatrix, (float) -0.3, (float) 0, (float) 0.0).tex(minU, maxV).normal(0, 1, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) -0.3, (float) 0, (float) -0.06).tex(minU, minV).normal(0, 1, 0).endVertex();
        v1_8_9$vertex(worldrenderer, matrix, (float) 0.3, (float) 0, (float) -0.06).tex(maxU, minV).normal(0, 1, 0).endVertex();
    }
}
