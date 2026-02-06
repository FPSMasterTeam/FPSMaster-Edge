package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.render.CleanView;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity {
    @Shadow
    public abstract IAttributeInstance getEntityAttribute(IAttribute attribute);

    @SuppressWarnings("all")
    @Inject(method = "updatePotionEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/DataWatcher;getWatchableObjectInt(I)I"), cancellable = true)
    public void updatePotionEffects(CallbackInfo ci) {
        if (FPSMaster.moduleManager.getModule(CleanView.class).isEnabled() && (Object) this == Minecraft.getMinecraft().thePlayer) {
            ci.cancel();
        }
    }
}



