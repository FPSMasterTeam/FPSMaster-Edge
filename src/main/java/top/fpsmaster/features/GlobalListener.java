package top.fpsmaster.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.*;
import top.fpsmaster.features.impl.interfaces.BetterChat;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.shader.KawaseBlur;

import static top.fpsmaster.utils.core.Utility.mc;

public class GlobalListener {
    public void init() {
        EventDispatcher.registerListener(this);
    }

    @Subscribe
    public void onChat(EventPacket e) {
        if (e.packet instanceof C01PacketChatMessage && e.type == EventPacket.PacketType.SEND) {
            String msg = ((C01PacketChatMessage) e.packet).getMessage();
            if (msg.startsWith("\u0000#COPY")) {
                msg = msg.substring(6);
                GuiScreen.setClipboardString(msg);
                e.cancel();
            }
        }
    }

    @Subscribe
    public void onChatSend(EventSendChatMessage e) {
    }


    Thread tickThread;

    @Subscribe
    public void onTick(EventTick e) {
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
                if (mc != null && mc.theWorld != null) {
                    Utility.flush();
                }
            });
            tickThread.start();
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



