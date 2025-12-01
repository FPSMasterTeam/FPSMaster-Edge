package top.fpsmaster.impl.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.api.domain.sound.ISoundAPI;

public class SoundAPIImpl implements ISoundAPI {
    
    @Override
    public void playLightningSound(double x, double y, double z, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            new PositionedSoundRecord(new ResourceLocation("ambient.weather.thunder"), 
            volume, pitch, (float) x, (float) y, (float) z)
        );
    }
    
    @Override
    public void playExplosionSound(double x, double y, double z, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            new PositionedSoundRecord(new ResourceLocation("random.explode"), 
            volume, pitch, (float) x, (float) y, (float) z)
        );
    }
    
    @Override
    public void playRedstoneBreakSound(double x, double y, double z, float volume, float pitch) {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            new PositionedSoundRecord(new ResourceLocation("dig.stone"), 
            volume, pitch, (float) x, (float) y, (float) z)
        );
    }
}
