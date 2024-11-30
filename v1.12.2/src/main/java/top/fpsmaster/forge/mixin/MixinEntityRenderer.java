package top.fpsmaster.forge.mixin;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
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

    @Shadow
    private float thirdPersonDistancePrev = 4.0F;

    @Shadow
    private boolean cloudFog;
    @Shadow
    private boolean debugView = false;

    @Shadow
    private float fovModifierHand;
    @Shadow
    private float fovModifierHandPrev;
    private float screenScale = -1;

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

                IBlockState iblockstate = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, entity, partialTicks);
                if (iblockstate.getMaterial() == Material.WATER) {
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
                cir.setReturnValue(ForgeHooksClient.getFOVModifier((EntityRenderer) (Object) this, entity, iblockstate, partialTicks, screenScale));
            }
        }
    }


    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE))
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo callbackInfo) {
        EventDispatcher.dispatchEvent(new EventRender3D(partialTicks));
    }

    @Inject(method = "applyBobbing", at = @At("HEAD"), cancellable = true)
    public void bobbing(float partialTicks, CallbackInfo ci) {
        if (MinimizedBobbing.using)
            ci.cancel();
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    public void hurtCameraEffect(float amount, CallbackInfo ci) {
        if (NoHurtCam.using)
            ci.cancel();
    }
    @Unique
    private float partialTicks;


    @Redirect(method = "renderWor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"))
    public float modifyEyeHeight_renderWorldDirections(Entity entity) {
        if (mc.getRenderViewEntity() != mc.player) return entity.getEyeHeight();
        return OldAnimations.Companion.getClientEyeHeight(partialTicks);
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
        if (mc.getRenderViewEntity() == mc.player){
            f = OldAnimations.Companion.getClientEyeHeight(partialTicks);
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
                IBlockState iblockstate = mc.world.getBlockState(blockpos);
                ForgeHooksClient.orientBedCamera(mc.world, blockpos, iblockstate, entity);
                if (FreeLook.using) {
                    GlStateManager.rotate(FreeLook.getCameraPrevYaw() + (FreeLook.getCameraYaw() - FreeLook.getCameraPrevYaw()) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(FreeLook.getCameraPrevPitch() + (FreeLook.getCameraPitch() - FreeLook.getCameraPrevPitch()) * partialTicks, -1.0F, 0.0F, 0.0F);
                } else {
                    GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
                }
            }
        } else if (mc.gameSettings.thirdPersonView > 0) {
            double d3 = this.thirdPersonDistancePrev + (4.0F - this.thirdPersonDistancePrev) * partialTicks;
            if (mc.gameSettings.debugCamEnable) {
                GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
            } else {
                f1 = entity.rotationYaw;
                float f2 = entity.rotationPitch;
                if (mc.gameSettings.thirdPersonView == 2) {
                    f2 += 180.0F;
                }

                double d4 = (double) (-MathHelper.sin(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F)) * d3;
                double d5 = (double) (MathHelper.cos(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F)) * d3;
                double d6 = (double) (-MathHelper.sin(f2 * 0.017453292F)) * d3;

                for (int i = 0; i < 8; ++i) {
                    float f3 = (float) ((i & 1) * 2 - 1);
                    float f4 = (float) ((i >> 1 & 1) * 2 - 1);
                    float f5 = (float) ((i >> 2 & 1) * 2 - 1);
                    f3 *= 0.1F;
                    f4 *= 0.1F;
                    f5 *= 0.1F;
                    RayTraceResult raytraceresult = mc.world.rayTraceBlocks(new Vec3d(d0 + (double) f3, d1 + (double) f4, d2 + (double) f5), new Vec3d(d0 - d4 + (double) f3 + (double) f5, d1 - d6 + (double) f4, d2 - d5 + (double) f5));
                    if (raytraceresult != null) {
                        double d7 = raytraceresult.hitVec.distanceTo(new Vec3d(d0, d1, d2));
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
            GlStateManager.translate(0.0F, 0.0F, 0.05F);
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

            IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, entity, partialTicks);
            EntityViewRenderEvent.CameraSetup event = new EntityViewRenderEvent.CameraSetup((EntityRenderer) ((Object) this), entity, state, partialTicks, yaw, pitch, f1);
            MinecraftForge.EVENT_BUS.post(event);
            GlStateManager.rotate(event.getRoll(), 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(event.getPitch(), 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(event.getYaw(), 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.translate(0.0F, -f, 0.0F);
        d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
        this.cloudFog = mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
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
    public abstract void setupOverlayRendering();

    @Shadow
    private long renderEndNanoTime;

    @Shadow
    public abstract void renderWorld(float partialTicks, long finishTimeNano);

    @Shadow
    private long timeWorldIcon;

    @Shadow
    protected abstract void createWorldIcon();

    @Shadow
    private ShaderGroup shaderGroup;
    @Shadow
    private boolean useShader;

    @Shadow
    protected abstract void renderItemActivation(int p_190563_1_, int p_190563_2_, float p_190563_3_);

    @Inject(method = "updateCameraAndRender", at = @At(value = "HEAD"))
    public void freelook(float partialTicks, long nanoTime, CallbackInfo ci) {
        FreeLook.overrideMouse();
    }

}
