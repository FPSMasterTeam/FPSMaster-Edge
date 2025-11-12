package top.fpsmaster.wrapper.sound;

import net.minecraft.init.SoundEvents;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.api.wrapper.SoundWrap;

public class SoundProvider implements SoundWrap {
    public void playLightning(double posX, double posY, double posZ, float i, float v, boolean b) {
        ProviderManager.mcProvider.getWorld().playSound(posX, posY, posZ, SoundEvents.ENTITY_LIGHTNING_IMPACT, net.minecraft.util.SoundCategory.BLOCKS, i, v, b);
    }
    public void playExplosion(double posX, double posY, double posZ, float i, float v, boolean b) {
        ProviderManager.mcProvider.getWorld().playSound(posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, net.minecraft.util.SoundCategory.BLOCKS, i, v, b);
    }
    public void playRedStoneBreak(double posX, double posY, double posZ, float i, float v, boolean b) {
        ProviderManager.mcProvider.getWorld().playSound(posX, posY, posZ, SoundEvents.BLOCK_STONE_BREAK, net.minecraft.util.SoundCategory.BLOCKS, i, v, b);
    }
}
