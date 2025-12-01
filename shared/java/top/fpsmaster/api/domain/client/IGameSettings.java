package top.fpsmaster.api.domain.client;

/**
 * 游戏设置接口
 * 注意：不直接暴露 KeyBinding 等 Minecraft 内部类
 */
public interface IGameSettings {
    
    /**
     * 设置按键按下状态（通过按键名称）
     * 
     * @param keyName 按键名称，如 "key.forward", "key.attack" 等
     * @param pressed 是否按下
     */
    void setKeyPressed(String keyName, boolean pressed);
    
    /**
     * 获取视距（区块数）
     * 
     * @return 渲染距离（区块数）
     */
    int getRenderDistance();
    
    /**
     * 设置视距
     * 
     * @param chunks 区块数
     */
    void setRenderDistance(int chunks);
    
    /**
     * 获取鼠标灵敏度 (0.0 - 1.0)
     * 
     * @return 灵敏度值
     */
    float getMouseSensitivity();
    
    /**
     * 设置鼠标灵敏度
     * 
     * @param sensitivity 灵敏度值 (0.0 - 1.0)
     */
    void setMouseSensitivity(float sensitivity);
    
    /**
     * 是否显示调试信息（F3）
     * 
     * @return true 如果显示调试信息
     */
    boolean isShowDebugInfo();
    
    /**
     * 获取 GUI 缩放级别
     * 
     * @return GUI 缩放级别
     */
    int getGuiScale();
    
    /**
     * 获取 FOV 设置值
     * 
     * @return FOV 值
     */
    float getFov();
    
    /**
     * 是否启用 VSync
     * 
     * @return true 如果启用垂直同步
     */
    boolean isVsyncEnabled();
    
    /**
     * 获取帧率限制
     * 
     * @return 帧率限制值
     */
    int getFramerateLimit();
}
