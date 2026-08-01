package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.ClockDisplay;
import top.fpsmaster.ui.custom.TextComponent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClockDisplayComponent extends TextComponent {

    public ClockDisplayComponent() {
        super(ClockDisplay.class);
        x = 0.60f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    protected String text() {
        ClockDisplay module = getModule();
        String pattern = module.hour24Mode.getValue() ? "HH:mm" : "hh:mm";
        if (module.showSeconds.getValue()) {
            pattern += ":ss";
        }
        if (!module.hour24Mode.getValue()) {
            pattern += " a";
        }
        String label = resolveLabel(module.label.getValue(), "clockdisplay.defaultlabel", "Time: ");
        return label + new SimpleDateFormat(pattern).format(new Date());
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

    private ClockDisplay getModule() {
        return (ClockDisplay) mod;
    }
}
