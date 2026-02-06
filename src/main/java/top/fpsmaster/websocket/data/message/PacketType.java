package top.fpsmaster.websocket.data.message;

import com.google.gson.annotations.SerializedName;

public enum PacketType {
    @SerializedName("CLIENT_LOGIN")
    CLIENT_LOGIN,
    @SerializedName("CLIENT_MESSAGE")
    CLIENT_MESSAGE,
    @SerializedName("CLIENT_DIRECT_MSG")
    CLIENT_DIRECT_MSG,
    @SerializedName("CLIENT_PLAYER_INFO")
    CLIENT_PLAYER_INFO,
    @SerializedName("CLIENT_FETCH_PLAYER")
    CLIENT_FETCH_PLAYER,
    @SerializedName("SERVER_FETCH_PLAYER")
    SERVER_FETCH_PLAYER,
    @SerializedName("SERVER_MESSAGE")
    SERVER_MESSAGE
}
