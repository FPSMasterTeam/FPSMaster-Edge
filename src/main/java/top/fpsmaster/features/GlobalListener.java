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
import top.fpsmaster.benchmark.FontCompare;
import top.fpsmaster.benchmark.FontStyles;
import top.fpsmaster.benchmark.CollisionProbe;
import top.fpsmaster.benchmark.HudBreakdown;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.benchmark.UiShot;
import top.fpsmaster.replay.ReplayProbe;
import top.fpsmaster.replay.ReplayRecorder;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.*;
import top.fpsmaster.features.impl.interfaces.BetterChat;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.ui.PendingScreen;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.ui.screens.replay.ReplayHud;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.ChunkUpdateBudget;
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
        Minecraft minecraft = Minecraft.getMinecraft();
        // Entity rendering stops entirely after a disconnect, so culling cannot rely on its render
        // hook to notice a null world and release the old WorldClient/pending entity references.
        Performance.ENTITY_CULLING.updateWorld(minecraft.theWorld);
        if (minecraft.theWorld != null) {
            ReplayRecorder.instance().startIfRequested();
        }
        ReplayRecorder.instance().onClientTick();
        ReplayPlayer.instance().startIfRequested();
        ReplayPlayer.instance().onClientTick();
        ReplayProbe.onClientTick();
        Performance.onClientTick();
        ChunkUpdateBudget.onClientTick();
        CollisionProbe.onClientTick();
        UiShot.onClientTick();
        PendingScreen.tick();
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

        long started = HudBreakdown.enabled() ? System.nanoTime() : 0L;
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
            if (started != 0L) {
                HudBreakdown.record("~blur pass", System.nanoTime() - started);
                started = System.nanoTime();
            }
        }

        FPSMaster.componentsManager.draw((int) mouseX, (int) mouseY);
        if (started != 0L) {
            HudBreakdown.record("~components total", System.nanoTime() - started);
            started = System.nanoTime();
        }

        if (FontStyles.enabled()) {
            FontStyles.draw();
        }
        if (FontCompare.enabled()) {
            FontCompare.draw();
        }
        ReplayHud.draw();
        if (started != 0L) {
            // Charged separately because it only exists during playback - counting it as HUD cost
            // would attribute the measuring apparatus to the thing being measured.
            HudBreakdown.record("~replay overlay", System.nanoTime() - started);
            started = System.nanoTime();
        }
        NotificationManager.drawNotifications();
        if (started != 0L) {
            HudBreakdown.record("~notifications", System.nanoTime() - started);
            HudBreakdown.endFrame();
        }

    }

}


