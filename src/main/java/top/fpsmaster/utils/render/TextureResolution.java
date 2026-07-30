package top.fpsmaster.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.mixin.accessor.TextureMapAccessor;
import top.fpsmaster.modules.logger.ClientLogger;

import static top.fpsmaster.utils.core.Utility.mc;

/**
 * Forces the block atlas to be sampled from a coarser mipmap level than the one the pack supplies.
 *
 * <p>The obvious way to lower texture resolution is to downscale the images, and it does not work
 * here: {@code TextureAtlasSprite} reads the number of animation frames out of the image's own
 * height, so an atlas built from resized images renumbers every animated sprite, and intercepting
 * the image load catches the GUI, the font and the lightmap along with the blocks.
 *
 * <p>Sampling is the lever that avoids all of that. {@code GL_TEXTURE_MIN_LOD} clamps the level of
 * detail before the magnification decision is made, so a floor of 2 means even a block pressed
 * against the camera is drawn from mip level 2 — a quarter of the resolution in each axis. Nothing
 * about the atlas changes: the same images, the same stitching, the same frame layout, the same
 * texture object. Only the sample the fragment shader takes moves.
 *
 * <p>And it can only touch the blocks. The atlas is the one texture vanilla allocates with a mipmap
 * chain; the GUI, the font, the lightmap and every dynamic texture allocate with a single level, so
 * they are out of reach of a mipmap setting by construction rather than by a filter that has to be
 * kept correct.
 *
 * <p>The corollary is that this does nothing when the game's own Mipmap Levels video setting is off,
 * because then the atlas has no coarser level to fall back to.
 */
public final class TextureResolution {

    private TextureResolution() {
    }

    /**
     * Writes the current setting onto the atlas. Safe to call whenever: the parameter lives on the
     * texture object, and vanilla only rewrites it when it reallocates the atlas.
     */
    public static void apply() {
        if (mc == null) {
            return;
        }
        TextureMap atlas = mc.getTextureMapBlocks();
        if (atlas == null) {
            return;
        }
        int requested = Performance.using ? Performance.textureResolution.getMode() : 0;
        int available = ((TextureMapAccessor) atlas).fpsmaster$getMipmapLevels();
        if (requested > available) {
            // Clamped rather than ignored, so picking Sixteenth with two mip levels still gets the
            // two. Worth saying out loud: with Mipmap Levels off there is no chain at all and the
            // setting silently does nothing, which is otherwise indistinguishable from a broken one.
            ClientLogger.warn("TextureResolution asked for mip level " + requested
                    + " but the block atlas only has " + available
                    + "; raise the game's Mipmap Levels setting for the rest of the effect");
        }
        int level = Math.min(requested, available);
        GlStateManager.bindTexture(atlas.getGlTextureId());
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, (float) level);
    }
}
