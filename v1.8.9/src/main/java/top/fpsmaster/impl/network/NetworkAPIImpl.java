package top.fpsmaster.impl.network;

import top.fpsmaster.api.domain.network.IChatPacketHandler;
import top.fpsmaster.api.domain.network.INetworkAPI;
import top.fpsmaster.api.domain.network.IPlayerListPacketHandler;
import top.fpsmaster.api.domain.network.ITimeUpdatePacketHandler;

public class NetworkAPIImpl implements INetworkAPI {
    
    @Override
    public IChatPacketHandler getChatPacketHandler() {
        return new ChatPacketHandlerImpl();
    }
    
    @Override
    public IPlayerListPacketHandler getPlayerListPacketHandler() {
        return new PlayerListPacketHandlerImpl();
    }
    
    @Override
    public ITimeUpdatePacketHandler getTimeUpdatePacketHandler() {
        return new TimeUpdatePacketHandlerImpl();
    }
}
