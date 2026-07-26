package top.fpsmaster.utils.render.culling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.forge.api.ICullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Hardware occlusion culling for entities.
 *
 * <p>Draws each entity's bounding box as a depth-only probe wrapped in an occlusion query, and skips
 * rendering entities whose probe reported no samples passing. On a scene with real occluders this
 * removes the model, texture and animation work for everything behind a wall — measured at 43% of
 * frame time on the entity-dense scenario before any culling.
 *
 * <p>Written against the GL specification rather than ported: the mods implementing this for 1.8.9
 * are CC BY-NC-SA or carry no licence at all, neither of which is compatible with GPL-3.0.
 *
 * <h3>Why it is shaped this way</h3>
 *
 * <p><b>Probes are issued in one batch</b> at the start of the entity pass, not lazily per entity.
 * The probe needs colour and depth writes masked off and texturing disabled; doing that per entity
 * would mean two state transitions per entity and would interleave with each entity's own state
 * setup. Batching costs one setup and one restore per frame. The tradeoff is that probes only test
 * against terrain, since no entity has been drawn yet — terrain is the occluder that matters.
 *
 * <p><b>Results are never waited for.</b> Reading {@code GL_QUERY_RESULT} blocks until the GPU
 * reaches that command, which would stall the frame the culling is supposed to speed up. Queries are
 * polled with {@code GL_QUERY_RESULT_AVAILABLE} and a result that is not ready simply leaves the
 * previous verdict in place for another frame.
 *
 * <p><b>Entities default to visible.</b> Every failure mode — no result yet, first sight of an
 * entity, query pool exhausted — renders the entity. Culling something that should be visible is a
 * rendering bug that makes frame times look better, which is the worst possible failure direction.
 *
 * <p><b>The camera inside a bounding box always renders.</b> Back-face culling stays enabled so the
 * probe rasterises half as many fragments, which means a box containing the camera would produce no
 * samples and be wrongly culled.
 */
public final class EntityCulling {

    /** Queries in flight are bounded so a pathological entity count cannot exhaust GL objects. */
    private static final int MAX_QUERIES = 512;

    /** An entity keeps its verdict this long before being probed again. */
    private static final long DEFAULT_REPROBE_MILLIS = 50L;

    /** Bounding boxes are grown slightly so a probe cannot fall inside its own model's geometry. */
    private static final double PROBE_EXPANSION = 0.15d;

    /**
     * Entities with a query in flight, so results can be harvested without walking the world.
     *
     * <p>Holds strong references only between issuing a probe and reading it back, which is at most
     * a handful of frames; anything longer would keep dead entities alive.
     */
    private final List<Entity> pendingProbes = new ArrayList<Entity>();
    private final ArrayDeque<Integer> freeQueries = new ArrayDeque<Integer>();
    private int allocatedQueries;
    private boolean supported;
    private boolean initialised;
    private int queryTarget;

