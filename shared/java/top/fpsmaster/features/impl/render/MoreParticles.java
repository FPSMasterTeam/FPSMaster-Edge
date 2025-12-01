package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventAttack;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.api.MinecraftAPI;
import top.fpsmaster.api.model.BlockPos;
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
                // Use raw world for lightning effect
                net.minecraft.client.multiplayer.WorldClient rawWorld = 
                    (net.minecraft.client.multiplayer.WorldClient) MinecraftAPI.world().getWorld().getRawWorld();
                rawWorld.addWeatherEffect(
                    new net.minecraft.entity.effect.EntityLightningBolt(
                        rawWorld,
                        entityLivingBase.posX,
                        entityLivingBase.posY,
                        entityLivingBase.posZ
                    )
                );
                MinecraftAPI.sound().playExplosionSound(
                    entityLivingBase.posX,
                    entityLivingBase.posY,
                    entityLivingBase.posZ,
                    1f,
                    1.0f
                );
            } else if (killEffect.getValue() == 2) {
                Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.EXPLOSION_LARGE);
                MinecraftAPI.sound().playExplosionSound(
                    entityLivingBase.posX,
                    entityLivingBase.posY,
                    entityLivingBase.posZ,
                    1f,
                    1.0f
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
            if (MinecraftAPI.client().getPlayer().getFallDistance() != 0f || alwaysCrit.getValue()) {
                for (int i = 0; i < crit.getValue().intValue(); i++) {
                    Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.CRIT);
                }
            }
            boolean needSharpness = false;
            net.minecraft.entity.player.EntityPlayer rawPlayer = 
                (net.minecraft.entity.player.EntityPlayer) MinecraftAPI.client().getPlayer().getRawPlayer();
            if (rawPlayer.inventory.getCurrentItem() != null) {
                needSharpness = rawPlayer.inventory.getCurrentItem().getEnchantmentTagList() != null
                        && !rawPlayer.inventory.getCurrentItem().getEnchantmentTagList().hasNoTags()
                        && rawPlayer.inventory.getCurrentItem().getEnchantmentTagList().toString().contains("id:16s");
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
                    MinecraftAPI.sound().playRedstoneBreakSound(
                            mc.objectMouseOver.hitVec.xCoord,
                            mc.objectMouseOver.hitVec.yCoord,
                            mc.objectMouseOver.hitVec.zCoord,
                            1f,
                            1f
                    );
                    MinecraftAPI.render().getParticleManager().addRedstoneBreakParticle(
                        new BlockPos(
                            (int)mc.objectMouseOver.hitVec.xCoord,
                            (int)mc.objectMouseOver.hitVec.yCoord,
                            (int)mc.objectMouseOver.hitVec.zCoord
                        )
                    );
                }
            }
        }
    }
}
