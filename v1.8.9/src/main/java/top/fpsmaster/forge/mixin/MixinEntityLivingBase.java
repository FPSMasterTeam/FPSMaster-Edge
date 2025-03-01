package top.fpsmaster.forge.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.render.DamageIndicator;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity {
    @Shadow
    public abstract IAttributeInstance getEntityAttribute(IAttribute attribute);

    @Shadow
    public abstract float getHealth();

    @Inject(method = "damageEntity", at = @At(value = "INVOKE",target = "Lnet/minecraft/entity/EntityLivingBase;setHealth(F)V", shift = At.Shift.BEFORE))
    protected void damageEntity(DamageSource damageSrc, float damageAmount, CallbackInfo ci) {
        EntityLivingBase entity = (EntityLivingBase) ((Object) this);
        BlockPos position = entity.getPosition();
        if (damageAmount > entity.getHealth()){
            damageAmount = entity.getHealth();
        }
        DamageIndicator.addIndicator(position.getX(), position.getY(), position.getZ(), damageAmount);
    }

    @Inject(method = "heal", at = @At(value = "INVOKE",target = "Lnet/minecraft/entity/EntityLivingBase;setHealth(F)V", shift = At.Shift.BEFORE))
    public void heal(float healAmount, CallbackInfo ci) {
        EntityLivingBase entity = (EntityLivingBase) ((Object) this);
        BlockPos position = entity.getPosition();
        if (healAmount > entity.getMaxHealth() - entity.getHealth()) {
            healAmount = entity.getMaxHealth() - entity.getHealth();
        }
        DamageIndicator.addIndicator(position.getX(), position.getY(), position.getZ(), -healAmount);
    }

}
