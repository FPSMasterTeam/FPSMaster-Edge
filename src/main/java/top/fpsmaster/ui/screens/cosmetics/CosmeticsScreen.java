package top.fpsmaster.ui.screens.cosmetics;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.optimizes.WavyCape;
import top.fpsmaster.features.impl.render.DragonWings;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.CosmeticsBridge;
import top.fpsmaster.prism.screen.SharedCosmetics;
import top.fpsmaster.prism.widget.UiFrame;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;

public final class CosmeticsScreen extends ScaledGuiScreen {
    private final GuiScreen parent;
    private final SharedCosmetics cosmetics = new SharedCosmetics();
    private final EdgeCosmeticsBridge bridge = new EdgeCosmeticsBridge();
    private final float[] preview = new float[5];

    public CosmeticsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (cosmetics.draw(EdgeUi.frame(), bridge)) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (mc.thePlayer == null || preview[2] <= 0f) return;
        int centerX = Math.round(preview[0] + preview[2] * 0.5f);
        int feetY = Math.round(preview[1] + preview[3] - 24f);
        int size = Math.max(22, Math.round(preview[3] * 0.31f));
        float lookX = (float) Math.sin(Math.toRadians(preview[4])) * 30f;
        GuiInventory.drawEntityOnScreen(centerX, feetY, size, lookX, 0f, mc.thePlayer);
        if (bridge.wingsEnabled()) bridge.wings.renderPreview(centerX, feetY, size, preview[4]);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        try {
            FPSMaster.configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
        } catch (FileException e) {
            ClientLogger.error("Failed to save cosmetics settings: " + e.getMessage());
        }
        super.onGuiClosed();
    }

    private final class EdgeCosmeticsBridge implements CosmeticsBridge {
        private final DragonWings wings = FPSMaster.moduleManager.getModule(DragonWings.class);
        private final WavyCape cape = FPSMaster.moduleManager.getModule(WavyCape.class);

        public String i18n(String key) { return FPSMaster.i18n.get(key); }
        public String playerName() { return mc.thePlayer == null ? "Steve" : mc.thePlayer.getName(); }
        public boolean capeEnabled() { return cape.isEnabled(); }
        public void setCapeEnabled(boolean enabled) { cape.set(enabled); }
        public boolean wingsEnabled() { return wings.isEnabled(); }
        public void setWingsEnabled(boolean enabled) { wings.set(enabled); }
        public float wingScale() { return wings.scale.getValue().floatValue() / 100f; }
        public void setWingScale(float scale) { wings.scale.setValue(scale * 100f); }
        public void paintPlayerPreview(UiFrame ui, float x, float y, float w, float h, float yaw) {
            preview[0] = x; preview[1] = y; preview[2] = w; preview[3] = h; preview[4] = yaw;
        }
    }
}
