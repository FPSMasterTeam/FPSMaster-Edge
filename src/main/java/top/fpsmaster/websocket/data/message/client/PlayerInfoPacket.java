package top.fpsmaster.websocket.data.message.client;

import com.google.gson.annotations.SerializedName;
import top.fpsmaster.websocket.data.message.Packet;
import top.fpsmaster.websocket.data.message.PacketType;

public class PlayerInfoPacket extends Packet {
    @SerializedName("playerName")
    public String playerName;
    @SerializedName("UUID")
    public String UUID;
    @SerializedName("server")
    public String server;
    @SerializedName("skin")
    public String skin;
    @SerializedName("cosmetics")
    public String cosmetics;

    public PlayerInfoPacket(String playerName, String UUID, String server, String skin, String cosmetics) {
        super(PacketType.CLIENT_PLAYER_INFO);
        this.playerName = playerName;
        this.UUID = UUID;
        this.server = server;
        this.skin = skin;
        this.cosmetics = cosmetics;
    }
}
