package top.fpsmaster.websocket.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.utility.IRC;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.websocket.data.message.Packet;
import top.fpsmaster.websocket.data.message.client.*;
import top.fpsmaster.websocket.data.message.server.SFetchPlayerPacket;
import top.fpsmaster.websocket.data.message.server.SMessagePacket;

import java.net.URI;
import java.net.URISyntaxException;

public class WsClient extends WebSocketClient {

    public WsClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Utility.sendClientDebug("成功连接到irc服务器，开始验证登录信息");
        if (ProviderManager.mcProvider.getPlayer() != null) {
            Utility.sendClientMessage(FPSMaster.i18n.get("irc.enable").replace("%s", ClientSettings.prefix.getValue()));
        }
        assert FPSMaster.accountManager != null;
        send(new LoginPacket(FPSMaster.accountManager.getUsername(), FPSMaster.accountManager.getToken()).toJson());
    }

    public void sendMessage(String message) {
        send(new MessagePacket(MessagePacket.MessageType.CHAT, message).toJson());
    }

    public void sendInformation(String skin, String cosmetics, String gameID, String serverAddress) {
        send(new PlayerInfoPacket(gameID, ProviderManager.mcProvider.getPlayer().getUniqueID().toString(), serverAddress, skin, cosmetics).toJson());
    }


    public void sendCommand(String message) {
        send(new MessagePacket(MessagePacket.MessageType.COMMAND, message).toJson());
    }

    public void sendDM(String to, String message) {
        send(new DMPacket(to, message).toJson());
    }

    @Override
    public void onMessage(String message) {
        Packet packet = Packet.parsePacket(message, Packet.class);
        switch (packet.type) {
            case SERVER_MESSAGE:
                SMessagePacket parsePacket = (SMessagePacket) Packet.parsePacket(message, SMessagePacket.class);
                if (IRC.using)
                    Utility.sendClientMessage(parsePacket.msg);
                break;
            case SERVER_FETCH_PLAYER:
                SFetchPlayerPacket parsePacket1 = (SFetchPlayerPacket) Packet.parsePacket(message, SFetchPlayerPacket.class);
                FPSMaster.clientUsersManager.addFromFetch(parsePacket1);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Utility.sendClientDebug("连接关闭:" + reason);
        FPSMaster.INSTANCE.wsClient = null;
    }

    @Override
    public void onError(Exception ex) {
        Utility.sendClientDebug("聊天服务错误 " + ex.getMessage());
    }

    public static WsClient start(String addr) throws URISyntaxException {
        WsClient client = new WsClient(new URI(addr));
        client.connect();
        return client;
    }

    public void fetchPlayer(String uuid, String name) {
        send(new FetchPlayerPacket(uuid, name).toJson());
    }
}