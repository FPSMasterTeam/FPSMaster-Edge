package top.fpsmaster.api.provider.sound;

import top.fpsmaster.api.provider.IProvider;

public interface ISoundProvider extends IProvider {
    void playLightning(double posX, double posY, double posZ, float i, float v, boolean b);
    void playExplosion(double posX, double posY, double posZ, float i, float v, boolean b);
    void playRedStoneBreak(double posX, double posY, double posZ, float i, float v, boolean b);
}