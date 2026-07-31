package top.fpsmaster.forge.mixin;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.utils.render.ItemModelLists;

/**
 * Drops the cached item display lists when the models behind them are rebuilt.
 *
 * <p>A resource reload bakes new models, so every key in the cache is dead and the geometry may have
 * changed with the pack. Without this, switching resource packs would keep drawing the old pack's
 * items from lists nothing can reach any more — and the display lists themselves would leak, one set
 * per reload, for the life of the session.
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin_InvalidateItemLists {

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void edge$dropItemLists(IResourceManager manager, CallbackInfo ci) {
        ItemModelLists.invalidate();
    }
}
