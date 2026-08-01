package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * How many mipmap levels the atlas actually has.
 *
 * <p>Not the same as the game's Mipmap Levels setting: {@code loadTextureAtlas} lowers it when the
 * stitched atlas is not a large enough power of two, and clamping to the real number is the
 * difference between a valid LOD range and one the driver rejects.
 */
@Mixin(TextureMap.class)
public interface TextureMapAccessor {

    @Accessor("mipmapLevels")
    int fpsmaster$getMipmapLevels();
}
