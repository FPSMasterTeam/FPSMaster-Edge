package top.fpsmaster.ui.screens.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
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
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");
    private static final ModelPlayer STEVE_MODEL = new ModelPlayer(0f, false);
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
        if (preview[2] <= 0f) return;
        int centerX = Math.round(preview[0] + preview[2] * 0.5f);
        int feetY = Math.round(preview[1] + preview[3] - 24f);
        int size = Math.max(22, Math.round(preview[3] * 0.31f));
        float lookX = (float) Math.sin(Math.toRadians(preview[4])) * 30f;
        float modelYaw = (float) Math.toDegrees(Math.atan(lookX / 40f));
        if (bridge.wingsEnabled()) bridge.wings.renderPreview(centerX, feetY, size, modelYaw);
        if (mc.thePlayer == null) renderSteve(centerX, feetY, size, modelYaw);
        else GuiInventory.drawEntityOnScreen(centerX, feetY, size, lookX, 0f, mc.thePlayer);
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

    private void renderSteve(int x, int y, int size, float yaw) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50f);
        GlStateManager.scale(-size, size, size);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        GlStateManager.rotate(yaw, 0f, 1f, 0f);
        RenderHelper.enableStandardItemLighting();
        mc.getTextureManager().bindTexture(STEVE_SKIN);
        float unit = 0.0625f;
        STEVE_MODEL.bipedHead.render(unit);
        STEVE_MODEL.bipedHeadwear.render(unit);
        STEVE_MODEL.bipedBody.render(unit);
        STEVE_MODEL.bipedBodyWear.render(unit);
        STEVE_MODEL.bipedRightArm.render(unit);
        STEVE_MODEL.bipedRightArmwear.render(unit);
        STEVE_MODEL.bipedLeftArm.render(unit);
        STEVE_MODEL.bipedLeftArmwear.render(unit);
        STEVE_MODEL.bipedRightLeg.render(unit);
        STEVE_MODEL.bipedRightLegwear.render(unit);
        STEVE_MODEL.bipedLeftLeg.render(unit);
        STEVE_MODEL.bipedLeftLegwear.render(unit);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }

    private final class EdgeCosmeticsBridge implements CosmeticsBridge {
        private final DragonWings wings = FPSMaster.moduleManager.getModule(DragonWings.class);
        private final WavyCape cape = FPSMaster.moduleManager.getModule(WavyCape.class);

        public String i18n(String key) { return FPSMaster.i18n.get(key); }
        public String playerName() {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft.thePlayer == null ? "Steve" : minecraft.thePlayer.getName();
        }
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
