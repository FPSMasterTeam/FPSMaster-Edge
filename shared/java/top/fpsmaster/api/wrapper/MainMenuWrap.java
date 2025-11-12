package top.fpsmaster.api.wrapper;

import net.minecraft.client.gui.GuiScreen;

public interface MainMenuWrap {
    void initGui();
    void renderSkybox(int mouseX, int mouseY, float partialTicks, int width, int height, float zLevel);
    void showSinglePlayer(GuiScreen screen);
}