package top.fpsmaster.utils.render.culling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
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
     * Extra height added to a player's probe box.
     *
     * <p>A player's bounding box stops at the top of their head, but what is drawn does not: the
     * name label floats above it. Probing the body alone would hide the label of anyone standing
     * behind a wall with their tag showing over the top of it — the label is the part that is still
     * visible, and it is the part that matters in a fight. One body height clears it.
     */
    private static final double NAMEPLATE_HEADROOM = 1.8d;

    /** How many verdicts the occlusion rate is judged over before the interval is reconsidered. */
    private static final int RATE_WINDOW = 256;

    /**
     * Hysteresis on that rate.
     *
     * <p>Below the lower bound almost nothing is being hidden and the probes are paying for
     * nothing, so they are spaced out; above the upper bound they are earning their cost and run at
     * the configured interval. Between the two the current decision stands, which is what stops a
     * scene hovering around the boundary from flipping every window.
     */
    private static final double IDLE_FRACTION = 0.05d;
    private static final double BUSY_FRACTION = 0.10d;

    /**
     * How many entities a sweep has to be hiding before culling is worth its probes.
     *
     * <p>The rate alone is not enough and this was measured the hard way. On a recorded Hypixel
     * fight with players culled, 65.5% of probes came back occluded — well past {@link
     * #BUSY_FRACTION} — so the state machine started culling, and the frame rate fell 4.5%. Two and
     * a third entities hidden per frame do not pay for twenty probes a sweep.
     *
     * <p>The two signals answer different questions. The rate says whether anything is behind
     * something; this says whether what is behind something is worth reclaiming. An entity count on
     * its own — the threshold this replaced — answered neither, because it counted entities that
     * might be in plain sight. The product of the two is what matters, and the count of entities
     * actually hidden is that product measured directly.
     */
    private static final double CULL_FLOOR = 8.0d;

    /**
     * How often the whole sweep runs while scouting.
     *
     * <p>Scouting is the state this starts in and returns to whenever the occlusion rate says the
     * probes are not earning: the sweep still runs, so the rate keeps being measured and the scene
     * can be noticed changing, but <b>nothing is hidden</b> and it runs twenty times less often.
     *
     * <p>Not hiding is what makes the long cadence safe rather than merely cheap. A verdict half a
     * second old is not something to hide an entity on — the camera has moved — so a state that
     * probes rarely must also decline to act, and one that acts must probe often. Tying the two
     * together is the whole design.
     */
    private static final long SCOUT_INTERVAL_MILLIS = 500L;

    /**
     * Entities with a query in flight, so results can be harvested without walking the world.
     *
     * <p>Holds strong references only between issuing a probe and reading it back, which is at most
     * a handful of frames; anything longer would keep dead entities alive.
     */
    private final List<Entity> pendingProbes = new ArrayList<Entity>();
    /** Reused so the per-frame candidate walk does not allocate. */
    private final List<Entity> candidates = new ArrayList<Entity>();
    private final ArrayDeque<Integer> freeQueries = new ArrayDeque<Integer>();
    private int allocatedQueries;
    private boolean supported;
    private boolean initialised;
    private int queryTarget;

    /** The last world probes were issued against, so a world change can drop stale in-flight queries. */
    private WorldClient lastWorld;

    /** Set when there is too little on screen for culling to be worth probing for. */
    /**
     * True while the probes are advisory: measured, counted, and not acted on.
     *
     * <p>Starts true, so a world that has never been looked at hides nothing.
     */
    private boolean scouting = true;

    /** Wall clock of the last full sweep, which is what the cadence is applied to. */
    private long lastSweepMillis;

    /** Set when scouting ends, so the first culling sweep does not act on a scouting verdict. */
    private boolean discardVerdicts;

    private int windowHarvested;
    private int windowOccluded;

    /** Sweeps the current rate window spans, so the occluded total can be read per sweep. */
    private int windowSweeps;

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
        if (scouting || !supported || !isCullable(entity, cullPlayers)) {
            return true;
        }
        return !((ICullable) entity).fpsmaster$isOccluded();
    }

    /**
     * The three kinds of entity worth spending a query on, and the one exception.
     *
     * <p>Probing everything is not free — each entity costs a query object, a box and a slot in the
     * pass — and most kinds cannot pay it back. In the scenes this runs in, the entity count is
     * players, the armour stands a server decorates with, and dropped items; mobs, projectiles,
     * minecarts and the rest turn up in numbers where the probes cost more than the models. So the
     * set is named rather than defined by exclusion.
     *
     * <p>Players are further gated behind a setting, because culling a player also culls the tag
     * this client's NameTags feature draws. With {@link #NAMEPLATE_HEADROOM} the probe now covers
     * where that tag is drawn, so the two agree — but it stays opt-in, since a player who cannot see
     * an opponent's tag has no way to guess which setting did it.
     *
     * <p>Anything with a custom name is never culled whatever its kind. A named armour stand is a
     * hologram, and the text is its entire content — culling it deletes the shop label rather than
     * saving the model behind it.
     */
    private static boolean isCullable(Entity entity, boolean cullPlayers) {
        if (entity.hasCustomName()) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            return cullPlayers;
        }
        return entity instanceof EntityArmorStand || entity instanceof EntityItem;
    }

    /**
     * Issues probes for entities due a refresh, and harvests whatever results are ready.
     *
     * <p>Call once at the start of the entity pass, after opaque terrain has been drawn.
     */
    public void update(Minecraft mc, ICamera camera, double renderPosX, double renderPosY,
                       double renderPosZ, long reprobeMillis, int minEntities, boolean cullPlayers) {
        if (!supported || mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }
        if (mc.theWorld != lastWorld) {
            // A world change leaves in-flight probes pointing at entities the new world never heard
            // of. Drop them (and return their query ids) before the new world issues any; without
            // this the dead entities stay strongly referenced here and their ids cannot be reissued.
            reset();
            lastWorld = (WorldClient) mc.theWorld;
        }
        harvest();

        long now = System.currentTimeMillis();
        Entity viewEntity = mc.getRenderViewEntity();
        RenderManager manager = mc.getRenderManager();
        List<Entity> entities = mc.theWorld.getLoadedEntityList();

        // The sweep below asks the render manager for each entity's renderer and runs six plane
        // checks, for every loaded entity. Paying that every frame in a scene with nothing to hide
        // cost 6.9% of the frame rate on a recorded lobby, and an entity count used to be the guard
        // against it. A count is a poor predictor of what is behind something — a hundred entities
        // in the open hide none and twenty-five in a cave hide most — so the guard is now the
        // occlusion rate the probes already measure, applied as a cadence: while scouting, this
        // whole sweep runs once every SCOUT_INTERVAL_MILLIS rather than every frame, which is the
        // same saving without needing to guess.
        //
        // minEntities is kept as a manual floor and defaults to zero. Harvesting stays outside the
        // gate: it only walks the probes already in flight and their results should not go stale.
        long sweepInterval = scouting ? SCOUT_INTERVAL_MILLIS : reprobeMillis;
        if (now - lastSweepMillis < sweepInterval) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.cullDormantFrames++;
            }
            return;
        }
        lastSweepMillis = now;
        windowSweeps++;

        if (minEntities > 0) {
            int cullable = 0;
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                if (entity != viewEntity && isCullable(entity, cullPlayers)) {
                    cullable++;
                }
            }
            if (cullable < minEntities) {
                return;
            }
        }

        // Only entities the game is about to draw anyway. Probing one that is behind the camera or
        // out of render range costs a query and a box and can save nothing, because vanilla was
        // never going to draw it.
        candidates.clear();
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (entity == viewEntity || !isCullable(entity, cullPlayers)) {
                continue;
            }
            if (!manager.shouldRender(entity, camera, renderPosX, renderPosY, renderPosZ)) {
                ((ICullable) entity).fpsmaster$setInFrustum(false);
                continue;
            }
            candidates.add(entity);
        }
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.cullCandidates += candidates.size();
        }

        // Verdicts collected while scouting were measured up to half a second ago, from a camera
        // that has since moved. The sweep that starts culling throws them away and probes fresh:
        // one frame of everything visible is the safe direction, and hiding on a stale verdict is
        // the artefact this whole design exists to avoid.
        boolean discard = discardVerdicts;
        discardVerdicts = false;

        long interval = reprobeMillis;
        beginProbeState();
        try {
            WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
            for (int i = 0; i < candidates.size(); i++) {
                Entity entity = candidates.get(i);
                ICullable state = (ICullable) entity;
                // Coming back into view retires the old verdict rather than waiting out the timer:
                // it was measured against whatever was in the way from the previous angle, and the
                // camera no longer has that angle. Clearing it to visible is the safe direction —
                // the entity draws for one frame until the fresh probe lands.
                boolean reentered = discard || !state.fpsmaster$wasInFrustum();
                state.fpsmaster$setInFrustum(true);
                if (reentered) {
                    state.fpsmaster$setOccluded(false);
                }
                if (state.fpsmaster$isQueryPending()
                        || (!reentered && now - state.fpsmaster$getLastProbeMillis() < interval)) {
                    continue;
                }

                AxisAlignedBB box = entity.getEntityBoundingBox().expand(
                        PROBE_EXPANSION, PROBE_EXPANSION, PROBE_EXPANSION);
                if (entity instanceof EntityPlayer) {
                    box = box.addCoord(0.0d, NAMEPLATE_HEADROOM, 0.0d);
                }
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
            windowHarvested++;
            if (occluded) {
                windowOccluded++;
            }
            if (windowHarvested >= RATE_WINDOW) {
                double fraction = windowOccluded / (double) windowHarvested;
                // Hysteresis: below the lower bound the probes are paying for nothing and this
                // drops back to scouting; above the upper bound they are earning and it culls.
                // Between the two the current state stands, so a scene sitting on the boundary
                // does not flip every window and make everything behind a wall blink.
                // Hidden entities per sweep, which is what the culling actually saves. The window
                // spans several sweeps, so the total is scaled back to one.
                double perSweep = windowOccluded / Math.max(1.0d, windowSweeps);
                if (fraction <= IDLE_FRACTION || perSweep < CULL_FLOOR) {
                    scouting = true;
                } else if (fraction >= BUSY_FRACTION && perSweep >= CULL_FLOOR && scouting) {
                    scouting = false;
                    discardVerdicts = true;
                }
                windowHarvested = 0;
                windowOccluded = 0;
                windowSweeps = 0;
            }
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
            state.fpsmaster$setInFrustum(false);
            freeQueries.addLast(state.fpsmaster$getQueryId());
        }
        pendingProbes.clear();
        candidates.clear();
        scouting = true;
        discardVerdicts = false;
        lastSweepMillis = 0L;
        windowHarvested = 0;
        windowOccluded = 0;
        windowSweeps = 0;
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
