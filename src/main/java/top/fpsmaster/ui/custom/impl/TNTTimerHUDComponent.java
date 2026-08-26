package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.TNTTimerHUD;
import top.fpsmaster.ui.custom.TextComponent;

import java.text.DecimalFormat;

public class TNTTimerHUDComponent extends TextComponent {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    public TNTTimerHUDComponent() {
        super(TNTTimerHUD.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        float seconds = TNTTimerHUD.secondsRemaining();
        return seconds < 0f ? null : label() + FORMAT.format(Math.max(0f, seconds));
    }

    /** Nothing is primed while the editor is open, so it is shown mid-countdown instead. */
    @Override
    protected String previewText() {
        return label() + FORMAT.format(2.5f);
    }

    private String label() {
        return resolveLabel(null, "tnttimerhud.label", "TNT");
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        float seconds = TNTTimerHUD.secondsRemaining();
        return TNTTimerHUD.colorFor(seconds < 0f ? Float.MAX_VALUE : seconds);
    }

    @Override
    protected float boxHeight() {
        return 16f;
    }

    @Override
    protected float textOffsetY() {
        return 4f;
    }
}
