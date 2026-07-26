package top.fpsmaster.features.impl.utility;

import net.minecraft.event.ClickEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.math.MathTimer;

import static top.fpsmaster.utils.core.Utility.mc;

public class AutoGG extends Module {

    private final BooleanSetting autoPlay = new BooleanSetting("AutoPlay", false);
    private final NumberSetting delay = new NumberSetting("DelayToPlay", 5, 0, 10, 1, autoPlay::getValue);
    private final TextSetting message = new TextSetting("Message", "gg");

    private final ModeSetting servers = new ModeSetting("Servers", 0, "hypixel", "normal");

    private final ModeSetting detectionMode = new ModeSetting("DetectionMode", 2, "chat", "title", "both");

    private final String[] hypixelChatTrigger = {
            "Reward Summary",
            "1st Killer",
            "Damage Dealt",
            "Winners:",
            "You died!",
            "奖励总览",
            "击杀数第一名",
            "造成伤害",
            "你死了！",
            "获胜者："
    };

    private final String[] normalChatTrigger = {
            "获胜者",
            "第一名杀手",
            "击杀第一名",
            "胜利",
            "恭喜"
    };

    private final String[] victoryKeywords = {
            "VICTORY",
            "YOU WIN",
            "1ST KILLER",
            "WINS!",
            "WINNER",
            "VICTOR",
            "GAME OVER",
            "YOU DIED",
            "胜利",
            "获胜",
            "恭喜",
            "你赢了"
    };

    private boolean ggPending = false;
    private int ggTicks = 0;

    private boolean autoPlayPending = false;
    private String autoPlayCommand = null;

    private final MathTimer cooldownTimer = new MathTimer();

    public AutoGG() {
        super("AutoGG", Category.Utility);
        this.addSettings(autoPlay, delay, message, servers, detectionMode);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.type != EventPacket.PacketType.RECEIVE) return;
        if (mc.thePlayer == null) return;

        if (event.packet instanceof S02PacketChat) {
            handleChatPacket((S02PacketChat) event.packet);
        } else if (event.packet instanceof S45PacketTitle) {
            handleTitlePacket((S45PacketTitle) event.packet);
        }
    }

    private void handleChatPacket(S02PacketChat packet) {
        IChatComponent component = packet.getChatComponent();
        String raw = StringUtils.stripControlCodes(component.getUnformattedText());

        boolean matched = false;
        switch (servers.getValue()) {
            case 0:
                for (String s : hypixelChatTrigger) {
                    if (raw.contains(s)) {
                        matched = true;
                        break;
                    }
                }
                break;
            case 1:
                for (String s : normalChatTrigger) {
                    if (raw.contains(s)) {
                        matched = true;
                        break;
                    }
                }
                break;
        }

        if (matched && isDetectionEnabled("chat")) {
            scheduleGG();
        }

        if (autoPlay.getValue() && servers.getValue() == 0) {
            for (IChatComponent sibling : component.getSiblings()) {
                ClickEvent clickEvent = sibling.getChatStyle().getChatClickEvent();
                if (clickEvent != null
                        && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND
                        && clickEvent.getValue().trim().toLowerCase().startsWith("/play ")) {
                    scheduleAutoPlay(clickEvent.getValue());
                    break;
                }
            }
        }
    }

    private void handleTitlePacket(S45PacketTitle packet) {
        if (packet.getType() != S45PacketTitle.Type.TITLE
                && packet.getType() != S45PacketTitle.Type.SUBTITLE) {
            return;
        }

        IChatComponent titleComponent = packet.getMessage();
        if (titleComponent == null) return;

        String text = StringUtils.stripControlCodes(titleComponent.getUnformattedText()).toUpperCase().trim();
        if (text.isEmpty()) return;

        for (String keyword : victoryKeywords) {
            if (text.contains(keyword)) {
                if (isDetectionEnabled("title")) {
                    scheduleGG();
                }
                break;
            }
        }
    }

    private boolean isDetectionEnabled(String source) {
        switch (detectionMode.getValue()) {
            case 0:  return source.equals("chat");
            case 1:  return source.equals("title");
            case 2:  return true;
            default: return true;
        }
    }

    private void scheduleGG() {
        if (!cooldownTimer.delay(5000)) {
            return;
        }
        if (ggPending) return;
        ggPending = true;
        ggTicks = 20;
    }

    private void scheduleAutoPlay(String command) {
        if (autoPlayPending) return;
        autoPlayPending = true;
        autoPlayCommand = command;

        int delaySec = delay.getValue().intValue();
        if (delaySec > 0) {
            Utility.sendClientNotify(
                    "Sending you to the next game in " + delaySec + " seconds"
            );
        }

        FPSMaster.async.runnable(() -> {
            try {
                Thread.sleep(delaySec * 1000L);
                String cmd = autoPlayCommand;
                if (cmd != null) {
                    Utility.sendChatMessage(cmd);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            resetState();
            return;
        }

        if (ggPending) {
            if (--ggTicks <= 0) {
                String msg = message.getValue();
                if (servers.getValue() == 0) {
                    Utility.sendChatMessage("/ac " + msg);
                } else {
                    Utility.sendChatMessage(msg);
                }
                ggPending = false;
            }
        }
    }

    private void resetState() {
        ggPending = false;
        ggTicks = 0;
        autoPlayPending = false;
        autoPlayCommand = null;
        cooldownTimer.reset();
    }
}
