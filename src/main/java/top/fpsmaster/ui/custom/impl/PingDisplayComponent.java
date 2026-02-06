package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.PingDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import top.fpsmaster.ui.custom.Component;

public class PingDisplayComponent extends Component {

    public PingDisplayComponent() {
        super(PingDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        
        // Get ping of connection
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        String ping = (info != null ? info.getResponseTime() : 0) + "ms";
        String text = "Ping: " + ping;

        width = getStringWidth(16, text) + 4;
        height = 14f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x, y + 2, PingDisplay.textColor.getRGB());
    }
}



