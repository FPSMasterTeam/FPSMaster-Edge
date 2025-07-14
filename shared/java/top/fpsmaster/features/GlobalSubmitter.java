package top.fpsmaster.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.*;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.OptifineUtil;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.shader.KawaseBlur;
import top.fpsmaster.utils.render.shader.RoundedUtil;
import top.fpsmaster.websocket.client.WsClient;

import java.net.URISyntaxException;

public class GlobalSubmitter {

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

    @Subscribe
    public void onTick(EventTick e) throws URISyntaxException {
        if (musicSwitchTimer.delay(500)) {
            FPSMaster.async.runnable(() -> {
                if (MusicPlayer.isPlaying && MusicPlayer.getPlayProgress() > 0.999) {
                    MusicPlayer.curPlayProgress = 0f;
                    MusicPlayer.playList.next();
                }
                if (ProviderManager.mcProvider.getWorld() != null){
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
                    }
                }
            });
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
}
