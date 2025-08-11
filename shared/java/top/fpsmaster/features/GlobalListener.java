package top.fpsmaster.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.java_websocket.enums.ReadyState;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.*;
import top.fpsmaster.features.impl.interfaces.BetterChat;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.interfaces.gui.IGuiNewChatProvider;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.modules.account.Cosmetic;
import top.fpsmaster.modules.client.ClientUser;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.shader.KawaseBlur;
import top.fpsmaster.websocket.client.WsClient;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static top.fpsmaster.utils.Utility.mc;

public class GlobalListener {
    private IChatComponent lastMessage = null;
    private int counter = 1;

    public void init() {
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventPacket e) {
        if (e.packet instanceof S02PacketChat && e.type == EventPacket.PacketType.RECEIVE) {
            if (BetterChat.using && BetterChat.foldMessage.getValue()) {
                S02PacketChat packet = (S02PacketChat) e.packet;
                if (packet.getType() == 2) return;
                IGuiNewChatProvider chatProvider = (IGuiNewChatProvider) mc.ingameGUI.getChatGUI();
                if (chatProvider.getDrawnChatLines().isEmpty()) {
                    counter = 1;
                    lastMessage = packet.getChatComponent();
                    return;
                }
                if (lastMessage.equals(packet.getChatComponent())) {
                    ChatLine c = chatProvider.getDrawnChatLines().get(0);
                    c = new ChatLine(c.getUpdatedCounter(), lastMessage.createCopy().appendSibling(new ChatComponentText("\247r\247f [x" + ++counter + "]")), c.getChatLineID());
                    chatProvider.getChatLines().set(0, c);
                    chatProvider.getDrawnChatLines().set(0, c);
                    e.cancel();
                } else {
                    System.out.println(lastMessage);
                    System.out.println(packet.getChatComponent());
                    counter = 1;
                    lastMessage = packet.getChatComponent();
                }
            }
            if (((S02PacketChat) e.packet).getChatComponent().getUnformattedText().trim().length() > 2 && ((S02PacketChat) e.packet).getType() != 2) {
                IChatComponent copyText = new ChatComponentText(" \247f[C]");
                copyText.getChatStyle()
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "\u0000#COPY" + ((S02PacketChat) e.packet).getChatComponent().getUnformattedText()))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(FPSMaster.i18n.get("copy.hover"))));
                ((S02PacketChat) e.packet).getChatComponent().appendSibling(copyText);
            }
        } else if (e.packet instanceof C01PacketChatMessage && e.type == EventPacket.PacketType.SEND) {
            String msg = ((C01PacketChatMessage) e.packet).getMessage();
            if (msg.startsWith("\u0000#COPY")) {
                msg = msg.substring(6);
                GuiScreen.setClipboardString(msg);
                e.cancel();
            }
        }
