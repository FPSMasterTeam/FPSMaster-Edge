package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.ReachDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class ReachDisplayComponent extends TextComponent {

    public ReachDisplayComponent() {
        super(ReachDisplay.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        return ReachDisplay.reach + " b";
    }

    @Override
    protected int fontSize() {
        return 18;
    }

    @Override
    protected int textColor() {
        return ReachDisplay.textColor.getRGB();
    }
}
