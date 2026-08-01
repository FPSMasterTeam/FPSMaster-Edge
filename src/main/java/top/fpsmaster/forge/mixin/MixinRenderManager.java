package top.fpsmaster.forge.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.render.FreeLook;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.api.IRenderManager;

@Mixin(value = RenderManager.class, priority = 999)
@Implements(@Interface(iface = IRenderManager.class, prefix = "fpsmaster$"))
public class MixinRenderManager implements IRenderManager {

    @Shadow
    private double renderPosX;
    @Shadow
    private double renderPosY;
    @Shadow
    private double renderPosZ;

    @Override
    public double renderPosX() {
        return renderPosX;
    }

    @Override
    public double renderPosY() {
        return renderPosY;
    }

    @Override
    public double renderPosZ() {
        return renderPosZ;
    }

    @Inject(method = "doRenderEntity", at = @At("HEAD"), cancellable = true)
    public void preRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.entitiesAttempted++;
            BenchProfiler.begin(BenchProfiler.SECTION_ENTITY_RENDER);
        }
        // Before the occlusion check, because an entity outside its category's range is not being
        // drawn either way and there is no point probing whether something is in front of it.
        if (Performance.using && edge$isBeyondCategoryRange(entity, x, y, z)) {
            if (BenchmarkMode.ACTIVE) {
                BenchProfiler.end(BenchProfiler.SECTION_ENTITY_RENDER);
            }
            cir.setReturnValue(false);
            return;
        }
        if (Performance.using && Performance.entityCulling.getValue()
                && entity != Minecraft.getMinecraft().getRenderViewEntity()
                && !Performance.ENTITY_CULLING.shouldRender(entity, Performance.cullPlayers.getValue())) {
            Performance.ENTITY_CULLING.countVisibility(false);
            if (BenchmarkMode.ACTIVE) {
                BenchProfiler.end(BenchProfiler.SECTION_ENTITY_RENDER);
            }
            cir.setReturnValue(false);
            return;
        }
        Performance.ENTITY_CULLING.countVisibility(true);
    }

    /**
     * Applies the per-category render distance multipliers.
     *
     * <p>Named {@code edge$} rather than {@code fpsmaster$} on purpose. This class carries an
     * {@code @Implements} with that prefix, which means every method starting with it is treated as
     * a soft implementation of an {@link IRenderManager} method — so a helper called
     * {@code fpsmaster$isBeyondCategoryRange} makes Mixin look for that name on the interface, not
     * find it, and refuse to apply the mixin at all. Which is how this shipped: the render
     * distances did nothing, {@code FreeLook}'s overwrite in this class did nothing, and entity
     * occlusion culling crashed on casting a {@code RenderManager} that no longer implemented the
     * interface.
     *
     * <p>Reproduces vanilla's own limit — the bounding box's average edge times 64 times the
     * entity's {@code renderDistanceWeight} — and scales it. Scaling rather than replacing, because
     * that limit already differs per entity: a fixed distance in blocks would put a dropped item and
     * a giant on the same leash.
     *
     * <p>The coordinates handed to this method are already relative to the camera, so their length
     * is the distance the limit is compared against and nothing has to be recomputed.
     */
    @Unique
    private static boolean edge$isBeyondCategoryRange(Entity entity, double x, double y, double z) {
        double multiplier;
        if (entity instanceof EntityPlayer) {
            multiplier = Performance.playerRenderDistance.getValue().doubleValue();
        } else if (entity instanceof IMob) {
            // Checked before IAnimals: IMob extends it, so the passive test would swallow hostiles.
            multiplier = Performance.hostileRenderDistance.getValue().doubleValue();
        } else if (entity instanceof IAnimals) {
            multiplier = Performance.passiveRenderDistance.getValue().doubleValue();
        } else {
            multiplier = Performance.miscRenderDistance.getValue().doubleValue();
        }
        if (multiplier >= 1.0d) {
            return false;
        }
        double edge = entity.getEntityBoundingBox().getAverageEdgeLength();
        if (Double.isNaN(edge)) {
            edge = 1.0d;
        }
        double limit = edge * 64.0d * entity.renderDistanceWeight * multiplier;
        return x * x + y * y + z * z > limit * limit;
    }

    /**
     * Closes the per-entity bracket.
     *
     * <p>Splitting the entity pass this way separates the actual model rendering from the walk over
     * the entity list, the frustum checks and the tile-entity work that also live in it. At 1073us
     * for roughly 95 armour stands, 11us each looked too high to attribute without checking.
     */
    @Inject(method = "doRenderEntity", at = @At("RETURN"))
    public void postRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_ENTITY_RENDER);
        }
    }

    @Shadow
    public float playerViewY;
    @Shadow
    public float playerViewX;
    @Shadow
    public double viewerPosX;
    @Shadow
    public double viewerPosY;
    @Shadow
    public double viewerPosZ;
    @Shadow
    public GameSettings options;
    @Shadow
    public Entity pointedEntity;
    @Shadow
    private FontRenderer textRenderer;
    @Shadow
    public World worldObj;
    @Shadow
    public Entity livingPlayer;


    /**
     * @author SuperSkidder
     * @reason Free Look
     */
    @Overwrite
    public void cacheActiveRenderInfo(World worldIn, FontRenderer textRendererIn, Entity livingPlayerIn, Entity pointedEntityIn, GameSettings optionsIn, float partialTicks) {
        this.worldObj = worldIn;
        this.options = optionsIn;
        this.livingPlayer = livingPlayerIn;
        this.pointedEntity = pointedEntityIn;
        this.textRenderer = textRendererIn;
        if (livingPlayerIn instanceof EntityLivingBase && ((EntityLivingBase) livingPlayerIn).isPlayerSleeping()) {
            IBlockState iblockstate = worldIn.getBlockState(new BlockPos(livingPlayerIn));
            Block block = iblockstate.getBlock();
            if (block.isBed(worldIn, new BlockPos(livingPlayerIn), livingPlayerIn)) {
                int i = block.getBedDirection(worldIn, new BlockPos(livingPlayerIn)).getHorizontalIndex();
                this.playerViewY = (float) (i * 90 + 180);
                this.playerViewX = 0.0F;
            }
        } else {
            if (FreeLook.using) {
                this.playerViewY = FreeLook.getCameraPrevYaw() + (FreeLook.getCameraYaw() - FreeLook.getCameraPrevYaw()) * partialTicks;
                this.playerViewX = FreeLook.getCameraPrevPitch() + (FreeLook.getCameraPitch() - FreeLook.getCameraPrevPitch()) * partialTicks;
            } else {
                this.playerViewY = livingPlayerIn.prevRotationYaw + (livingPlayerIn.rotationYaw - livingPlayerIn.prevRotationYaw) * partialTicks;
                this.playerViewX = livingPlayerIn.prevRotationPitch + (livingPlayerIn.rotationPitch - livingPlayerIn.prevRotationPitch) * partialTicks;
            }
        }

        if (optionsIn.thirdPersonView == 2) {
            this.playerViewY += 180.0F;
        }

        this.viewerPosX = livingPlayerIn.lastTickPosX + (livingPlayerIn.posX - livingPlayerIn.lastTickPosX) * (double) partialTicks;
        this.viewerPosY = livingPlayerIn.lastTickPosY + (livingPlayerIn.posY - livingPlayerIn.lastTickPosY) * (double) partialTicks;
        this.viewerPosZ = livingPlayerIn.lastTickPosZ + (livingPlayerIn.posZ - livingPlayerIn.lastTickPosZ) * (double) partialTicks;
    }

}



