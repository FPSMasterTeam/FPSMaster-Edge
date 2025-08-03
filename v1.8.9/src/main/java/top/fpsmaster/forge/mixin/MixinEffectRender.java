package top.fpsmaster.forge.mixin;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.utility.ParticlesModifier;

import java.util.List;

@Mixin(EffectRenderer.class)
public class MixinEffectRender {
    @Shadow
    private List<EntityFX>[][] fxLayers = new List[4][];

    @Inject(method = "addEffect", at=@At("HEAD"), cancellable = true)
    public void add(EntityFX effect, CallbackInfo ci){
        if (ParticlesModifier.using){
            if (effect instanceof EntityDiggingFX){
                if (ParticlesModifier.block.getValue()){
                    addEff(effect);
                }
                ci.cancel();
            }
        }
    }

    @Unique
    public void addEff(EntityFX effect){
        int i = effect.getFXLayer();
        int j = effect.getAlpha() != 1.0F ? 0 : 1;
        if (this.fxLayers[i][j].size() >= 4000) {
            this.fxLayers[i][j].remove(0);
        }

        this.fxLayers[i][j].add(effect);
    }
}
