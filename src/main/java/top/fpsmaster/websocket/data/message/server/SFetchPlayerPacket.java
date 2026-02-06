package top.fpsmaster.websocket.data.message.server;

import top.fpsmaster.websocket.data.message.Packet;
import top.fpsmaster.websocket.data.message.PacketType;

public class SFetchPlayerPacket extends Packet {
    public String uid;
    public String name;
    public String uuid;
    public String gameId;
    public String cosmetics;
    public String skin;
    public String rank;
    public String customRank;

    public SFetchPlayerPacket(String uid, String name, String uuid, String gameId, String cosmetics, String skin, String rank, String customRank) {
        super(PacketType.SERVER_FETCH_PLAYER);
        this.uid = uid;
        this.name = name;
        this.uuid = uuid;
        this.gameId = gameId;
        this.cosmetics = cosmetics;
        this.skin = skin;
        this.rank = rank;
        this.customRank = customRank;
    }
}
