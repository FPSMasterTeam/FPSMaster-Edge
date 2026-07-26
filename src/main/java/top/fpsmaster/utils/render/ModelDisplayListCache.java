package top.fpsmaster.utils.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches an entity model's geometry as one display list, keyed by its pose.
 *
 * <p>Vanilla renders a model box by box: each one issues a matrix push, a translate, up to three
 * rotates, a {@code glCallList} and a pop. An armour stand is about a dozen boxes, which measured at
 * 8.66us per entity — roughly 2200 cycles per box for a translate, a rotate and a list call. The
 * geometry is trivial; the cost is the call sequence around it.
 *
 * <p>For an entity that is not animating, that entire sequence produces identical output every
 * frame. Recording it once into a single list turns a dozen calls and some sixty matrix operations
 * into one call.
 *
 * <h3>When this is not safe, and how that is handled</h3>
 *
 * <p><b>A pose that changes cannot be cached.</b> The key is every box's rotation, pivot and
 * visibility, so a walking mob keys differently each frame. Left alone that would compile a fresh
 * list per frame and leak them, so a model whose pose keeps changing is marked uncacheable and
 * rendered normally from then on. Hashing is far cheaper than the render it replaces, so paying it
 * on entities that never benefit is still a win overall.
 *
 * <p><b>The list holds geometry only.</b> It brackets the call to {@code ModelBase.render} inside
 * {@code renderModel}, which is after the texture bind and outside the colour and depth-mask
 * handling. Nothing that varies independently of the pose is baked in.
 *
 * <p><b>A pose is only compiled the second time it is seen.</b> {@code ModelRenderer.render} builds
 * each box's own display list on first use, and opening a list inside a list is
 * {@code GL_INVALID_OPERATION} — which would not throw, it would corrupt the recording. Waiting one
 * sighting guarantees every box is already compiled before the outer list opens.
 */
public final class ModelDisplayListCache {

    /**
     * Distinct poses kept per model. A handful covers the idle and a few common stances; beyond
     * that the model is animating and the cache is the wrong tool.
     */
    private static final int MAX_POSES_PER_MODEL = 8;

    /** Pose changes tolerated before a model is judged to be animating. */
    private static final int ANIMATION_THRESHOLD = 16;

    private static final class ModelEntry {
        final Map<Long, Integer> listsByPose = new HashMap<Long, Integer>();
        /** Poses seen once but not yet compiled. See the note on nested display lists. */
        final java.util.Set<Long> seenOnce = new java.util.HashSet<Long>();
        long lastPose = Long.MIN_VALUE;
        int poseChanges;
        boolean animating;
    }

    private static final Map<Class<?>, ModelEntry> MODELS = new HashMap<Class<?>, ModelEntry>();

    private static int compilingList;
    private static long compilingPose;
    private static ModelEntry compilingEntry;

    private ModelDisplayListCache() {
    }

    /**
     * Replays a cached list for this pose if there is one.
     *
     * @return true when the caller should skip the model's own rendering
     */
    public static boolean replay(ModelBase model, float scale) {
        ModelEntry entry = MODELS.get(model.getClass());
        if (entry == null) {
            entry = new ModelEntry();
            MODELS.put(model.getClass(), entry);
        }
        if (entry.animating) {
            return false;
        }

        long pose = poseKey(model, scale);
        if (pose != entry.lastPose) {
            entry.lastPose = pose;
            if (++entry.poseChanges > ANIMATION_THRESHOLD) {
                // Compiling a list per frame would cost more than it saves and would grow without
                // bound, so this model opts out permanently.
                entry.animating = true;
                releaseAll(entry);
                return false;
            }
        }

        Integer list = entry.listsByPose.get(Long.valueOf(pose));
        if (list != null) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.modelListHits++;
            }
            GlStateManager.callList(list.intValue());
            return true;
        }

        if (entry.seenOnce.add(Long.valueOf(pose))) {
            // First sighting: let vanilla render so each box compiles its own list. Opening the
            // outer list now would nest one glNewList inside another.
            return false;
        }

        if (entry.listsByPose.size() >= MAX_POSES_PER_MODEL) {
            entry.animating = true;
            releaseAll(entry);
            return false;
        }

        compilingEntry = entry;
        compilingPose = pose;
        compilingList = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(compilingList, GL11.GL_COMPILE_AND_EXECUTE);
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.modelListMisses++;
        }
        return false;
    }

    /** Closes a compile started by {@link #replay}. Must be called after the model has rendered. */
    public static void endCompile() {
        if (compilingList == 0) {
            return;
        }
        GL11.glEndList();
        compilingEntry.listsByPose.put(Long.valueOf(compilingPose), Integer.valueOf(compilingList));
        compilingList = 0;
        compilingEntry = null;
    }

    public static boolean isCompiling() {
        return compilingList != 0;
    }

    private static void releaseAll(ModelEntry entry) {
        for (Integer list : entry.listsByPose.values()) {
            GLAllocation.deleteDisplayLists(list.intValue());
        }
        entry.listsByPose.clear();
        entry.seenOnce.clear();
    }

    /**
     * Drops every cached list.
     *
     * <p>Needed when anything that changes what a box compiles into its own list changes — model
     * batching being toggled is the case that exists today — because the outer list would otherwise
     * keep replaying geometry built under the old setting.
     */
    public static void invalidateAll() {
        for (ModelEntry entry : MODELS.values()) {
            releaseAll(entry);
            entry.animating = false;
            entry.poseChanges = 0;
            entry.lastPose = Long.MIN_VALUE;
        }
    }

    /**
     * Hashes every box's pose.
     *
     * <p>Covers rotation, pivot, visibility and the scale argument — everything the vanilla render
     * path reads when deciding what matrix work to emit. Anything omitted here would let two
     * genuinely different poses share a list, which is a rendering bug, so the check is deliberately
     * broad rather than clever.
     */
    private static long poseKey(ModelBase model, float scale) {
        List<?> boxes = model.boxList;
        long hash = Float.floatToRawIntBits(scale);
        for (int i = 0; i < boxes.size(); i++) {
            Object box = boxes.get(i);
            if (!(box instanceof ModelRenderer)) {
                continue;
            }
            ModelRenderer renderer = (ModelRenderer) box;
            hash = hash * 31 + Float.floatToRawIntBits(renderer.rotateAngleX);
            hash = hash * 31 + Float.floatToRawIntBits(renderer.rotateAngleY);
            hash = hash * 31 + Float.floatToRawIntBits(renderer.rotateAngleZ);
            hash = hash * 31 + Float.floatToRawIntBits(renderer.rotationPointX);
            hash = hash * 31 + Float.floatToRawIntBits(renderer.rotationPointY);
            hash = hash * 31 + Float.floatToRawIntBits(renderer.rotationPointZ);
            hash = hash * 31 + (renderer.showModel ? 1 : 0) + (renderer.isHidden ? 2 : 0);
        }
        return hash;
    }
}
