package top.fpsmaster.ui.screens.oobe.impls;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.screens.oobe.Scene;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;

public class Login extends Scene {
    private String msg;
    private boolean msgbox = false;
    private GuiButton btn;
    private GuiButton btn2;
    private TextField username;
    private TextField password;
    private ColorAnimation msgBoxAnimation;

    public Login(boolean isOOBE) {
        username = new TextField(FPSMaster.fontManager.s18, false, FPSMaster.i18n.get("oobe.login.username"), -1, new Color(200, 200, 200).getRGB(), 32);
        password = new TextField(FPSMaster.fontManager.s18, true, FPSMaster.i18n.get("oobe.login.password"), -1, new Color(200, 200, 200).getRGB(), 32);
        msgBoxAnimation = new ColorAnimation(new Color(0, 0, 0, 0));

        username.setText(FPSMaster.configManager.configure.getOrCreate("username", ""));
        
        btn = new GuiButton(FPSMaster.i18n.get("oobe.login.login"), () -> {
            try {
                JsonObject login = AccountManager.login(username.getText(), password.getText());
                if (login.get("code").getAsInt() == 200) {
                    if (FPSMaster.accountManager != null) {
                        FPSMaster.accountManager.setUsername(username.getText());
                        FPSMaster.accountManager.setToken(login.get("msg").getAsString());
                    }
                    FileUtils.saveTempValue("token", FPSMaster.accountManager.getToken());
                    FPSMaster.INSTANCE.loggedIn = true;
                    if (isOOBE) {
                        FPSMaster.oobeScreen.nextScene();
                    } else {
                        Minecraft.getMinecraft().displayGuiScreen(new MainMenu());
                    }
                } else {
                    msg = login.get("msg").getAsString();
                    msgbox = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                msg = "未知错误: " + e.getMessage();
                msgbox = true;
            }
        });

        btn2 = new GuiButton(FPSMaster.i18n.get("oobe.login.skip"), () -> {
            if (isOOBE) {
                FPSMaster.oobeScreen.nextScene();
            } else {
                Minecraft.getMinecraft().displayGuiScreen(new MainMenu());
            }
            FPSMaster.configManager.configure.set("username", "offline");
        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        
        Render2DUtils.drawRect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(), new Color(235, 242, 255).getRGB());

        FPSMaster.fontManager.s24.drawCenteredString(FPSMaster.i18n.get("oobe.login.desc"), sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f - 90, FPSMaster.theme.getTextColorDescription().getRGB());
        FPSMaster.fontManager.s18.drawString(FPSMaster.i18n.get("oobe.login.register"), sr.getScaledWidth() / 2f - 90, sr.getScaledHeight() / 2f + 15, FPSMaster.theme.getPrimary().getRGB());
        FPSMaster.fontManager.s40.drawCenteredString(FPSMaster.i18n.get("oobe.login.title"), sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f - 75, FPSMaster.theme.getPrimary().getRGB());

        btn.render(sr.getScaledWidth() / 2f - 70, sr.getScaledHeight() / 2f + 40, 60f, 24f, mouseX, mouseY);
        btn2.render(sr.getScaledWidth() / 2f + 5, sr.getScaledHeight() / 2f + 40, 60f, 24f, mouseX, mouseY);

        username.drawTextBox(sr.getScaledWidth() / 2f - 90, sr.getScaledHeight() / 2f - 40, 180f, 20f);
        password.drawTextBox(sr.getScaledWidth() / 2f - 90, sr.getScaledHeight() / 2f - 10, 180f, 20f);

        msgBoxAnimation.update();
        if (msgbox) {
            msgBoxAnimation.start(new Color(0, 0, 0, 0), new Color(0, 0, 0, 100), 0.6f, Type.EASE_IN_OUT_QUAD);
            Render2DUtils.drawRect(0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(), msgBoxAnimation.getColor());
            Render2DUtils.drawOptimizedRoundedRect(sr.getScaledWidth() / 2f - 100, sr.getScaledHeight() / 2f - 50, 200f, 100f, new Color(255, 255, 255));
            Render2DUtils.drawOptimizedRoundedRect(sr.getScaledWidth() / 2f - 100, sr.getScaledHeight() / 2f - 50, 200f, 20f, new Color(113, 127, 254));

            FPSMaster.fontManager.s18.drawString(FPSMaster.i18n.get("oobe.login.info"), sr.getScaledWidth() / 2f - 90, sr.getScaledHeight() / 2f - 45, -1);
            FPSMaster.fontManager.s18.drawString(msg, sr.getScaledWidth() / 2f - 90, sr.getScaledHeight() / 2f - 20, new Color(60, 60, 60).getRGB());
        } else {
            msgBoxAnimation.start(new Color(0, 0, 0, 100), new Color(0, 0, 0, 0), 0.6f, Type.EASE_IN_OUT_QUAD);
        }
    }

    @Override
    public void mouseClick(int mouseX, int mouseY, int mouseButton) {
        super.mouseClick(mouseX, mouseY, mouseButton);
        btn.mouseClick(mouseX, mouseY, mouseButton);
        btn2.mouseClick(mouseX, mouseY, mouseButton);
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        if (Render2DUtils.isHovered(sr.getScaledWidth() / 2f - 90, sr.getScaledHeight() / 2f + 15, 100f, 10f, mouseX, mouseY) && mouseButton == 0) {
            String url = "https://fpsmaster.top/register";
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (msgbox && msgBoxAnimation.getColor().getAlpha() > 50) {
            msgbox = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        username.textboxKeyTyped(typedChar, keyCode);
        FPSMaster.configManager.configure.set("username", username.getText());
        password.textboxKeyTyped(typedChar, keyCode);
    }
}
