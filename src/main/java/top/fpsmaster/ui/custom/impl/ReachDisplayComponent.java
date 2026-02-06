package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.ReachDisplay;
import top.fpsmaster.ui.custom.Component;

public class ReachDisplayComponent extends Component {

    public ReachDisplayComponent() {
        super(ReachDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String s = ReachDisplay.reach + " b";
        width = getStringWidth(18, s) + 4;
        height = 14f;
        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(18, s, x, y + 2, ReachDisplay.textColor.getRGB());
    }
}
