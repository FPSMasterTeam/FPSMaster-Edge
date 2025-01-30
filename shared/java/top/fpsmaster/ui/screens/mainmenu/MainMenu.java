package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.ui.screens.account.GuiWaiting;
import top.fpsmaster.ui.screens.oobe.GuiLogin;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;
import top.fpsmaster.wrapper.TextFormattingProvider;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class MainMenu extends ScaledGuiScreen {

    // Buttons for the main menu
    private final GuiButton singlePlayer;
    private final GuiButton multiPlayer;
    private final GuiButton options;
    private final GuiButton exit;

    private String info = "Failed to get version update";
    private String welcome = "Failed to get version update";
    private boolean needUpdate = false;

    private ResourceLocation textureLocation = null;

    public MainMenu() {
        singlePlayer = new GuiButton("mainmenu.single", () -> {
        ProviderManager.mainmenuProvider.showSinglePlayer(this);
    });
        multiPlayer = new GuiButton("mainmenu.multi", () -> {
        mc.displayGuiScreen(new GuiMultiplayer(this));
    });
        options = new GuiButton("mainmenu.settings", () -> {
        mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
    });
        exit = new GuiButton("X", () -> mc.shutdown());
    }

    @Override
    public void initGui() {
        ProviderManager.mainmenuProvider.initGui();
        if (!MusicPlayer.playList.getMusics().isEmpty()) {
            if (MusicPlayer.isPlaying) {
                MusicPlayer.playList.pause();
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        // Check for background file and render it if available
        if (FileUtils.hasBackground) {
            if (textureLocation == null) {
                textureLocation = new ResourceLocation("fpsmaster/gui/background.png");
                File file = FileUtils.background;
                TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
                ThreadDownloadImageData textureArt = new ThreadDownloadImageData(file, null, null, null);
                textureManager.loadTexture(textureLocation, textureArt);
            }
            Render2DUtils.drawImage(textureLocation, 0f, 0f, this.guiWidth, this.guiHeight, -1);
            Render2DUtils.drawRect(0f, 0f, guiWidth, guiHeight, new Color(22, 22, 22, 50));
        } else {
            ProviderManager.mainmenuProvider.renderSkybox(mouseX, mouseY, partialTicks, (int) this.guiWidth, (int) this.guiHeight, this.zLevel);
            Render2DUtils.drawRect(0f, 0f, guiWidth, guiHeight, new Color(26, 59, 109, 60));
        }

        // Display user info and avatar
        float stringWidth = FPSMaster.fontManager.s16.getStringWidth(mc.getSession().getUsername());
        if (Render2DUtils.isHovered(10f, 10f, 80f, 20f, mouseX, mouseY)) {
            Render2DUtils.drawOptimizedRoundedRect(10f, 10f, 30 + stringWidth, 20f, new Color(0, 0, 0, 100));
            if (Mouse.isButtonDown(0)) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiWaiting());
            }
        } else {
            Render2DUtils.drawOptimizedRoundedRect(10f, 10f, 30 + stringWidth, 20f, new Color(0, 0, 0, 60));
        }
        Render2DUtils.drawImage(new ResourceLocation("client/gui/screen/avatar.png"), 14f, 15f, 10f, 10f, -1);
        FPSMaster.fontManager.s16.drawString(mc.getSession().getUsername(), 28, 16, Color.WHITE.getRGB());
        Render2DUtils.drawImage(new ResourceLocation("client/gui/logo.png"), guiWidth / 2f - 153 / 4f, guiHeight / 2f - 100, 153 / 2f, 67f, -1);

        // Position buttons and render them
        float x = guiWidth / 2f - 50;
        float y = guiHeight / 2f - 30;
        singlePlayer.render(x, y, 100f, 20f, mouseX, mouseY);
        multiPlayer.render(x, y + 24f, 100f, 20f, mouseX, mouseY);
        options.render(x, y + 48f, 70f, 20f, mouseX, mouseY);
        exit.render(x + 74f, y + 48f, 26f, 20f, mouseX, mouseY);

        // Render copyright and other text info
        float w = FPSMaster.fontManager.s16.getStringWidth("Copyright Mojang AB. Do not distribute!");
        FPSMaster.fontManager.s16.drawString("Copyright Mojang AB. Do not distribute!", guiWidth - w - 4, guiHeight - 14, Color.WHITE.getRGB());

        // Display welcome message
        welcome = FPSMaster.INSTANCE.loggedIn ? TextFormattingProvider.getGreen() + String.format(FPSMaster.i18n.get("mainmenu.welcome"), FPSMaster.configManager.configure.getOrCreate("username", "")) : TextFormattingProvider.getRed().toString() + TextFormattingProvider.getBold().toString() + FPSMaster.i18n.get("mainmenu.notlogin");
        FPSMaster.fontManager.s16.drawString(welcome, 4, guiHeight - 52, Color.WHITE.getRGB());

        // Version info
        if (FPSMaster.updateFailed) {
            info = TextFormattingProvider.getGreen() + FPSMaster.i18n.get("mainmenu.failed");
        } else {
            if (FPSMaster.isLatest) {
                info = TextFormattingProvider.getGreen() + FPSMaster.i18n.get("mainmenu.latest");
            } else {
                info = TextFormattingProvider.getRed().toString() + TextFormattingProvider.getBold().toString() + String.format(FPSMaster.i18n.get("mainmenu.notlatest"), FPSMaster.latest);
                needUpdate = true;
            }
        }
        FPSMaster.fontManager.s16.drawString(info, 4, guiHeight - 40, Color.WHITE.getRGB());

        // Render client info
        Render2DUtils.drawRect(0f, 0f, 0f, 0f, -1);
        FPSMaster.fontManager.s16.drawString(FPSMaster.COPYRIGHT, 4, guiHeight - 14, Color.WHITE.getRGB());
        FPSMaster.fontManager.s16.drawString(FPSMaster.CLIENT_NAME + " Client " + FPSMaster.CLIENT_VERSION + " (Minecraft " + FPSMaster.EDITION + ")", 4, guiHeight - 28, Color.WHITE.getRGB());
    }

    @Override
    public void onClick(int mouseX, int mouseY, int mouseButton) {
        singlePlayer.mouseClick(mouseX, mouseY, mouseButton);
        multiPlayer.mouseClick(mouseX, mouseY, mouseButton);
        options.mouseClick(mouseX, mouseY, mouseButton);
        exit.mouseClick(mouseX, mouseY, mouseButton);

        float uw = FPSMaster.fontManager.s16.getStringWidth(info);
        float nw = FPSMaster.fontManager.s16.getStringWidth(info);

        if (mouseButton == 0) {
            if (Render2DUtils.isHovered(4f, guiHeight - 52, nw, 14f, mouseX, mouseY)) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiLogin());
            }

            if (Render2DUtils.isHovered(4f, guiHeight - 40, uw, 14f, mouseX, mouseY) && needUpdate) {
                try {
                    Desktop.getDesktop().browse(new URI("https://fpsmaster.top/download"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
