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
import top.fpsmaster.font.EnhancedFontRenderer;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.draw.Circles;
import top.fpsmaster.utils.render.shader.KawaseBlur;

import static top.fpsmaster.utils.core.Utility.mc;

public class GlobalListener {
    private long lastFlushAt;

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

    @Subscribe
    public void onValueChange(EventValueChange e) {
        if (FPSMaster.configManager.isConfigLoaded() && !FPSMaster.configManager.isLoadingConfig()) {
            ConfigProfileUtils.saveActiveProfileQuietly();
        }
    }
    @Subscribe
    public void onTick(EventTick e) {
        EnhancedFontRenderer.tickAllInstances();
        long now = System.currentTimeMillis();
        if (now - lastFlushAt < 1000L) {
            return;
        }
        lastFlushAt = now;
        FPSMaster.telemetryReporter.tick(now);
        if (mc != null && mc.theWorld != null) {
            Utility.flush();
        }
    }

    @Subscribe
    public void onRender(EventRender2D e) {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        float mouseX = (float) Mouse.getX() / scaledResolution.getScaleFactor();
        float mouseY = scaledResolution.getScaledHeight() - (float) Mouse.getY() / scaledResolution.getScaleFactor();

        // Size everything first: the blur mask and the anchor math below both read width/height, and
        // components only assign those while drawing.
        FPSMaster.componentsManager.measureAll();

        if (ClientSettings.blur.getValue()) {
            StencilUtil.initStencilToWrite();
            try {
                EventDispatcher.dispatchEvent(new EventShader());
                FPSMaster.componentsManager.drawBackgroundMasks();
                StencilUtil.readStencilBuffer(1);
                KawaseBlur.renderBlur(3, 3);
            } finally {
                StencilUtil.uninitStencilBuffer();
            }
        }

        FPSMaster.componentsManager.draw((int) mouseX, (int) mouseY);

        NotificationManager.drawNotifications();

    }

}



