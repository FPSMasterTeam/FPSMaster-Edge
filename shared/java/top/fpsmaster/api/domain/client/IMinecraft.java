package top.fpsmaster.api.domain.client;

/**
 * Minecraft 实例接口
 * 提供 Minecraft 客户端的核心功能
 */
public interface IMinecraft {
    
    /**
     * 是否单人游戏
     * 
     * @return true 如果是单人游戏
     */
    boolean isSingleplayer();
    
    /**
     * 获取帧率限制
     * 
     * @return 帧率限制值
     */
    int getFramerateLimit();
    
    /**
     * 获取渲染部分刻
     * 
     * @return 部分刻值 (0.0 - 1.0)
     */
    float getPartialTicks();
    
    /**
     * 移除左键点击延迟
     */
    void removeLeftClickDelay();
    
    /**
     * 打印聊天消息（使用文本字符串）
     * 
     * @param message 消息文本
     */
    void printChatMessage(String message);
    
    /**
     * 获取字体渲染器（渲染文字用）
     * 注意：返回 Minecraft 原生类型，用于渲染功能
     * 
     * @return FontRenderer 实例
     */
    Object getFontRenderer();
}
