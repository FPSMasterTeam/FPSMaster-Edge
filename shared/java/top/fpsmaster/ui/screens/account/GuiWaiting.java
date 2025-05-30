package top.fpsmaster.ui.screens.account;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.thirdparty.microsoft.MicrosoftLogin;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.io.IOException;

public class GuiWaiting extends GuiScreen {
    public static boolean loggedIn = false;

    @Override
    public void initGui() {
        super.initGui();
        FPSMaster.async.runnable(MicrosoftLogin::loginViaBrowser);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

        // Draw background
        Render2DUtils.drawRect(
            0f, 0f,
            sr.getScaledWidth(), sr.getScaledHeight(),
            new Color(255, 255, 255)
        );

        // Draw text
        FPSMaster.fontManager.s24.drawCenteredString(
            MicrosoftLogin.loginProgressMessage,
            sr.getScaledWidth() / 2f,
            sr.getScaledHeight() / 2f - 30,
            FPSMaster.theme.getTextColorDescription().getRGB()
        );

        FPSMaster.fontManager.s40.drawCenteredString(
            FPSMaster.i18n.get("microsoft.login.title"),
            sr.getScaledWidth() / 2f,
            sr.getScaledHeight() / 2f + 10,
            FPSMaster.theme.getPrimary().getRGB()
        );

        // Check if logged in and switch to the main menu
        if (loggedIn) {
            loggedIn = false;
            Minecraft.getMinecraft().displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == 1) { // Escape key
            Minecraft.getMinecraft().displayGuiScreen(new GuiMainMenu());
        }
    }
}
