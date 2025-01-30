package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.PingDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.interfaces.ProviderManager;

public class PingDisplayComponent extends Component {

    public PingDisplayComponent() {
        super(PingDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        
        // Get ping of connection
        if (ProviderManager.mcProvider.getPlayer() == null) {
            return;
        }

        String ping = ProviderManager.mcProvider.getRespondTime() + "ms";
        String text = "Ping: " + ping;

        width = getStringWidth(16, text) + 4;
        height = 14f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x, y + 2, PingDisplay.textColor.getRGB());
    }
}
