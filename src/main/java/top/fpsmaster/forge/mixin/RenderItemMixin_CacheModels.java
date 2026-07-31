package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.utils.render.ItemModelLists;

/**
 * Replays an item model from a display list instead of rebuilding it every draw.
 *
 * <p>Priced before it was written, and the answer was not where this project's attention had been.
 * On {@code armor-dense-quick} — 103 stands in full diamond, each holding a sword — deleting the
 * held-item layer takes {@code entityLayers} from 734us to 301us and deleting all the armour takes
 * it to 543us. <b>The held item is 59% of the layer work and the armour is 26%</b>: 4.2us for one
 * sword against 0.46us for a piece of armour, so one held item costs as much as nine armour pieces.
 * All ten segments of the run point the same way.
 *
 * <p>What it spends that on is {@code renderModel}: open a {@code WorldRenderer}, copy every quad of
 * the model into it, draw. Once per item per entity per frame, over geometry that is identical every
 * time. A sword is not a cheap model — 1.8.9 extrudes the flat sprite into a solid, so it carries a
 * front face, a back face and a rim quad for every texel step of the silhouette.
 *
 * <p>The transforms stay outside the list, on the matrix stack, so one recording serves every entity
 * holding that item wherever it stands.
 */
@Mixin(RenderItem.class)
public class RenderItemMixin_CacheModels {

    /**
     * One injection, not two, and the order inside it is the point.
     *
     * <p>Cancelling from a callback does not stop the other callbacks bound to the same injection
     * point — so a separate "start recording" callback at {@code HEAD} would still fire after the
     * replay cancelled, and open a list that nothing ever writes to and nothing ever closes. Both
     * decisions have to be made in one place, in order.
     */
    @Inject(method = "renderModel(Lnet/minecraft/client/resources/model/IBakedModel;ILnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void edge$replayOrRecord(IBakedModel model, int color, ItemStack stack, CallbackInfo ci) {
        if (!Performance.using || !Performance.cacheItemModels.getValue()) {
            return;
        }
        // Only the plain pass. The glint redraws the same model under a scrolling texture matrix
        // with its own colour, and a tinted model resolves its colour from the stack.
        if (color != -1 || ItemModelLists.rejected(model)) {
            return;
        }
        int list = ItemModelLists.lookup(model, stack);
        if (list != 0) {
            GlStateManager.callList(list);
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.itemModelListHits++;
            }
            ci.cancel();
            return;
        }
        if (ItemModelLists.beginRecording(model, stack) != 0 && BenchmarkMode.ACTIVE) {
            BenchCounters.itemModelListsRecorded++;
        }
    }

    /**
     * Closes the list if this call opened one.
     *
     * <p>Guarded on the flag rather than on the same conditions as the head: the settings can change
     * between the two, and an unbalanced {@code glNewList} leaves the driver recording every GL call
     * in the frame.
     */
    @Inject(method = "renderModel(Lnet/minecraft/client/resources/model/IBakedModel;ILnet/minecraft/item/ItemStack;)V",
            at = @At("RETURN"))
    private void edge$closeList(IBakedModel model, int color, ItemStack stack, CallbackInfo ci) {
        if (ItemModelLists.recording()) {
            ItemModelLists.endRecording();
        }
    }
}
