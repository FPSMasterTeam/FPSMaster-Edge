package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.ComboDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class ComboDisplayComponent extends TextComponent {

    public ComboDisplayComponent() {
        super(ComboDisplay.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        return ComboDisplay.combo == 0 ? "No Combo" : "Combo: " + ComboDisplay.combo;
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        return ComboDisplay.textColor.getRGB();
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
