package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.utils.render.TextureResolution;

/**
 * Re-applies the texture resolution clamp after the atlas is rebuilt.
 *
 * <p>Rebuilding reallocates the texture object, and {@code TextureUtil.allocateTextureImpl} writes
 * the LOD range back to its defaults on the way — so a resource pack change, a resolution change or
 * an F3+T would otherwise quietly restore full resolution and leave the setting reading as on.
 */
@Mixin(TextureMap.class)
public class TextureMapMixin_Resolution {

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"))
    private void fpsmaster$applyResolution(IResourceManager resourceManager, CallbackInfo ci) {
        TextureResolution.apply();
    }
}
