package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ComboDisplay;
import top.fpsmaster.ui.custom.Component;

public class ComboDisplayComponent extends Component {

    public ComboDisplayComponent() {
        super(ComboDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String text = "Combo: " + ComboDisplay.combo;
        if (ComboDisplay.combo == 0) {
            text = "No Combo";
        }
        
        width = getStringWidth(16, text) + 4;
        height = 16f;
        
        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x, y + 4, ComboDisplay.textColor.getRGB());
    }
}
