package top.fpsmaster.api.wrapper;

public interface SoundWrap {
    void playLightning(double posX, double posY, double posZ, float i, float v, boolean b);
    void playExplosion(double posX, double posY, double posZ, float i, float v, boolean b);
    void playRedStoneBreak(double posX, double posY, double posZ, float i, float v, boolean b);
}