//        if (Translator.using) {
//            if (ProviderManager.packetChat.isPacket(e.packet)) {
//                ProviderManager.packetChat.appendTranslation(e.packet);
//            }
//        }
    }

    @Subscribe
    public void onChatSend(EventSendChatMessage e) {
    }


    PlayerInformation playerInformation = null;


    Map<UUID, NetworkPlayerInfo> playerInfos = new ConcurrentHashMap<>();
    Thread tickThread;
    Thread accThread;

    @Subscribe
    public void onTick(EventTick e) throws URISyntaxException {
        if (tickThread == null || !tickThread.isAlive()) {
            tickThread = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
//                if (MusicPlayer.isPlaying && MusicPlayer.getPlayProgress() > 0.999 && (Music.downloadThread == null || !Music.downloadThread.isAlive())) {
//                    MusicPlayer.playList.next();
//                    try {
//                        Thread.sleep(20000);
//                    } catch (InterruptedException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                }
                if (ProviderManager.mcProvider.getWorld() != null) {
                    Utility.flush();
                }
            });
            tickThread.start();
        }


        if (accThread == null || !accThread.isAlive()) {
            accThread = new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (FPSMaster.INSTANCE.loggedIn) {
                    if (FPSMaster.INSTANCE.wsClient == null) {
                        try {
                            FPSMaster.INSTANCE.wsClient = WsClient.start("wss://service.fpsmaster.top/");
                        } catch (URISyntaxException ex) {
                            throw new RuntimeException(ex);
                        }
                        Utility.sendClientDebug("尝试连接");
                    } else if (FPSMaster.INSTANCE.wsClient.isClosed() || FPSMaster.INSTANCE.wsClient.getReadyState() != ReadyState.OPEN) {
                        FPSMaster.INSTANCE.wsClient.close();
                        FPSMaster.INSTANCE.wsClient.connect();
                        playerInformation = null;
                        playerInfos.clear();
                        Utility.sendClientDebug("尝试重连");
                    } else {
                        FPSMaster.INSTANCE.wsClient.sendPing();
                    }
                    if (mc.getNetHandler() == null)
                        return;
                    if (mc.getNetHandler().getPlayerInfoMap() == null)
                        return;

                    Set<UUID> currentPlayers = mc.getNetHandler().getPlayerInfoMap().stream()
                            .map(info -> info.getGameProfile().getId())
                            .collect(Collectors.toSet());

                    playerInfos.keySet().retainAll(currentPlayers);


                    if (FPSMaster.INSTANCE.wsClient.getReadyState() != ReadyState.OPEN)
                        return;

                    for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                        UUID uuid = info.getGameProfile().getId();
                        if (!playerInfos.containsKey(uuid)) {
                            playerInfos.put(uuid, info);
                            FPSMaster.INSTANCE.wsClient.fetchPlayer(uuid.toString(), info.getGameProfile().getName());
                        }
                    }

                    for (ClientUser user : FPSMaster.clientUsersManager.users) {
                        FPSMaster.INSTANCE.wsClient.fetchPlayer(user.uuid, user.name);
                    }

                    if (playerInformation == null) {
                        playerInformation = new PlayerInformation(ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getPlayer().getUniqueID().toString(), ProviderManager.mcProvider.getServerAddress(), AccountManager.cosmeticsUsing, AccountManager.skin);
                        FPSMaster.INSTANCE.wsClient.sendInformation(AccountManager.skin, AccountManager.cosmeticsUsing, ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getServerAddress());
                    } else if (!playerInformation.serverAddress.equals(ProviderManager.mcProvider.getServerAddress()) || !playerInformation.name.equals(ProviderManager.mcProvider.getPlayer().getName()) || !playerInformation.skin.equals(AccountManager.skin) || !playerInformation.uuid.equals(ProviderManager.mcProvider.getPlayer().getUniqueID().toString()) || !playerInformation.cosmetics.equals(AccountManager.cosmeticsUsing)) {
                        playerInformation = new PlayerInformation(ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getPlayer().getUniqueID().toString(), ProviderManager.mcProvider.getServerAddress(), AccountManager.cosmeticsUsing, AccountManager.skin);
                        FPSMaster.INSTANCE.wsClient.sendInformation(AccountManager.skin, AccountManager.cosmeticsUsing, ProviderManager.mcProvider.getPlayer().getName(), ProviderManager.mcProvider.getServerAddress());
                    }
                }
            });
            accThread.start();
        }
    }

    @Subscribe
    public void onCape(EventCapeLoading e) {
        String[] cosmetics;

        if (e.player == mc.thePlayer) {
            if (AccountManager.cosmeticsUsing.isEmpty())
                return;
            cosmetics = AccountManager.cosmeticsUsing.split(",");
        } else {
            ClientUser clientUser = FPSMaster.clientUsersManager.getClientUser(e.player);
            if (clientUser == null)
                return;
            cosmetics = clientUser.cosmetics.split(",");
        }

        for (String cosmetic : cosmetics) {
            if (cosmetic.isEmpty())
                continue;
            Cosmetic cosmetic1 = AccountManager.cosmetics.get(Integer.valueOf(cosmetic));
            if (cosmetic1.resource.endsWith(".gif")) {
                if (cosmetic1.frame < cosmetic1.frames.size() - 1) {
                    if (System.currentTimeMillis() - cosmetic1.frameTime > cosmetic1.frames.get(cosmetic1.frame).delay) {
                        cosmetic1.frame++;
                        cosmetic1.frameTime = System.currentTimeMillis();
                    }
                } else {
                    cosmetic1.frame = 0;
                    cosmetic1.frameTime = System.currentTimeMillis();
                }
                e.setCachedCape("ornaments/" + cosmetic + "_resource_" + cosmetic1.frame);
            } else {
                e.setCachedCape("ornaments/" + cosmetic + "_resource");
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

    static class PlayerInformation {
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