    public void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        // Occlusion queries are GL 1.5 core, so the capability check is really about
        // GL_ANY_SAMPLES_PASSED, which lets the driver stop counting after the first sample.
        supported = GLContext.getCapabilities().OpenGL15;
        queryTarget = GLContext.getCapabilities().OpenGL33
                ? GL33.GL_ANY_SAMPLES_PASSED
                : GL15.GL_SAMPLES_PASSED;
    }

    public boolean isSupported() {
        return supported;
    }

    /** Whether the entity should be drawn. Anything not positively known to be hidden is drawn. */
    public boolean shouldRender(Entity entity, boolean cullPlayers) {
        if (!supported || !isCullable(entity, cullPlayers)) {
            return true;
        }
        return !((ICullable) entity).fpsmaster$isOccluded();
    }

    /**
     * Players are excluded by default.
     *
     * <p>Skipping an entity's render also skips its nameplate, and this client has a NameTags
     * feature built on that. Hiding a player's tag because a wall is in the way is a change in what
     * the client shows, not a rendering optimisation, so it is opt-in rather than a side effect.
     */
    private static boolean isCullable(Entity entity, boolean cullPlayers) {
        return cullPlayers || !(entity instanceof EntityPlayer);
    }

    /**
     * Issues probes for entities due a refresh, and harvests whatever results are ready.
     *
     * <p>Call once at the start of the entity pass, after opaque terrain has been drawn.
     */
    public void update(Minecraft mc, double renderPosX, double renderPosY, double renderPosZ,
                       long reprobeMillis, boolean cullPlayers) {
        if (!supported || mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }
        harvest();

        long now = System.currentTimeMillis();
        Entity viewEntity = mc.getRenderViewEntity();
        List<Entity> entities = mc.theWorld.getLoadedEntityList();

        beginProbeState();
        try {
            WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                if (entity == viewEntity || !isCullable(entity, cullPlayers)) {
                    continue;
                }
                ICullable state = (ICullable) entity;
                if (state.fpsmaster$isQueryPending()
                        || now - state.fpsmaster$getLastProbeMillis() < reprobeMillis) {
                    continue;
                }

                AxisAlignedBB box = entity.getEntityBoundingBox().expand(
                        PROBE_EXPANSION, PROBE_EXPANSION, PROBE_EXPANSION);
                if (containsCamera(box, renderPosX, renderPosY, renderPosZ)) {
                    // Front faces only means a box around the camera rasterises nothing.
                    state.fpsmaster$setOccluded(false);
                    continue;
                }
                Integer queryId = acquireQuery();
                if (queryId == null) {
                    state.fpsmaster$setOccluded(false);
                    continue;
                }
                state.fpsmaster$setQueryId(queryId.intValue());
                state.fpsmaster$setQueryPending(true);
                state.fpsmaster$setLastProbeMillis(now);
                pendingProbes.add(entity);

                if (BenchmarkMode.ACTIVE) {
                    BenchCounters.cullProbesIssued++;
                }
                GL15.glBeginQuery(queryTarget, queryId.intValue());
                drawBox(worldRenderer, box, renderPosX, renderPosY, renderPosZ);
                GL15.glEndQuery(queryTarget);
            }
        } finally {
            endProbeState();
        }
    }

    private void harvest() {
        for (int i = pendingProbes.size() - 1; i >= 0; i--) {
            ICullable state = (ICullable) pendingProbes.get(i);
            int queryId = state.fpsmaster$getQueryId();
            if (GL15.glGetQueryObjectui(queryId, GL15.GL_QUERY_RESULT_AVAILABLE) != GL11.GL_TRUE) {
                continue;  // not ready: keep the previous verdict rather than stall
            }
            boolean occluded = GL15.glGetQueryObjectui(queryId, GL15.GL_QUERY_RESULT) == 0;
            state.fpsmaster$setOccluded(occluded);
            state.fpsmaster$setQueryPending(false);
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.cullProbesHarvested++;
                if (occluded) {
                    BenchCounters.cullProbesOccluded++;
                }
            }
            freeQueries.addLast(queryId);
            // Swap-remove: order does not matter and this avoids shifting the tail.
            pendingProbes.set(i, pendingProbes.get(pendingProbes.size() - 1));
            pendingProbes.remove(pendingProbes.size() - 1);
        }
    }

    private Integer acquireQuery() {
        if (!freeQueries.isEmpty()) {
            return freeQueries.pollFirst();
        }
        if (allocatedQueries >= MAX_QUERIES) {
            return null;
        }
        allocatedQueries++;
        return GL15.glGenQueries();
    }

    private static boolean containsCamera(AxisAlignedBB box, double x, double y, double z) {
        return x >= box.minX && x <= box.maxX
                && y >= box.minY && y <= box.maxY
                && z >= box.minZ && z <= box.maxZ;
    }

    /**
     * Masks colour and depth writes for the probe pass.
     *
     * <p>Deliberately touches as little state as possible. An earlier version also disabled blending
     * and lighting but only restored texturing and lighting, so a probe pass left blending off for
     * whatever drew next. Neither blending nor lighting can affect the result once colour writes are
     * masked, so the fix is to not touch them at all rather than to add a matching restore.
     */
    private static void beginProbeState() {
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.colorMask(false, false, false, false);
    }

    private static void endProbeState() {
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    private static void drawBox(WorldRenderer worldRenderer, AxisAlignedBB box,
                                double renderPosX, double renderPosY, double renderPosZ) {
        double x0 = box.minX - renderPosX;
        double y0 = box.minY - renderPosY;
        double z0 = box.minZ - renderPosZ;
        double x1 = box.maxX - renderPosX;
        double y1 = box.maxY - renderPosY;
        double z1 = box.maxZ - renderPosZ;

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        // Counter-clockwise when seen from outside, so front-face culling keeps exactly one
        // rasterised face per silhouette direction.
        worldRenderer.pos(x0, y0, z1).endVertex();
        worldRenderer.pos(x1, y0, z1).endVertex();
        worldRenderer.pos(x1, y1, z1).endVertex();
        worldRenderer.pos(x0, y1, z1).endVertex();

        worldRenderer.pos(x1, y0, z0).endVertex();
        worldRenderer.pos(x0, y0, z0).endVertex();
        worldRenderer.pos(x0, y1, z0).endVertex();
        worldRenderer.pos(x1, y1, z0).endVertex();

        worldRenderer.pos(x0, y0, z0).endVertex();
        worldRenderer.pos(x0, y0, z1).endVertex();
        worldRenderer.pos(x0, y1, z1).endVertex();
        worldRenderer.pos(x0, y1, z0).endVertex();

        worldRenderer.pos(x1, y0, z1).endVertex();
        worldRenderer.pos(x1, y0, z0).endVertex();
        worldRenderer.pos(x1, y1, z0).endVertex();
        worldRenderer.pos(x1, y1, z1).endVertex();

        worldRenderer.pos(x0, y1, z1).endVertex();
        worldRenderer.pos(x1, y1, z1).endVertex();
        worldRenderer.pos(x1, y1, z0).endVertex();
        worldRenderer.pos(x0, y1, z0).endVertex();

        worldRenderer.pos(x0, y0, z0).endVertex();
        worldRenderer.pos(x1, y0, z0).endVertex();
        worldRenderer.pos(x1, y0, z1).endVertex();
        worldRenderer.pos(x0, y0, z1).endVertex();
        Tessellator.getInstance().draw();
    }

    /** Drops in-flight probes; call when the world changes so queries cannot outlive their entity. */
    public void reset() {
        for (int i = 0; i < pendingProbes.size(); i++) {
            ICullable state = (ICullable) pendingProbes.get(i);
            state.fpsmaster$setQueryPending(false);
            state.fpsmaster$setOccluded(false);
            freeQueries.addLast(state.fpsmaster$getQueryId());
        }
        pendingProbes.clear();
    }

    public void countVisibility(boolean rendered) {
        if (BenchmarkMode.ACTIVE) {
            if (rendered) {
                BenchCounters.entitiesRendered++;
            } else {
                BenchCounters.entitiesCulled++;
            }
        }
    }

    public static long defaultReprobeMillis() {
        return DEFAULT_REPROBE_MILLIS;
    }
}
