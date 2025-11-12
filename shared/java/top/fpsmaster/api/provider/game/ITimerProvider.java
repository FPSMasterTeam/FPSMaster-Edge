package top.fpsmaster.api.provider.game;

import top.fpsmaster.api.provider.IProvider;

public interface ITimerProvider extends IProvider {
    float getRenderPartialTicks();
}