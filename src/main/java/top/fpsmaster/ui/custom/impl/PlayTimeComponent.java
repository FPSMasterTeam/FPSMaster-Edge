package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.PlayTime;
import top.fpsmaster.modules.statistics.PlayTimeStatistics;
import top.fpsmaster.ui.custom.TextComponent;

import java.util.Locale;

public class PlayTimeComponent extends TextComponent {
    public PlayTimeComponent() {
        super(PlayTime.class);
        x = 0.05f;
        y = 0.15f;
        allowScale = true;
    }

    @Override
    protected String text() {
        PlayTime module = getModule();
        String label = resolveLabel(module.label.getValue(), "playtime.defaultlabel", "Played: ");
        return label + formatTime(PlayTimeStatistics.getDisplayMillis(Minecraft.getMinecraft(), module.displayMode.getMode()));
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        return getModule().textColor.getRGB();
    }

    @Override
    protected float horizontalPadding() {
        return 8f;
    }

    @Override
    protected float boxHeight() {
        return 16f;
    }

    @Override
    protected float textOffsetX() {
        return 2f;
    }

    @Override
    protected float textOffsetY() {
        return 3f;
    }


    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        if (totalSeconds < 60L) {
            return totalSeconds + FPSMaster.i18n.get("playtime.unit.seconds");
        }

        long minutes = totalSeconds / 60L;
        if (minutes < 60L) {
            return minutes + FPSMaster.i18n.get("playtime.unit.minutes");
        }

        long hours = minutes / 60L;
        long remainMinutes = minutes % 60L;
        if (remainMinutes == 0L) {
            return hours + FPSMaster.i18n.get("playtime.unit.hours");
        }
        return String.format(Locale.ROOT, "%d%s%d%s", hours, FPSMaster.i18n.get("playtime.unit.hours"), remainMinutes, FPSMaster.i18n.get("playtime.unit.minutes"));
    }

    private PlayTime getModule() {
        return (PlayTime) mod;
    }
}
