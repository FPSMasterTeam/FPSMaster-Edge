package top.fpsmaster.forge.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
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

    @Unique
    private static final int SEGMENTS = 18;
    @Unique
    private static final float CAPE_WIDTH = 0.6F;
    @Unique
    private static final float CAPE_LENGTH = 1.08F;
    @Unique
    private static final float CAPE_DEPTH = 0.06F;

    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    public void onRenderCape(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks,
                             float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (!FPSMaster.moduleManager.getModule(WavyCape.class).isEnabled()) return;
        if (shouldSkipRender(player)) return;

        playerRenderer.bindTexture(player.getLocationCape());
        renderWavyCape(player, partialTicks);
        ci.cancel();
    }

    @Unique
    private boolean shouldSkipRender(AbstractClientPlayer player) {
        return player.isInvisible() ||
                !player.hasPlayerInfo() ||
                !player.isWearing(EnumPlayerModelParts.CAPE) ||
                player.getLocationCape() == null;
    }

    @Unique
    private void renderWavyCape(AbstractClientPlayer player, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        PoseStack poseStack = new PoseStack();

        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
        poseStack.pushPose();

        Matrix4f prevMatrix = null;
        final float segmentLength = CAPE_LENGTH / SEGMENTS;

        for (int segment = 0; segment < SEGMENTS; segment++) {
            poseStack.pushPose();
            applySegmentTransform(poseStack, player, partialTicks, segment);

            Matrix4f currentMatrix = poseStack.last().pose;
            float yOffsetTop = (segment - 1) * segmentLength;
            float yOffsetBottom = (segment) * segmentLength;


            if (prevMatrix != null) {
                renderCapeSegment(buffer, prevMatrix, currentMatrix, yOffsetTop, yOffsetBottom, segment);
            }
            if (segment == 0) {
                renderTopSegment(buffer, currentMatrix);
            }
            prevMatrix = currentMatrix;
            poseStack.popPose();
        }

        poseStack.popPose();
        tessellator.draw();
    }

    @Unique
    private void applySegmentTransform(PoseStack poseStack, AbstractClientPlayer player, float partialTicks, int segment) {
        poseStack.translate(0.0, 0.0, 0.175);

        // 计算玩家运动差值
        double motionX = interpolate(partialTicks, player.prevChasingPosX, player.chasingPosX) -
                interpolate(partialTicks, player.prevPosX, player.posX);
        double motionY = interpolate(partialTicks, player.prevChasingPosY, player.chasingPosY) -
                interpolate(partialTicks, player.prevPosY, player.posY);
        double motionZ = interpolate(partialTicks, player.prevChasingPosZ, player.chasingPosZ) -
                interpolate(partialTicks, player.prevPosZ, player.posZ);

        // 计算旋转角度
        float yawOffset = interpolate(partialTicks, player.prevRenderYawOffset, player.renderYawOffset);
        double sinYaw = Math.sin(yawOffset * Math.PI / 180.0);
        double cosYaw = -Math.cos(yawOffset * Math.PI / 180.0);

        // 计算高度偏移
        float heightOffset = (float) motionY * 10.0F;
        heightOffset = MathHelper.clamp_float(heightOffset, -6.0F, 32.0F);

        // 计算摆动幅度
        float swingFactor = (float) (motionX * sinYaw + motionZ * cosYaw) *
                easeOutSine((float) segment / SEGMENTS) * 100;
        swingFactor = MathHelper.clamp_float(swingFactor, 0.0F, 150.0F * easeOutSine((float) segment / SEGMENTS));

        // 计算侧向旋转
        float sidewaysRotation = (float) (motionX * cosYaw - motionZ * sinYaw) * 100.0F;
        sidewaysRotation = MathHelper.clamp_float(sidewaysRotation, -20.0F, 20.0F);

        // 添加行走动画效果
        float walkAnimation = interpolate(partialTicks, player.prevDistanceWalkedModified, player.distanceWalkedModified);
        heightOffset += MathHelper.sin(walkAnimation * 6.0F) * 32.0F *
                interpolate(partialTicks, player.prevCameraYaw, player.cameraYaw);

        // 蹲下调整
        if (player.isSneaking()) {
            heightOffset += 25.0F;
            poseStack.translate(0, 0.15F, 0);
        }

        // 应用风摆效果
        float windSwing = calculateWindSwing(segment);

        // 应用旋转
        poseStack.mulPose(createQuaternion(1.0F, 0.0F, 0.0F, 6.0F + swingFactor / 2.0F + heightOffset + windSwing));
        poseStack.mulPose(createQuaternion(0.0F, 0.0F, 1.0F, sidewaysRotation / 2.0F));
        poseStack.mulPose(createQuaternion(0.0F, 1.0F, 0.0F, 180.0F - sidewaysRotation / 2.0F));
    }

    @Unique
    private float calculateWindSwing(int segment) {
        long time = System.currentTimeMillis() / 3;
        float phase = (float) (segment + 1) / SEGMENTS;
        return (float) Math.sin(Math.toRadians(phase * 360 - (time % 360))) * 3;
    }

    @Unique
    private void renderCapeSegment(WorldRenderer buffer, Matrix4f prevMatrix, Matrix4f currentMatrix,
                                   float yTop, float yBottom, int segment) {
        renderBackFace(buffer, prevMatrix, currentMatrix, yTop, yBottom, segment);
        renderFrontFace(buffer, prevMatrix, currentMatrix, yTop, yBottom, segment);
        renderLeftSide(buffer, prevMatrix, currentMatrix, yTop, yBottom, segment);
        renderRightSide(buffer, prevMatrix, currentMatrix, yTop, yBottom, segment);

        if (segment == SEGMENTS - 1) {
            renderBottomEdge(buffer, prevMatrix, currentMatrix, yTop, yBottom);
        }
    }

    @Unique
    private void renderBackFace(WorldRenderer buffer, Matrix4f prevMatrix, Matrix4f currentMatrix,
                                float yTop, float yBottom, int segment) {
        float minU = 0.015625F;
        float maxU = 0.171875F;
        float[] texCoords = getVerticalTexCoords(segment, 0.03125F, 0.53125F);

        addVertex(buffer, prevMatrix, -CAPE_WIDTH/2, yTop, -CAPE_DEPTH, minU, texCoords[0]);
        addVertex(buffer, prevMatrix, CAPE_WIDTH/2, yTop, -CAPE_DEPTH, maxU, texCoords[0]);
        addVertex(buffer, currentMatrix, CAPE_WIDTH/2, yBottom, -CAPE_DEPTH, maxU, texCoords[1]);
        addVertex(buffer, currentMatrix, -CAPE_WIDTH/2, yBottom, -CAPE_DEPTH, minU, texCoords[1]);
    }

    @Unique
    private void renderFrontFace(WorldRenderer buffer, Matrix4f prevMatrix, Matrix4f currentMatrix,
                                 float yTop, float yBottom, int segment) {
        float minU = 0.1875F;
        float maxU = 0.34375F;
        float[] texCoords = getVerticalTexCoords(segment, 0.03125F, 0.53125F);

        addVertex(buffer, prevMatrix, -CAPE_WIDTH/2, yBottom, 0, minU, texCoords[1]);
        addVertex(buffer, prevMatrix, CAPE_WIDTH/2, yBottom, 0, maxU, texCoords[1]);
        addVertex(buffer, currentMatrix, CAPE_WIDTH/2, yTop, 0, maxU, texCoords[0]);
        addVertex(buffer, currentMatrix, -CAPE_WIDTH/2, yTop, 0, minU, texCoords[0]);
    }

    @Unique
    private void renderLeftSide(WorldRenderer buffer, Matrix4f prevMatrix, Matrix4f currentMatrix,
                                float yTop, float yBottom, int segment) {
        float minU = 0.0F;
        float maxU = 0.015625F;
        float[] texCoords = getVerticalTexCoords(segment, 0.03125F, 0.53125F);

        addVertex(buffer, prevMatrix, -CAPE_WIDTH/2, yTop, 0, maxU, texCoords[0]);
        addVertex(buffer, prevMatrix, -CAPE_WIDTH/2, yTop, -CAPE_DEPTH, minU, texCoords[0]);
        addVertex(buffer, currentMatrix, -CAPE_WIDTH/2, yBottom, -CAPE_DEPTH, minU, texCoords[1]);
        addVertex(buffer, currentMatrix, -CAPE_WIDTH/2, yBottom, 0, maxU, texCoords[1]);
    }

    @Unique
    private void renderRightSide(WorldRenderer buffer, Matrix4f prevMatrix, Matrix4f currentMatrix,
                                 float yTop, float yBottom, int segment) {
        float minU = 0.171875F;
        float maxU = 0.1875F;
        float[] texCoords = getVerticalTexCoords(segment, 0.03125F, 0.53125F);

        addVertex(buffer, prevMatrix, CAPE_WIDTH/2, yTop, -CAPE_DEPTH, minU, texCoords[0]);
        addVertex(buffer, prevMatrix, CAPE_WIDTH/2, yTop, 0, maxU, texCoords[0]);
        addVertex(buffer, currentMatrix, CAPE_WIDTH/2, yBottom, 0, maxU, texCoords[1]);
        addVertex(buffer, currentMatrix, CAPE_WIDTH/2, yBottom, -CAPE_DEPTH, minU, texCoords[1]);
    }

    @Unique
    private void renderTopSegment(WorldRenderer buffer, Matrix4f matrix) {
        float minU = 0.015625F;
        float maxU = 0.171875F;
        float minV = 0.0F;
        float maxV = 0.03125F;

        addVertex(buffer, matrix, -CAPE_WIDTH/2, 0, 0, minU, maxV);
        addVertex(buffer, matrix, CAPE_WIDTH/2, 0, 0, maxU, maxV);
        addVertex(buffer, matrix, CAPE_WIDTH/2, 0, -CAPE_DEPTH, maxU, minV);
        addVertex(buffer, matrix, -CAPE_WIDTH/2, 0, -CAPE_DEPTH, minU, minV);
    }

    @Unique
    private void renderBottomEdge(WorldRenderer buffer, Matrix4f prevMatrix, Matrix4f currentMatrix,
                                  float yTop, float yBottom) {
        float minU = 0.171875F;
        float maxU = 0.328125F;
        float minV = 0.0F;
        float maxV = 0.03125F;

        addVertex(buffer, prevMatrix, -CAPE_WIDTH/2, yBottom, -CAPE_DEPTH, minU, minV);
        addVertex(buffer, prevMatrix, CAPE_WIDTH/2, yBottom, -CAPE_DEPTH, maxU, minV);
        addVertex(buffer, currentMatrix, CAPE_WIDTH/2, yTop, 0, maxU, maxV);
        addVertex(buffer, currentMatrix, -CAPE_WIDTH/2, yTop, 0, minU, maxV);
    }

    @Unique
    private float[] getVerticalTexCoords(int segment, float minV, float maxV) {
        float vRange = maxV - minV;
        float vStep = vRange / SEGMENTS;
        return new float[] {
                minV + segment * vStep,
                minV + (segment + 1) * vStep
        };
    }

    @Unique
    private void addVertex(WorldRenderer buffer, Matrix4f matrix, float x, float y, float z, float u, float v) {
        Vector4f pos = new Vector4f(x, y, z, 1.0F);
        matrix.transform(pos);
        buffer.pos(pos.x, pos.y, pos.z)
                .tex(u, v)
                .normal(0, 1, 0) // 实际法线应根据面调整，此处简化为(0,1,0)
                .endVertex();
    }

    @Unique
    private float interpolate(float delta, float prev, float current) {
        return prev + delta * (current - prev);
    }

    @Unique
    private double interpolate(double delta, double prev, double current) {
        return prev + delta * (current - prev);
    }

    @Unique
    private float easeOutSine(float progress) {
        return (float) Math.sin((progress * Math.PI) / 2);
    }

    @Unique
    private Quaternion createQuaternion(float axisX, float axisY, float axisZ, float degrees) {
        Quaternion q = new Quaternion();
        float radians = degrees * (float) Math.PI / 180;
        float sinHalf = (float) Math.sin(radians / 2);
        q.x = axisX * sinHalf;
        q.y = axisY * sinHalf;
        q.z = axisZ * sinHalf;
        q.w = (float) Math.cos(radians / 2);
        return q;
    }
}