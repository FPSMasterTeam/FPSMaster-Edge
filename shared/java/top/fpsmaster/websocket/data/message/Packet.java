package top.fpsmaster.websocket.data.message;

import com.google.gson.annotations.SerializedName;
import top.fpsmaster.websocket.data.JsonUtils;


public class Packet {
    @SerializedName("type")
    public PacketType type;

    public Packet(PacketType type) {
        this.type = type;
    }

    public String toJson() {
        return JsonUtils.toJson(this);
    }

    public Packet parse(String json) {
        return (Packet) JsonUtils.parseJson(json, this.getClass());
    }

    public static Packet parsePacket(String json, Class packet) {
        return (Packet) JsonUtils.parseJson(json, packet);
    }
}
