package top.fpsmaster.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.*;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.modules.client.ClientUser;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.shader.KawaseBlur;
import top.fpsmaster.websocket.client.WsClient;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static top.fpsmaster.utils.Utility.mc;

public class GlobalListener {

    MathTimer musicSwitchTimer = new MathTimer();

    public void init() {
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventPacket e) {
//        if (Translator.using) {
//            if (ProviderManager.packetChat.isPacket(e.packet)) {
//                ProviderManager.packetChat.appendTranslation(e.packet);
//            }
//        }
    }

    @Subscribe
    public void onChatSend(EventSendChatMessage e) {
        String msg = e.msg;
    }


    PlayerInformation playerInformation = null;


    Map<UUID, NetworkPlayerInfo> playerInfos = new ConcurrentHashMap<>();
    Thread tickThread;

    @Subscribe
    public void onTick(EventTick e) throws URISyntaxException {
        if (musicSwitchTimer.delay(500)) {
            if (tickThread == null || !tickThread.isAlive()) {
                tickThread = new Thread(() -> {
                    if (MusicPlayer.isPlaying && MusicPlayer.getPlayProgress() > 0.999) {
                        MusicPlayer.playList.next();
                    }
                    if (ProviderManager.mcProvider.getWorld() != null) {
                        Utility.flush();
                    }
                    if (FPSMaster.INSTANCE.loggedIn) {
                        if (FPSMaster.INSTANCE.wsClient == null) {
                            try {
                                FPSMaster.INSTANCE.wsClient = WsClient.start("wss://service.fpsmaster.top/");
                            } catch (URISyntaxException ex) {
                                throw new RuntimeException(ex);
                            }
                            Utility.sendClientDebug("尝试连接");
                        } else if (FPSMaster.INSTANCE.wsClient.isClosed() && !FPSMaster.INSTANCE.wsClient.isOpen()) {
                            FPSMaster.INSTANCE.wsClient.close();
                            FPSMaster.INSTANCE.wsClient.connect();
                            Utility.sendClientDebug("尝试重连");
                        } else {
                            FPSMaster.INSTANCE.wsClient.sendPing();
                        }
                    }
                    if (mc.getNetHandler() == null)
                        return;
                    if (mc.getNetHandler().getPlayerInfoMap() == null)
                        return;

                    Set<UUID> currentPlayers = mc.getNetHandler().getPlayerInfoMap().stream()
                            .map(info -> info.getGameProfile().getId())
                            .collect(Collectors.toSet());

                    playerInfos.keySet().retainAll(currentPlayers);

                    for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                        UUID uuid = info.getGameProfile().getId();
                        if (!playerInfos.containsKey(uuid)) {
                            playerInfos.put(uuid, info);
                            FPSMaster.INSTANCE.wsClient.fetchPlayer(uuid.toString(), info.getGameProfile().getName());
                        }
                    }

                    if (playerInformation == null) {
                        playerInformation = new PlayerInformation(ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getPlayer().getUniqueID().toString(), ProviderManager.mcProvider.getServerAddress(), AccountManager.cosmeticsUsing, AccountManager.skin);
                        FPSMaster.INSTANCE.wsClient.sendInformation(AccountManager.skin, AccountManager.cosmeticsUsing, ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getServerAddress());
                    } else if (!playerInformation.serverAddress.equals(ProviderManager.mcProvider.getServerAddress()) || !playerInformation.name.equals(ProviderManager.mcProvider.getPlayer().getName()) || !playerInformation.skin.equals(AccountManager.skin) || !playerInformation.uuid.equals(ProviderManager.mcProvider.getPlayer().getUniqueID().toString())) {
                        playerInformation = new PlayerInformation(ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getPlayer().getUniqueID().toString(), ProviderManager.mcProvider.getServerAddress(), AccountManager.cosmeticsUsing, AccountManager.skin);
                        FPSMaster.INSTANCE.wsClient.sendInformation(AccountManager.skin, AccountManager.cosmeticsUsing, ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getServerAddress());
                    }
                });
                tickThread.start();
            }
        }
    }

    @Subscribe
    public void onCape(EventCapeLoading e) {
        if (!AccountManager.cosmeticsUsing.isEmpty()) {
            if (e.player == mc.thePlayer)
                e.setCachedCape("ornaments/" + AccountManager.cosmeticsUsing + "_resource");
            else {
                ClientUser clientUser = FPSMaster.clientUsersManager.getClientUser(e.player);
                if (clientUser != null) {
                    e.setCachedCape("ornaments/" + clientUser.cosmetics + "_resource");
                }
            }
        }
    }

    @Subscribe
    public void onRender(EventRender2D e) {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        float mouseX = (float) Mouse.getX() / scaledResolution.getScaleFactor();
        float mouseY = scaledResolution.getScaledHeight() - (float) Mouse.getY() / scaledResolution.getScaleFactor();

        if (ClientSettings.blur.getValue()) {
            StencilUtil.initStencilToWrite();
            EventDispatcher.dispatchEvent(new EventShader());
            FPSMaster.componentsManager.draw((int) mouseX, (int) mouseY);
            StencilUtil.readStencilBuffer(1);
            KawaseBlur.renderBlur(3, 3);
            StencilUtil.uninitStencilBuffer();
        }
        FPSMaster.componentsManager.draw((int) mouseX, (int) mouseY);
        NotificationManager.drawNotifications();
    }

    class PlayerInformation {
        String name;
        String uuid;
        String serverAddress;
        String cosmetics;
        String skin;

        public PlayerInformation(String name, String uuid, String serverAddress, String cosmetics, String skin) {
            this.name = name;
            this.uuid = uuid;
            this.serverAddress = serverAddress;
            this.cosmetics = cosmetics;
            this.skin = skin;
        }
    }
}
