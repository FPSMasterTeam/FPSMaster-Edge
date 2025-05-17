package top.fpsmaster.forge.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.*;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.impl.optimizes.NoHurtCam;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.SmoothZoom;
import top.fpsmaster.features.impl.render.FreeLook;
import top.fpsmaster.features.impl.render.MinimizedBobbing;
import top.fpsmaster.utils.math.MathUtils;

import static top.fpsmaster.utils.Utility.mc;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    private float screenScale = -1;

    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE))
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo callbackInfo) {
        EventDispatcher.dispatchEvent(new EventRender3D(partialTicks));
    }

    @Redirect(method = "setupCameraTransform", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;viewBobbing:Z"))
    public boolean bobbing(GameSettings instance) {
        return mc.gameSettings.viewBobbing && !MinimizedBobbing.using;
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    public void hurtCameraEffect(float amount, CallbackInfo ci) {
        if (NoHurtCam.using)
            ci.cancel();
    }

    @Shadow
    private boolean debugView = false;

    @Shadow
    private float fovModifierHand;
    @Shadow
    private float fovModifierHandPrev;

    @Inject(method = "getFOVModifier", at = @At("HEAD"), cancellable = true)
    private void getFOVModifier(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        if (SmoothZoom.using) {
            if (this.debugView) {
                cir.setReturnValue(90.0F);
            } else {
                Entity entity = mc.getRenderViewEntity();
                float f = 70.0F;
                if (useFOVSetting) {
                    f = mc.gameSettings.fovSetting;
                    f *= this.fovModifierHandPrev + (this.fovModifierHand - this.fovModifierHandPrev) * partialTicks;
                }

                if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F) {
                    float f1 = (float) ((EntityLivingBase) entity).deathTime + partialTicks;
                    f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
                }

                Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);
                if (block.getMaterial() == Material.water) {
                    f = f * 60.0F / 70.0F;
                }

                if (screenScale == -1)
                    screenScale = f;

                if (SmoothZoom.using && SmoothZoom.zoom) {
                    if (SmoothZoom.smoothCamera.getValue()) {
                        screenScale = MathUtils.decreasedSpeed(screenScale, f, f / 4.0F, SmoothZoom.speed.getValue().floatValue() / (float) Minecraft.getDebugFPS() * 150.0f);
                    } else {
                        screenScale = f / 4.0F;
                    }
                } else {
                    screenScale = f;
                }

                cir.setReturnValue(ForgeHooksClient.getFOVModifier((EntityRenderer) (Object) this, entity, block, partialTicks, screenScale));
            }
        }
    }

    @Shadow
    private long prevFrameTime = Minecraft.getSystemTime();

    @Shadow
    private float smoothCamYaw;
    @Shadow
    private float smoothCamPitch;
    @Shadow
    private float smoothCamFilterX;
    @Shadow
    private float smoothCamFilterY;
    @Shadow
    private float smoothCamPartialTicks;
    @Shadow
    public static boolean anaglyphEnable;
    @Shadow
    private long renderEndNanoTime;
    @Shadow
    private boolean useShader;

    @Shadow
    public abstract void renderWorld(float partialTicks, long finishTimeNano);

    @Shadow
    private ShaderGroup theShaderGroup;

    @Shadow
    public abstract void setupOverlayRendering();


    @Shadow
    private boolean cloudFog;
    @Shadow
    private float thirdPersonDistance = 4.0F;
    @Shadow
    private float thirdPersonDistanceTemp = 4.0F;
    @Unique
    private float partialTicks;


    @Redirect(method = "renderWorldDirections", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"))
    public float modifyEyeHeight_renderWorldDirections(Entity entity) {
        if (mc.getRenderViewEntity() != mc.thePlayer || OldAnimations.using) return entity.getEyeHeight();
        return OldAnimations.getClientEyeHeight(partialTicks);
    }

    /**
     * @author SuperSkidder
     * @reason Free Look
     */
    @Overwrite
    private void orientCamera(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();

        this.partialTicks = partialTicks;
        float f = entity.getEyeHeight();
        if (mc.getRenderViewEntity() == mc.thePlayer && OldAnimations.using){
            f = OldAnimations.getClientEyeHeight(partialTicks);
        }
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
        float f1;
        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
            f = (float) ((double) f + 1.0);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);
            if (!mc.gameSettings.debugCamEnable) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = mc.theWorld.getBlockState(blockpos);
                ForgeHooksClient.orientBedCamera(mc.theWorld, blockpos, iblockstate, entity);
                if (FreeLook.using) {
                    GlStateManager.rotate(FreeLook.getCameraPrevYaw() + (FreeLook.getCameraYaw() - FreeLook.getCameraPrevYaw()) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(FreeLook.getCameraPrevPitch() + (FreeLook.getCameraPitch() - FreeLook.getCameraPrevPitch()) * partialTicks, -1.0F, 0.0F, 0.0F);
                } else {
                    GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
                }
            }
        } else if (mc.gameSettings.thirdPersonView > 0) {
            double d3 = this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks;
            if (mc.gameSettings.debugCamEnable) {
                GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
            } else {
                f1 = entity.rotationYaw;
                float f2 = entity.rotationPitch;
                if (mc.gameSettings.thirdPersonView == 2) {
                    f2 += 180.0F;
                }

                double d4 = (double) (-MathHelper.sin(f1 / 180.0F * 3.1415927F) * MathHelper.cos(f2 / 180.0F * 3.1415927F)) * d3;
                double d5 = (double) (MathHelper.cos(f1 / 180.0F * 3.1415927F) * MathHelper.cos(f2 / 180.0F * 3.1415927F)) * d3;
                double d6 = (double) (-MathHelper.sin(f2 / 180.0F * 3.1415927F)) * d3;

                for (int i = 0; i < 8; ++i) {
                    float f3 = (float) ((i & 1) * 2 - 1);
                    float f4 = (float) ((i >> 1 & 1) * 2 - 1);
                    float f5 = (float) ((i >> 2 & 1) * 2 - 1);
                    f3 *= 0.1F;
                    f4 *= 0.1F;
                    f5 *= 0.1F;
                    MovingObjectPosition movingobjectposition = mc.theWorld.rayTraceBlocks(new Vec3(d0 + (double) f3, d1 + (double) f4, d2 + (double) f5), new Vec3(d0 - d4 + (double) f3 + (double) f5, d1 - d6 + (double) f4, d2 - d5 + (double) f5));
                    if (movingobjectposition != null) {
                        double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));
                        if (d7 < d3) {
                            d3 = d7;
                        }
                    }
                }

                if (mc.gameSettings.thirdPersonView == 2) {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
                GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        } else {
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
        }

        if (!mc.gameSettings.debugCamEnable) {
            float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
            float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;

            if (FreeLook.using) {
                yaw = FreeLook.getCameraPrevYaw() + (FreeLook.getCameraYaw() - FreeLook.getCameraPrevYaw()) * partialTicks + 180.0F;
                pitch = FreeLook.getCameraPrevPitch() + (FreeLook.getCameraPitch() - FreeLook.getCameraPrevPitch()) * partialTicks;
            }
            f1 = 0.0F;
            if (entity instanceof EntityAnimal) {
                EntityAnimal entityanimal = (EntityAnimal) entity;
                yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F;
            }

            Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);
            EntityViewRenderEvent.CameraSetup event = new EntityViewRenderEvent.CameraSetup((EntityRenderer) ((Object) this), entity, block, partialTicks, yaw, pitch, f1);
            MinecraftForge.EVENT_BUS.post(event);
            GlStateManager.rotate(event.roll, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(event.pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(event.yaw, 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.translate(0.0F, -f, 0.0F);
        d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
        this.cloudFog = mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "HEAD"))
    public void freelook(float partialTicks, long nanoTime, CallbackInfo ci) {
        FreeLook.overrideMouse();
    }

}
