package top.fpsmaster.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.TerrainProbe;

/**
 * Counts the visible-chunk walk's unit of work.
 *
 * <p>One of these is built for every chunk the walk accepts, each carrying an {@code EnumSet} of
 * its own, and the whole list is thrown away at the end of the frame. Whether that allocation is
 * what the walk costs, or merely what it is easiest to notice about it, is the question the count
 * exists to answer — and answering it before building anything is the lesson of the text cache,
 * where the obvious allocation turned out to be a fifth of the bracket it lived in.
 */
@Mixin(targets = "net.minecraft.client.renderer.RenderGlobal$ContainerLocalRenderInformation")
public class ContainerLocalRenderInformationMixin_Probe {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fpsmaster$count(CallbackInfo ci) {
        if (TerrainProbe.enabled()) {
            TerrainProbe.container();
        }
    }
}
