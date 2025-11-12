package top.fpsmaster.api.provider.gui;

import net.minecraft.client.gui.GuiScreen;
import top.fpsmaster.api.provider.IProvider;

public interface IGuiMainMenuProvider extends IProvider {
    void initGui();
    void renderSkybox(int mouseX, int mouseY, float partialTicks, int width, int height, float zLevel);
    void showSinglePlayer(GuiScreen screen);
}