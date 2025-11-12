package top.fpsmaster.api.provider.render;

import top.fpsmaster.api.provider.IProvider;

public interface IRenderManagerProvider extends IProvider {
    double renderPosX();
    double renderPosY();
    double renderPosZ();
}