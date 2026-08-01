package top.fpsmaster.utils.render;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Display lists for item models, which are rebuilt from scratch every time one is drawn.
 *
 * <p>{@code RenderItem.renderModel} opens a {@code WorldRenderer}, copies every quad of the model
 * into it and draws — per item, per entity, per frame. The geometry does not change between those
 * frames. On a scene of 103 entities each holding a sword the held-item layer costs 433us, which is
 * 59% of everything the layer renderers do and <b>2.3 times what four pieces of armour cost</b>: one
 * held item is 4.2us against 0.46us for an armour piece. A sword is expensive because 1.8.9 extrudes
 * a flat sprite into a solid — front face, back face, and a rim quad for every texel step around the
 * silhouette.
 *
 * <p>Recording that once and replaying it with {@code glCallList} keeps the same geometry, the same
 * texture and the same transforms — those are applied to the matrix stack outside the list, so one
 * recording serves every entity holding that item, at any position.
 *
 * <h2>Tint is part of the key, not a reason to give up</h2>
 *
 * <p>The first version refused to cache anything carrying a tint index, and so cached <b>nothing</b>:
 * {@code ItemModelGenerator} writes the layer number into {@code tintIndex}, so every quad of every
 * flat item model — every sword, every tool — reports a tint. The counter read zero recordings,
 * which is the only reason that was not mistaken for the optimisation being worth nothing.
 *
 * <p>A tint does not mean the colour varies. {@code getColorFromItemStack} answers white for a
 * diamond sword and the dye colour for leather armour; what matters is the answer, not the question.
 * So the resolved colours are part of the key. The tint indices a model uses are found once; after
 * that a draw resolves one or two colours and looks the list up by them.
 *
 * <p>A model that accumulates more than {@link #MAX_TINTS_PER_MODEL} distinct colours is dropped
 * instead — dyed leather has sixteen million and would otherwise fill the driver with lists.
 *
 * <p><b>The glint pass is left alone.</b> It draws the same model again under a scrolling texture
 * matrix with its own colour; a different question.
 */
public final class ItemModelLists {

    /**
     * Keyed by model identity, not equality.
     *
     * <p>Baked models are shared singletons owned by the model manager, and identity is what makes
     * the lookup a pointer compare rather than a structural walk of the quad lists — which would
     * cost more than the draw it is trying to save.
     */
    private static final Map<IBakedModel, Map<Long, Integer>> LISTS =
            new IdentityHashMap<IBakedModel, Map<Long, Integer>>();

    /** Models examined and rejected, so the reason is established once rather than every frame. */
    private static final Map<IBakedModel, Boolean> REJECTED = new IdentityHashMap<IBakedModel, Boolean>();

    /** The tint indices each model actually uses, so a draw resolves only those. */
    private static final Map<IBakedModel, int[]> TINTS = new IdentityHashMap<IBakedModel, int[]>();

    /** Distinct colours one model may be cached under before it is dropped instead. */
    private static final int MAX_TINTS_PER_MODEL = 8;

    private ItemModelLists() {
    }

    /**
     * The list for this model, or 0 if it has none yet and cannot get one now.
     *
     * <p>Returns 0 rather than recording, because recording has to happen around the caller's own
     * submission — see {@link #beginRecording}.
     */
    public static int lookup(IBakedModel model, ItemStack stack) {
        Map<Long, Integer> byColour = LISTS.get(model);
        if (byColour == null) {
            return 0;
        }
        Integer list = byColour.get(Long.valueOf(signature(model, stack)));
        return list == null ? 0 : list.intValue();
    }

    /** Whether this model was already examined and found unfit to cache. */
    public static boolean rejected(IBakedModel model) {
        return REJECTED.containsKey(model);
    }

    /** True while a list is open, so the close can be skipped when the open was refused. */
    private static boolean recording;

    public static boolean recording() {
        return recording;
    }

    /**
     * Opens a list for the model, or returns 0 if the model must not be cached.
     *
     * <p>{@code GL_COMPILE_AND_EXECUTE}, not {@code GL_COMPILE}: the caller's draw happens inside
     * the recording, and under plain {@code GL_COMPILE} the driver captures it without performing
     * it — so the frame an item is first seen, it would not be drawn. One invisible frame per new
     * item is exactly the sort of fault that hides in a benchmark and shows up in a player's hand.
     */
    public static int beginRecording(IBakedModel model, ItemStack stack) {
        Map<Long, Integer> byColour = LISTS.get(model);
        if (byColour == null) {
            byColour = new HashMap<Long, Integer>(4);
            LISTS.put(model, byColour);
        }
        if (byColour.size() >= MAX_TINTS_PER_MODEL) {
            REJECTED.put(model, Boolean.TRUE);
            return 0;
        }
        int list = GLAllocation.generateDisplayLists(1);
        if (list == 0) {
            REJECTED.put(model, Boolean.TRUE);
            return 0;
        }
        byColour.put(Long.valueOf(signature(model, stack)), Integer.valueOf(list));
        GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE);
        recording = true;
        return list;
    }

    public static void endRecording() {
        recording = false;
        GL11.glEndList();
    }

    /**
     * Drops every list, for a resource reload.
     *
     * <p>A reload rebuilds the baked models, so the keys are dead and the geometry behind them may
     * have changed with the resource pack. Keeping the lists would draw the old pack's items.
     */
    public static void invalidate() {
        for (Map<Long, Integer> byColour : LISTS.values()) {
            for (Integer list : byColour.values()) {
                GLAllocation.deleteDisplayLists(list.intValue());
            }
        }
        LISTS.clear();
        REJECTED.clear();
        TINTS.clear();
        recording = false;
    }

    /**
     * The colours this stack resolves the model's tints to, packed into one key.
     *
     * <p>Zero when nothing can vary — no stack, or a model with no tints — so the common case never
     * calls into the item at all. Otherwise one call per distinct tint index, and models use one or
     * two of them.
     */
    private static long signature(IBakedModel model, ItemStack stack) {
        if (stack == null) {
            return 0L;
        }
        int[] tints = TINTS.get(model);
        if (tints == null) {
            tints = collectTints(model);
            TINTS.put(model, tints);
        }
        long signature = 0L;
        for (int i = 0; i < tints.length; i++) {
            int colour = stack.getItem().getColorFromItemStack(stack, tints[i]);
            signature = signature * 31L + (colour & 0xFFFFFFFFL);
        }
        return signature;
    }

    /** The distinct tint indices the model's quads carry. Walked once per model. */
    private static int[] collectTints(IBakedModel model) {
        Set<Integer> found = new TreeSet<Integer>();
        for (EnumFacing facing : EnumFacing.values()) {
            collectTints(model.getFaceQuads(facing), found);
        }
        collectTints(model.getGeneralQuads(), found);
        int[] tints = new int[found.size()];
        int at = 0;
        for (Integer tint : found) {
            tints[at++] = tint.intValue();
        }
        return tints;
    }

    private static void collectTints(List<BakedQuad> quads, Set<Integer> found) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            if (quad.hasTintIndex()) {
                found.add(Integer.valueOf(quad.getTintIndex()));
            }
        }
    }
}
