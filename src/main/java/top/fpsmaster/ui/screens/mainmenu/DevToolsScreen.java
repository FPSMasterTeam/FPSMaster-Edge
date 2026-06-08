package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.client.gui.GuiScreen;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.minimap.Minimap;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.config.Configure;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.screens.oobe.OobeScreen;
import top.fpsmaster.utils.render.draw.Rects;
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

        float panelWidth = Math.min(360f, guiWidth - 40f);
        float panelHeight = 232f;
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), Math.round(panelHeight), 8, new Color(0, 0, 0, 150).getRGB());
        FPSMaster.fontManager.s24.drawCenteredString("DevTools", panelX + panelWidth / 2f, panelY + 18f, Color.WHITE.getRGB());
        FPSMaster.fontManager.s16.drawCenteredString("Development-only tools", panelX + panelWidth / 2f, panelY + 44f, new Color(220, 220, 220).getRGB());

        float buttonX = panelX + 24f;
        float buttonWidth = panelWidth - 48f;
        float buttonHeight = 24f;
        float startY = panelY + 72f;
        float gap = 10f;

        clearCachesButton.renderInScreen(this, buttonX, startY, buttonWidth, buttonHeight, mouseX, mouseY);
        recreateConfigButton.renderInScreen(this, buttonX, startY + (buttonHeight + gap), buttonWidth, buttonHeight, mouseX, mouseY);
        openOobeButton.renderInScreen(this, buttonX, startY + 2 * (buttonHeight + gap), buttonWidth, buttonHeight, mouseX, mouseY);
        openClickGuiButton.renderInScreen(this, buttonX, startY + 3 * (buttonHeight + gap), buttonWidth, buttonHeight, mouseX, mouseY);
        backButton.renderInScreen(this, panelX + panelWidth - 94f, panelY + panelHeight - 34f, 70f, 20f, mouseX, mouseY);

        if (!statusMessage.isEmpty()) {
            FPSMaster.fontManager.s16.drawCenteredString(statusMessage, panelX + panelWidth / 2f, panelY + panelHeight - 58f, statusColor);
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
