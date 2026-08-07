package top.fpsmaster.forge.mixin;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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
import net.minecraft.potion.Potion;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.*;
import net.minecraft.init.Blocks;
import net.minecraft.block.BlockBed;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
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
import top.fpsmaster.features.impl.interfaces.ReachDisplay;
import top.fpsmaster.features.impl.optimizes.NoHurtCam;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.SmoothZoom;
import top.fpsmaster.features.impl.render.FreeLook;
import top.fpsmaster.features.impl.render.MinimizedBobbing;
import top.fpsmaster.features.impl.render.CustomFog;
import top.fpsmaster.utils.math.MathUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    private float screenScale = -1;
    private float screenScale2 = -1;

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

                if (useFOVSetting) {
                    if (screenScale == -1 || Double.isNaN(screenScale)) {
                        screenScale = f;
                    }
                } else {
                    if (screenScale2 == -1 || Double.isNaN(screenScale2)) {
                        screenScale2 = f;
                    }
                }
                if (SmoothZoom.zoom) {
                    if (useFOVSetting) {
                        if (SmoothZoom.smoothCamera.getValue()) {
                            screenScale = MathUtils.decreasedSpeed(screenScale, f, f / SmoothZoom.zoomScale, SmoothZoom.speed.getValue().floatValue() / (float) Minecraft.getDebugFPS() * 150.0f);
                        } else {
                            screenScale = f / SmoothZoom.zoomScale;
                        }
                    } else {
                        if (SmoothZoom.smoothCamera.getValue()) {
                            screenScale2 = MathUtils.decreasedSpeed(screenScale2, f, f / SmoothZoom.zoomScale, SmoothZoom.speed.getValue().floatValue() / (float) Minecraft.getDebugFPS() * 150.0f);
                        } else {
                            screenScale2 = f / SmoothZoom.zoomScale;
                        }
                    }
                }

                if (!SmoothZoom.zoom) {
                    if (useFOVSetting) {
                        if (SmoothZoom.smoothCamera.getValue()) {
                            screenScale = MathUtils.decreasedSpeed(screenScale, f / SmoothZoom.zoomScale, f, SmoothZoom.speed.getValue().floatValue() / (float) Minecraft.getDebugFPS() * 150.0f);
                        } else {
                            screenScale = f;
                        }
                    } else {
                        if (SmoothZoom.smoothCamera.getValue()) {
                            screenScale2 = MathUtils.decreasedSpeed(screenScale2, f / SmoothZoom.zoomScale, f, SmoothZoom.speed.getValue().floatValue() / (float) Minecraft.getDebugFPS() * 150.0f);
                        } else {
                            screenScale2 = f;
                        }
                    }
                }

                float screenScale = useFOVSetting ? this.screenScale : this.screenScale2;

                if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F) {
                    float f1 = (float) ((EntityLivingBase) entity).deathTime + partialTicks;
                    screenScale /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
                }

                assert entity != null;
                Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);

                if (block.getMaterial() == Material.water) {
                    screenScale = screenScale * 60.0F / 70.0F;
                }

                // Vanilla-equivalent of ForgeHooksClient.getFOVModifier: with no other FOV mods
                // subscribing (this client is the only mod), the Forge hook just returns the value
                // computed above, so return it directly and keep the class off Forge's constant pool.
                cir.setReturnValue(screenScale);
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
        if (mc.getRenderViewEntity() == mc.thePlayer && OldAnimations.using) {
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
                // Inline vanilla-equivalent of ForgeHooksClient.orientBedCamera: for a vanilla bed,
                // rotate the camera to the bed's facing. Keeps behaviour identical without Forge.
                if (iblockstate.getBlock() == Blocks.bed) {
                    int bedDir = ((EnumFacing) iblockstate.getValue(BlockBed.FACING)).getHorizontalIndex();
                    GlStateManager.rotate(bedDir * 90.0F, 0.0F, 1.0F, 0.0F);
                }
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

            // Vanilla-equivalent of the EntityViewRenderEvent.CameraSetup post: with no camera
            // mods subscribing, roll stays f1 (0) and yaw/pitch are the values computed above.
            GlStateManager.rotate(f1, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
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

    @Shadow
    private float fogColorRed;
    @Shadow
    private float fogColorGreen;
    @Shadow
    private float fogColorBlue;

    @Shadow
    private FloatBuffer setFogColorBuffer(float red, float green, float blue, float alpha) {
        throw new AssertionError();
    }

    /**
     * CustomFog 是否应该接管当前这一帧的雾。
     *
     * <p>失明药水那条是硬性的：原版会把雾强制成极短的线性雾，覆盖它等于解除视野限制，
     * 在服务器上属于优势而不是外观改动。
     */
    @Unique
    private boolean fpsmaster$shouldOverrideFog() {
        if (!CustomFog.using) return false;
        if (mc.thePlayer == null) return false;
        if (mc.thePlayer.isPotionActive(Potion.blindness)) return false;
        if (!CustomFog.affectWater.getValue() && mc.thePlayer.isInsideOfMaterial(Material.water)) return false;
        if (!CustomFog.affectLava.getValue() && mc.thePlayer.isInsideOfMaterial(Material.lava)) return false;
        return true;
    }

    /**
     * 天空和清屏色走的是 fogColorRed/Green/Blue 这三个字段（renderWorldPass 拿它 glClearColor，
     * renderSky 拿它画天空和地平线的雾带），跟 setupFog 里的 GL_FOG_COLOR 是两套状态。只改后者
     * 会得到"方块被自定义雾吃掉、天空还是原版蓝"的割裂画面，所以这里一并覆盖。
     */
    @Inject(method = "updateFogColor", at = @At("RETURN"))
    private void overrideFogColor(float partialTicks, CallbackInfo ci) {
        if (!fpsmaster$shouldOverrideFog()) return;
        java.awt.Color fogColor = CustomFog.color.getColor();
        fogColorRed = fogColor.getRed() / 255f;
        fogColorGreen = fogColor.getGreen() / 255f;
        fogColorBlue = fogColor.getBlue() / 255f;
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void overrideFog(int startCoords, float partialTicks, CallbackInfo ci) {
        if (!fpsmaster$shouldOverrideFog()) return;

        // 雾色只能用 glFog(GL_FOG_COLOR, buffer) 设置，不能走顶点色。
        // 复用原版的 setFogColorBuffer：它写的是 EntityRenderer 自己缓存的 direct buffer，
        // 而 setupFog 每帧要跑好几次，这里再新建 buffer 会持续制造 direct 内存分配。
        java.awt.Color fogColor = CustomFog.color.getColor();
        GL11.glFog(GL11.GL_FOG_COLOR, setFogColorBuffer(
                fogColor.getRed() / 255f,
                fogColor.getGreen() / 255f,
                fogColor.getBlue() / 255f,
                1.0f));

        if (CustomFog.fogMode.isMode("Linear")) {
            GlStateManager.setFog(GL11.GL_LINEAR);
            float start = CustomFog.startDistance.getValue().floatValue();
            float end = CustomFog.endDistance.getValue().floatValue();
            // 防反转：end 必须比 start 大，否则线性雾公式 (end - z)/(end - start) 会反过来，
            // 近处反而更浓。顺带把范围钳制在渲染距离内，避免雾一直延伸到可视边界外。
            if (end <= start) {
                end = start + 1.0f;
            }
            GlStateManager.setFogStart(start);
            GlStateManager.setFogEnd(end);
        } else {
            GlStateManager.setFog(GL11.GL_EXP);
            float density = 1.0f / Math.max(0.1f, CustomFog.endDistance.getValue().floatValue());
            GlStateManager.setFogDensity(density);
        }
    }


    @Shadow
    private Entity pointedEntity;

    /**
     * @author SuperSkidder
     * @reason Reach Display
     */
    @Overwrite
    public void getMouseOver(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;
            double d0 = (double) mc.playerController.getBlockReachDistance();
            mc.objectMouseOver = entity.rayTrace(d0, partialTicks);
            double d1 = d0;
            Vec3 vec3 = entity.getPositionEyes(partialTicks);
            boolean flag = false;
            int i = 3;
            if (mc.playerController.extendedReach()) {
                d0 = (double) 6.0F;
                d1 = (double) 6.0F;
            } else if (d0 > (double) 3.0F) {
                flag = true;
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            Vec3 vec31 = entity.getLook(partialTicks);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            this.pointedEntity = null;
            Vec3 vec33 = null;
            float f = 1.0F;
            List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand((double) f, (double) f, (double) f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (int j = 0; j < list.size(); ++j) {
                Entity entity1 = (Entity) list.get(j);
                float f1 = entity1.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand((double) f1, (double) f1, (double) f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= (double) 0.0F) {
                        this.pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = (double) 0.0F;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                    if (d3 < d2 || d2 == (double) 0.0F) {
                        if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                            if (d2 == (double) 0.0F) {
                                this.pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            this.pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            double v = 0;
            if (vec33 != null) {
                v = vec3.distanceTo(vec33);
            }

            if (this.pointedEntity != null && flag && v > (double) 3.0F) {
                this.pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, (EnumFacing) null, new BlockPos(vec33));
            }

            if (this.pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, vec33);
                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = this.pointedEntity;
                    ReachDisplay.distance = v;
                }
            }

            mc.mcProfiler.endSection();
        }
    }

}



