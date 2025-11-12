package top.fpsmaster.api.provider.packets;

import top.fpsmaster.api.provider.IProvider;

public interface IPacket extends IProvider {
    boolean isPacket(Object packet);
}