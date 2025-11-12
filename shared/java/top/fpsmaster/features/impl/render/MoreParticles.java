package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.wrapper.WrapperEntityLightningBolt;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;

import static top.fpsmaster.utils.Utility.mc;

public class MoreParticles extends Module {
    private Entity target = null;
    private Entity lastEffect = null;

    public static NumberSetting sharpness = new NumberSetting("Sharpness", 2, 0, 30, 1);
    public static BooleanSetting alwaysSharpness = new BooleanSetting("AlwaysSharpness", false);
    public static NumberSetting crit = new NumberSetting("Crit", 2, 0, 30, 1);
    public static BooleanSetting alwaysCrit = new BooleanSetting("AlwaysCrit", false);
    public static ModeSetting special = new ModeSetting("Special", 0, "None", "Heart", "Flame", "Blood");
    public static ModeSetting killEffect = new ModeSetting("killEffect", 0, "None", "Lightning", "Explosion");

    public MoreParticles() {
        super("MoreParticles", Category.RENDER);
        addSettings(sharpness, alwaysSharpness, crit, alwaysCrit, special, killEffect);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!(target instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase entityLivingBase = (EntityLivingBase) target;
        if (lastEffect != target && (entityLivingBase.getHealth() <= 0 || !entityLivingBase.isEntityAlive())) {
            if (killEffect.getValue() == 1) {
                Wrappers.world().addWeatherEffect(
                        new WrapperEntityLightningBolt(
                                Wrappers.world().getWorld(),
                                entityLivingBase.posX,
                                entityLivingBase.posY,
                                entityLivingBase.posZ,
                                false
                        )
                );
                Wrappers.sound().playExplosion(
                        entityLivingBase.posX,
                        entityLivingBase.posY,
                        entityLivingBase.posZ,
                        1f,
                        1.0f,
                        false
                );
            } else if (killEffect.getValue() == 2) {
                Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.EXPLOSION_LARGE);
                Wrappers.sound().playExplosion(
                        entityLivingBase.posX,
                        entityLivingBase.posY,
                        entityLivingBase.posZ,
                        1f,
                        1.0f,
                        false
                );
            }
            lastEffect = target;
            target = null;
        }
    }

    @Subscribe
    public void onAttack(EventAttack event) {
        if (event.target.isEntityAlive()) {
            target = event.target;
            if (Wrappers.minecraft().getPlayer().fallDistance != 0f || alwaysCrit.getValue()) {
                for (int i = 0; i < crit.getValue().intValue(); i++) {
                    Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.CRIT);
                }
            }
            boolean needSharpness = false;
            if (Wrappers.minecraft().getPlayer().inventory.getCurrentItem() != null) {
                needSharpness = Wrappers.minecraft().getPlayer().inventory.getCurrentItem().getEnchantmentTagList() != null
                        && !Wrappers.minecraft().getPlayer().inventory.getCurrentItem().getEnchantmentTagList().hasNoTags()
                        && Wrappers.minecraft().getPlayer().inventory.getCurrentItem().getEnchantmentTagList().toString().contains("id:16s");
            }
            if (needSharpness || alwaysSharpness.getValue()) {
                for (int i = 0; i < sharpness.getValue().intValue(); i++) {
                    Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.CRIT_MAGIC);
                }
            }
            if (special.getValue() == 1) {
                Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.HEART);
            } else if (special.getValue() == 2) {
                Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.FLAME);
            } else if (special.getValue() == 3) {
                if (mc.objectMouseOver.hitVec != null && event.target.hurtResistantTime <= 10) {
                    System.out.println();
                    Wrappers.sound().playRedStoneBreak(
                            mc.objectMouseOver.hitVec.xCoord,
                            mc.objectMouseOver.hitVec.yCoord,
                            mc.objectMouseOver.hitVec.zCoord,
                            1f,
                            1f,
                            true
                    );
                    Wrappers.effects().addRedStoneBreak(new WrapperBlockPos(new BlockPos(mc.objectMouseOver.hitVec)));
                }
            }
        }
    }
}
