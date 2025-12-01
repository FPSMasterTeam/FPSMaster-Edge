package top.fpsmaster.impl.gui;

import top.fpsmaster.api.domain.gui.IGuiAPI;
import top.fpsmaster.api.domain.gui.IGuiIngame;

public class GuiAPIImpl implements IGuiAPI {
    
    @Override
    public IGuiIngame getGuiIngame() {
        return new GuiIngameImpl();
    }
}
