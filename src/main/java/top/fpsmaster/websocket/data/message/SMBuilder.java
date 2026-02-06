package top.fpsmaster.websocket.data.message;

import top.fpsmaster.websocket.data.message.server.SMessageType;

public class SMBuilder {
    public static String build(SMessageType type, String msg) {
        switch (type) {
            case WELCOME:
                return "§e[欢迎] " + msg;
            case LEAVE:
                return "§3[离开] " + msg;
            case SERVER_MESSAGE:
                return "§9[通知] " + msg;
            case CHAT:
                return "§2[聊天] " + msg;
            case DM:
                return "§a[私聊] " + msg;
            case WORLD_MESSAGE:
                return "§2[世界消息] " + msg;
            default:
                return "";
        }
    }
}