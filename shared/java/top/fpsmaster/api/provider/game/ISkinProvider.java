package top.fpsmaster.api.provider.game;

import top.fpsmaster.api.provider.IProvider;

public interface ISkinProvider extends IProvider {
    void updateSkin(String name, String uuid, String skin);
}