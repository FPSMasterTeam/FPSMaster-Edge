package top.fpsmaster.ui.minimap;

import top.fpsmaster.ui.minimap.interfaces.InterfaceHandler;

import java.io.IOException;

public class XaeroMinimap
{
    public static XaeroMinimap instance;

    public void load() throws IOException {
        InterfaceHandler.loadPresets();
        InterfaceHandler.load();
    }
}



