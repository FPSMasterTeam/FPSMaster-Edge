package top.fpsmaster.websocket.data.message.client;

import top.fpsmaster.websocket.data.message.Packet;
import top.fpsmaster.websocket.data.message.PacketType;

public class FetchPlayerPacket extends Packet {
    public String uuid;
    public String gameId;

    public FetchPlayerPacket(String uuid, String gameId) {
        super(PacketType.CLIENT_FETCH_PLAYER);
        this.uuid = uuid;
        this.gameId = gameId;
    }
}
