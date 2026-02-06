package top.fpsmaster.websocket.data.message.server;

import com.google.gson.annotations.SerializedName;
import top.fpsmaster.websocket.data.message.Packet;
import top.fpsmaster.websocket.data.message.PacketType;

public class SMessagePacket extends Packet {
    @SerializedName("msg")
    public String msg;
    @SerializedName("msg_type")
    public SMessageType msg_type;
    @SerializedName("uuid")
    public String uuid;

    public SMessagePacket(SMessageType type, String msg) {
        super(PacketType.SERVER_MESSAGE);
        this.msg_type = type;
        this.msg = msg;
    }

    public SMessagePacket(SMessageType type, String msg, String uuid) {
        super(PacketType.SERVER_MESSAGE);
        this.msg_type = type;
        this.msg = msg;
        this.uuid = uuid;
    }
}
