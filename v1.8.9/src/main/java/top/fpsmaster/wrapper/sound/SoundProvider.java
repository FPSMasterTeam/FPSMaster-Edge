package top.fpsmaster.wrapper.sound;

import top.fpsmaster.api.Wrappers;
import top.fpsmaster.api.wrapper.SoundWrap;

public class SoundProvider implements SoundWrap {
    public void playLightning(double posX, double posY, double posZ, float i, float v, boolean b) {
        Wrappers.minecraft().getWorld().playSound(posX, posY, posZ, "lightning", i, v, b);
    }

    public void playExplosion(double posX, double posY, double posZ, float i, float v, boolean b) {
        Wrappers.minecraft().getWorld().playSound(posX, posY, posZ, "explosion", i, v, b);
    }

    public void playRedStoneBreak(double posX, double posY, double posZ, float i, float v, boolean b) {
        Wrappers.minecraft().getWorld().playSound(posX, posY, posZ, "dig.stone", i, v, b);
    }
}
