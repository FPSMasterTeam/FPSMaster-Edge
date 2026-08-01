package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.util.Set;

/**
 * Keeps the visible-chunk list across frames in which the camera has barely moved.
 *
 * <p>The walk that builds that list is 836us of an 891us {@code setupTerrain} on an orbiting
 * camera — 41% of the frame, the largest single item this client has measured. Forge already
 * skips it when nothing changed, but its test for "changed" is an exact inequality on the view
 * entity's position and rotation, so a camera moving a sub-pixel distance rebuilds the whole
 * list. At 437 frames a second that is 437 rebuilds a second producing the same answer.
 *
 * <p>The threshold is measured from where the list was last actually built, not from the previous
 * frame. Comparing against the previous frame is the obvious version and it never fires: each
 * frame's movement stays under the threshold, the anchor follows the camera anyway, and a player
 * walking slowly across the world would keep a visible list built where they started.
 *
 * <p>Only the camera's contribution is suppressed. Everything else Forge sets the flag for — a
 * pending chunk update, a rebuilt or newly visible chunk, {@code loadRenderers} on a render
 * distance or world change — passes through untouched, because a chunk whose geometry changed has
 * to be walked again and there is no version of that which is optional.
 *
 * <p>And a rebuild happens at least every {@link #MAX_REUSE_MILLIS} regardless. That is not for
 * any invalidation named here; it is for the ones that are not. A missed case with this in place
 * is a fifth of a second of stale visibility, and without it is a hole in the world that stays
 * until the player turns around.
 */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin_ReuseVisibleList {

    /**
     * Blocks of travel from the anchor before the list is rebuilt, per level.
     *
     * <p>All three are bounded by the same fact: a chunk is sixteen blocks across, and crossing a
     * chunk boundary already forces a rebuild on its own. Within one chunk, travel can only slide
     * the edge of a chunk into frustum, and the frustum test runs on a bounding box that would have
     * accepted it a frame earlier.
     */
    @Unique
    private static final double[] POSITION_THRESHOLDS = {0.25d, 0.5d, 1.0d};

    /**
     * Degrees of rotation from the anchor before the list is rebuilt, per level.
     *
     * <p>Chosen against what a rotation can reveal rather than by feel: a chunk sixteen blocks
     * across at render distance twelve subtends about five degrees, so none of these can bring one
     * into view from nothing — only slide an edge of one, which the frustum test on a bounding box
     * would already have accepted a frame earlier. Five is where the reasoning stops holding, and
     * the most aggressive level sits at two so the argument still has room in it.
     */
    @Unique
    private static final float[] ROTATION_THRESHOLDS = {0.5f, 1.0f, 2.0f};

    /**
     * Ceiling on how long a list may be reused, whatever the camera did.
     *
     * <p>Deliberately not part of the level. It is the safety net for invalidations nobody named,
     * not a performance dial — at any frame rate worth having, a forced rebuild five times a second
     * is a fraction of a percent of frames, so raising it buys nothing and lengthens how long a
     * missed case stays on screen.
     */
    @Unique
    private static final long MAX_REUSE_MILLIS = 200L;

    @Shadow
    private boolean displayListEntitiesDirty;

    @Shadow
    private Set<RenderChunk> chunksToUpdate;

    @Unique
    private boolean fpsmaster$rebuildForced;
    @Unique
    private double fpsmaster$anchorX;
    @Unique
    private double fpsmaster$anchorY;
    @Unique
    private double fpsmaster$anchorZ;
    @Unique
    private float fpsmaster$anchorYaw;
    @Unique
    private float fpsmaster$anchorPitch;
    @Unique
    private int fpsmaster$anchorChunkX = Integer.MIN_VALUE;
    @Unique
    private int fpsmaster$anchorChunkY;
    @Unique
    private int fpsmaster$anchorChunkZ;
    @Unique
    private long fpsmaster$anchorMillis;
    @Unique
    private float fpsmaster$anchorFov;
    @Unique
    private int fpsmaster$anchorWidth;
    @Unique
    private int fpsmaster$anchorHeight;

    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void fpsmaster$decideReuse(Entity viewEntity, double partialTicks, ICamera camera,
                                       int frameCount, boolean playerSpectator, CallbackInfo ci) {
        fpsmaster$rebuildForced = true;
        if (!Performance.using || !Performance.reuseVisibleChunks.getValue()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        // The projection decides what the walk accepts as much as the camera does, so a change to
        // it has to force a rebuild even from a camera that has not moved at all: a zoom key held
        // down narrows the frustum without moving anything.
        boolean projectionChanged = fpsmaster$anchorChunkX == Integer.MIN_VALUE
                || fpsmaster$anchorFov != mc.gameSettings.fovSetting
                || fpsmaster$anchorWidth != mc.displayWidth
                || fpsmaster$anchorHeight != mc.displayHeight;

        int level = Math.min(Performance.reuseVisibleChunksLevel.getMode(),
                POSITION_THRESHOLDS.length - 1);
        double positionThreshold = POSITION_THRESHOLDS[level];
        float rotationThreshold = ROTATION_THRESHOLDS[level];

        double dx = viewEntity.posX - fpsmaster$anchorX;
        double dy = viewEntity.posY - fpsmaster$anchorY;
        double dz = viewEntity.posZ - fpsmaster$anchorZ;
        boolean moved = dx * dx + dy * dy + dz * dz > positionThreshold * positionThreshold
                || viewEntity.chunkCoordX != fpsmaster$anchorChunkX
                || viewEntity.chunkCoordY != fpsmaster$anchorChunkY
                || viewEntity.chunkCoordZ != fpsmaster$anchorChunkZ;
        boolean turned = Math.abs(fpsmaster$wrap(viewEntity.rotationYaw - fpsmaster$anchorYaw)) > rotationThreshold
                || Math.abs(viewEntity.rotationPitch - fpsmaster$anchorPitch) > rotationThreshold;
        boolean stale = System.currentTimeMillis() - fpsmaster$anchorMillis > MAX_REUSE_MILLIS;

        fpsmaster$rebuildForced = projectionChanged || moved || turned || stale;
    }

    /**
     * Substitutes the flag Forge computed from an exact inequality.
     *
     * <p>Only ever narrows it. Vanilla saying nothing changed still means nothing changed; what
     * this can say is that the only thing that changed was the camera, and not by enough.
     */
    @Redirect(method = "setupTerrain",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;displayListEntitiesDirty:Z",
                    opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void fpsmaster$narrowDirty(RenderGlobal self, boolean vanillaValue) {
        if (!Performance.using || !Performance.reuseVisibleChunks.getValue()) {
            displayListEntitiesDirty = vanillaValue;
            return;
        }
        // Read before the assignment, so these are the reasons that existed independently of the
        // camera: something already marked the list dirty, or there is chunk work outstanding.
        boolean nonCameraReason = displayListEntitiesDirty || !chunksToUpdate.isEmpty();
        boolean rebuild = vanillaValue && (nonCameraReason || fpsmaster$rebuildForced);
        if (BenchmarkMode.ACTIVE && vanillaValue && !rebuild) {
            BenchCounters.visibleListReused++;
        }
        displayListEntitiesDirty = rebuild;
    }

    /**
     * Moves the anchor to wherever the camera is when a walk actually happens.
     *
     * <p>Anchored on the queue the walk allocates, which exists only inside the branch that
     * rebuilds — so this runs exactly when a rebuild does, and never when one is skipped.
     */
    @Inject(method = "setupTerrain",
            at = @At(value = "INVOKE",
                    target = "Lcom/google/common/collect/Lists;newLinkedList()Ljava/util/LinkedList;"))
    private void fpsmaster$anchorHere(Entity viewEntity, double partialTicks, ICamera camera,
                                      int frameCount, boolean playerSpectator, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        fpsmaster$anchorX = viewEntity.posX;
        fpsmaster$anchorY = viewEntity.posY;
        fpsmaster$anchorZ = viewEntity.posZ;
        fpsmaster$anchorYaw = viewEntity.rotationYaw;
        fpsmaster$anchorPitch = viewEntity.rotationPitch;
        fpsmaster$anchorChunkX = viewEntity.chunkCoordX;
        fpsmaster$anchorChunkY = viewEntity.chunkCoordY;
        fpsmaster$anchorChunkZ = viewEntity.chunkCoordZ;
        fpsmaster$anchorMillis = System.currentTimeMillis();
        fpsmaster$anchorFov = mc.gameSettings.fovSetting;
        fpsmaster$anchorWidth = mc.displayWidth;
        fpsmaster$anchorHeight = mc.displayHeight;
    }

    @Unique
    private static float fpsmaster$wrap(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }
}
