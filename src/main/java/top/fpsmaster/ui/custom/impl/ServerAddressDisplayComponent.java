package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ServerAddressDisplay;
import top.fpsmaster.ui.custom.Component;

public class ServerAddressDisplayComponent extends Component {
    public ServerAddressDisplayComponent() {
        super(ServerAddressDisplay.class);
        x = 0.02f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        ServerAddressDisplay module = getModule();

        String address = getServerAddress();
        if (address == null) {
            return;
        }

        String text = getLabel(module) + address;
        width = getStringWidth(16, text) + 8;
        height = 16f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x + 2, y + 3, module.textColor.getRGB());
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

    private String getLabel(ServerAddressDisplay module) {
        String label = module.label.getValue();
        if (label == null || label.trim().isEmpty()) {
            label = FPSMaster.i18n.get("serveraddressdisplay.defaultlabel");
            if ("serveraddressdisplay.defaultlabel".equals(label)) {
                label = "Server: ";
            }
            module.label.setValue(label);
        }
        if (!label.endsWith("：") && !label.endsWith(":") && !label.endsWith(" ")) {
            label += ": ";
        }
        return label;
    }

    private ServerAddressDisplay getModule() {
        return (ServerAddressDisplay) mod;
    }
}
