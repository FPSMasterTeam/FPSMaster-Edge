package top.fpsmaster.websocket.data.message.client;

import com.google.gson.annotations.SerializedName;
import top.fpsmaster.websocket.data.message.Packet;
import top.fpsmaster.websocket.data.message.PacketType;

public class MessagePacket extends Packet {

    @SerializedName("messageType")
    public MessageType messageType;
    @SerializedName("msg")
    public String msg;

    public MessagePacket(MessageType type, String msg) {
        super(PacketType.CLIENT_MESSAGE);
        this.messageType = type;
        this.msg = msg;
    }
    public enum MessageType {
        CHAT,
        WORLD,
        COMMAND
    }
}
