package top.fpsmaster.api.domain.sound;

/**
 * 音效 API
 * 提供音效相关的所有功能
 */
public interface ISoundAPI {
    
    /**
     * 播放雷电音效
     * 
     * @param x 位置 X
     * @param y 位置 Y
     * @param z 位置 Z
     * @param volume 音量
     * @param pitch 音调
     */
    void playLightningSound(double x, double y, double z, float volume, float pitch);
    
    /**
     * 播放爆炸音效
     * 
     * @param x 位置 X
     * @param y 位置 Y
     * @param z 位置 Z
     * @param volume 音量
     * @param pitch 音调
     */
    void playExplosionSound(double x, double y, double z, float volume, float pitch);
    
    /**
     * 播放红石破碎音效
     * 
     * @param x 位置 X
     * @param y 位置 Y
     * @param z 位置 Z
     * @param volume 音量
     * @param pitch 音调
     */
    void playRedstoneBreakSound(double x, double y, double z, float volume, float pitch);
}
