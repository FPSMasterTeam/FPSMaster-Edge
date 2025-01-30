package top.fpsmaster.ui.screens.oobe;

import net.minecraft.client.gui.GuiScreen;
import top.fpsmaster.ui.screens.oobe.impls.Login;

import java.io.IOException;

public class GuiLogin extends GuiScreen {

    private static Login login = new Login(false);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        login.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        login.mouseClick(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        login.keyTyped(typedChar, keyCode);
    }
}
