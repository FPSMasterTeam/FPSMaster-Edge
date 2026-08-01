package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ServerAddressDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class ServerAddressDisplayComponent extends TextComponent {
    public ServerAddressDisplayComponent() {
        super(ServerAddressDisplay.class);
        x = 0.02f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    protected String text() {
        String address = getServerAddress();
        if (address == null) {
            return null;
        }
        ServerAddressDisplay module = getModule();
        return resolveLabel(module.label.getValue(), "serveraddressdisplay.defaultlabel", "Server: ") + address;
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        return getModule().textColor.getRGB();
    }

    @Override
    protected float horizontalPadding() {
        return 8f;
    }

    @Override
    protected float boxHeight() {
        return 16f;
    }

    @Override
    protected float textOffsetX() {
        return 2f;
    }

    @Override
    protected float textOffsetY() {
        return 3f;
    }

    @Override
    public boolean shouldDisplay() {
        return mod.isEnabled() && getServerAddress() != null;
    }

    private String getServerAddress() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return null;

        if (mc.isIntegratedServerRunning()) {
            return FPSMaster.i18n.get("serveraddressdisplay.singleplayer");
        }

        ServerData serverData = mc.getCurrentServerData();
        if (serverData != null && serverData.serverIP != null && !serverData.serverIP.isEmpty()) {
            return serverData.serverIP;
        }

        return null;
    }


    private ServerAddressDisplay getModule() {
        return (ServerAddressDisplay) mod;
    }
}
