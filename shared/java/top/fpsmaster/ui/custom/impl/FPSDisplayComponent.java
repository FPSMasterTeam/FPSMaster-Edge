package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.ui.custom.Component;

public class FPSDisplayComponent extends Component {

    public FPSDisplayComponent() {
        super(FPSDisplay.class);
        x = 0.05f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String s = Minecraft.getDebugFPS() + "fps";
        
        width = getStringWidth(18, s) + 4;
        height = 14f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(18, s, x, y + 2, FPSDisplay.textColor.getRGB());
    }
}
