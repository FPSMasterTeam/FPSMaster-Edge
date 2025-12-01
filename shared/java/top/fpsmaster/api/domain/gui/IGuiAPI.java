package top.fpsmaster.api.domain.gui;

/**
 * GUI API
 * 提供 GUI 相关的所有功能
 */
public interface IGuiAPI {
    
    /**
     * 获取游戏内 GUI 管理器
     * 
     * @return GUI Ingame 实例
     */
    IGuiIngame getGuiIngame();
}
