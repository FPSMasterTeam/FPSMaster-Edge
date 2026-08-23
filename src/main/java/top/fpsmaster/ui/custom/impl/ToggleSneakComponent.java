package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.utility.ToggleSneak;
import top.fpsmaster.ui.custom.Component;

import static top.fpsmaster.utils.core.Utility.mc;

public class ToggleSneakComponent extends Component {
    public ToggleSneakComponent() {
        super(ToggleSneak.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String text;
        if (ToggleSneak.sneak) {
            text = "[Sneaking (Toggled)]";
        } else {
            text = "";
            if (mc.thePlayer != null && mc.thePlayer.isSneaking()) {
                text = "[Sneaking (Vanilla)]";
            }
        }
        drawString(16, text, x, y, -1);
        this.width = getStringWidth(16, text);
        this.height = 12;
    }

    @Override
    public void measurePreview() {
        width = getStringWidth(16, "[Sneaking (Toggled)]");
        height = 12f;
    }

    @Override
    public void drawPreview(float x, float y) {
        drawString(16, "[Sneaking (Toggled)]", x, y, -1);
    }
}
