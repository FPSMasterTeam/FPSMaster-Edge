package top.fpsmaster.websocket.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.fpsmaster.websocket.data.message.Packet;

public class JsonUtils {

    private static final Gson gson = new GsonBuilder().create();

    public static Object parseJson(String json, Class<?> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Packet packet) {
        return gson.toJson(packet);
    }
}
