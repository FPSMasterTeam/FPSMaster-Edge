package top.fpsmaster.api.provider.render;

import top.fpsmaster.api.provider.IProvider;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;

public interface IEffectRendererProvider extends IProvider {
    void addRedStoneBreak(WrapperBlockPos pos);
}