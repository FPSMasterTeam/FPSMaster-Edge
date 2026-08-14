package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.client.gui.GuiScreen;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.minimap.Minimap;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.config.Configure;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.screens.oobe.OobeScreen;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;
import java.io.File;

public class DevToolsScreen extends ScaledGuiScreen {
    private final GuiScreen parent;
    private final GuiButton backButton;
    private final GuiButton clearCachesButton;
    private final GuiButton recreateConfigButton;
    private final GuiButton openOobeButton;
    private final GuiButton openClickGuiButton;

    private String statusMessage = "";
    private int statusColor = new Color(220, 220, 220).getRGB();

    public DevToolsScreen(GuiScreen parent) {
        this.parent = parent;
        this.backButton = new GuiButton("Back", () -> mc.displayGuiScreen(parent)).setText("Back", false);
        this.clearCachesButton = new GuiButton("Clear Caches", this::clearCaches).setText("Clear Caches", false);
        this.recreateConfigButton = new GuiButton("Recreate Default Config", this::recreateDefaultConfig).setText("Recreate Default Config", false);
        this.openOobeButton = new GuiButton("Open OOBE", () -> mc.displayGuiScreen(new OobeScreen())).setText("Open OOBE", false);
        this.openClickGuiButton = new GuiButton("Open Click GUI", () -> mc.displayGuiScreen(FPSMaster.moduleManager.mainPanel)).setText("Open Click GUI", false);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);

        float panelWidth = Math.min(180f, guiWidth - 24f);
        float panelHeight = 128f;
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        UiChrome.panel(panelX, panelY, panelWidth, panelHeight);
        UiChrome.boldString(FPSMaster.fontManager.s16, "DevTools", panelX + 11f, panelY + 10f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(12).drawString("Development-only tools", panelX + 11f, panelY + 21f,
                ClickGuiTheme.textSecondary().getRGB());

        float buttonX = panelX + 11f;
        float buttonWidth = panelWidth - 22f;
        float buttonHeight = UiChrome.BTN_H;
        float startY = panelY + 34f;
        float gap = 4f;

        clearCachesButton.renderInScreen(this, buttonX, startY, buttonWidth, buttonHeight, mouseX, mouseY);
        recreateConfigButton.renderInScreen(this, buttonX, startY + (buttonHeight + gap), buttonWidth, buttonHeight, mouseX, mouseY);
        openOobeButton.renderInScreen(this, buttonX, startY + 2 * (buttonHeight + gap), buttonWidth, buttonHeight, mouseX, mouseY);
        openClickGuiButton.renderInScreen(this, buttonX, startY + 3 * (buttonHeight + gap), buttonWidth, buttonHeight, mouseX, mouseY);
        backButton.renderInScreen(this, panelX + panelWidth - 11f - 40f, panelY + panelHeight - 24f, 40f, 16f, mouseX, mouseY);

        if (!statusMessage.isEmpty()) {
            FPSMaster.fontManager.getFont(12).drawString(statusMessage, buttonX, panelY + panelHeight - 22f, statusColor);
        }
    }

    private void clearCaches() {
        Backgrounds.initGui();
        Minimap.clearBlockColours = true;
        FPSMaster.fontManager.load();
        setStatus("Caches cleared", new Color(110, 255, 150).getRGB());
    }

    private void recreateDefaultConfig() {
        try {
            File configFile = ConfigProfileUtils.getCurrentConfigFile();
            if (configFile.exists() && !configFile.delete()) {
                throw new FileException("Failed to delete config: " + configFile.getAbsolutePath());
            }
            FPSMaster.configManager.configure = new Configure();
            FPSMaster.configManager.loadConfig("default");
            setStatus("Default config recreated", new Color(110, 255, 150).getRGB());
            mc.displayGuiScreen(new MainMenu());
        } catch (Exception exception) {
            ClientLogger.error("Failed to recreate default config");
            setStatus("Failed to recreate config", new Color(255, 120, 120).getRGB());
        }
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
