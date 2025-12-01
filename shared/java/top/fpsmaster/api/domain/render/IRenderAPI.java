package top.fpsmaster.api.domain.render;

/**
 * 渲染 API
 * 提供渲染相关的所有功能
 */
public interface IRenderAPI {
    
    /**
     * 获取渲染管理器
     * 
     * @return 渲染管理器实例
     */
    IRenderManager getRenderManager();
    
    /**
     * 获取渲染部分刻
     * 
     * @return 部分刻值 (0.0 - 1.0)
     */
    float getPartialTicks();
    
    /**
     * 获取粒子管理器
     * 
     * @return 粒子管理器实例
     */
    IParticleManager getParticleManager();
}
