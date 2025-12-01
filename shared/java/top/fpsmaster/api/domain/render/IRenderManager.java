package top.fpsmaster.api.domain.render;

/**
 * 渲染管理器接口
 */
public interface IRenderManager {
    
    /**
     * 获取渲染位置 X
     * 
     * @return 渲染偏移 X 坐标
     */
    double getRenderPosX();
    
    /**
     * 获取渲染位置 Y
     * 
     * @return 渲染偏移 Y 坐标
     */
    double getRenderPosY();
    
    /**
     * 获取渲染位置 Z
     * 
     * @return 渲染偏移 Z 坐标
     */
    double getRenderPosZ();
}
