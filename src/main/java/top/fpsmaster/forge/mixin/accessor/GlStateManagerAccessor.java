package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor {
    @Accessor("colorState")
    static GlStateManager.Color getColorState() {
//        throw new UnsupportedOperationException("Mixin failed to inject!");
        throw new AssertionError();
    }

    @Accessor("textureState")
    static GlStateManager.TextureState[] getTextureState() {
//        throw new UnsupportedOperationException("Mixin failed to inject!");
        throw new AssertionError();
    }

    @Accessor("activeTextureUnit")
    static int getActiveTextureUnit() {
//        throw new UnsupportedOperationException("Mixin failed to inject!");
        throw new AssertionError();
    }
}



