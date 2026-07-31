package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import top.fpsmaster.features.impl.interfaces.PingDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class PingDisplayComponent extends TextComponent {

    public PingDisplayComponent() {
        super(PingDisplay.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return null;
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return "Ping: " + (info != null ? info.getResponseTime() : 0) + "ms";
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        return PingDisplay.textColor.getRGB();
    }
}
