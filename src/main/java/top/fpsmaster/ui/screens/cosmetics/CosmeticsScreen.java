package top.fpsmaster.ui.screens.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.cosmetic.CosmeticManager;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.CosmeticsBridge;
import top.fpsmaster.prism.screen.SharedCosmetics;
import top.fpsmaster.prism.widget.UiFrame;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CosmeticsScreen extends ScaledGuiScreen {
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");
    private static final ModelPlayer STEVE_MODEL = new ModelPlayer(0f, false);
    private final GuiScreen parent;
    private final SharedCosmetics cosmetics = new SharedCosmetics();
    private final EdgeCosmeticsBridge bridge = new EdgeCosmeticsBridge();
    private final float[] preview = new float[5];
    private final List<ItemPreview> itemPreviews = new ArrayList<>();

    public CosmeticsScreen(GuiScreen parent) {
        this.parent = parent;
        bridge.cosmetics.reloadCustom();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        itemPreviews.clear();
        if (cosmetics.draw(EdgeUi.frame(), bridge)) {
            mc.displayGuiScreen(parent);
            return;
        }
        renderItemPreviews();
        if (preview[2] <= 0f) return;
        int centerX = Math.round(preview[0] + preview[2] * 0.5f);
        int feetY = Math.round(preview[1] + preview[3] - 24f);
        int size = Math.max(22, Math.round(preview[3] * 0.42f));
        float yaw = preview[4];
        bridge.cosmetics.setPreviewing(true);
        bridge.cosmetics.wingsRenderer().renderPreview(centerX, feetY, size, yaw);
        if (mc.thePlayer == null) renderSteve(centerX, feetY, size, yaw);
        else renderPlayer(centerX, feetY, size, yaw, mc.thePlayer);
    }

    private void renderItemPreviews() {
        for (ItemPreview itemPreview : itemPreviews) {
            ResourceLocation texture = bridge.cosmetics.textureFor(itemPreview.item.id());
            if ("wings".equals(itemPreview.item.category())) {
                if (!itemPreview.item.builtin() && texture == null) continue;
                float size = Math.max(12f, itemPreview.h * 0.4f);
                bridge.cosmetics.wingsRenderer().renderPreview(
                        itemPreview.x + itemPreview.w * 0.5f,
                        itemPreview.y + itemPreview.h - 1f,
                        size,
                        180f,
                        texture,
                        0.78f
                );
            } else if ("cape".equals(itemPreview.item.category()) && texture != null) {
                renderCapeThumbnail(itemPreview, texture);
            }
        }
    }

    private void renderCapeThumbnail(ItemPreview itemPreview, ResourceLocation texture) {
        float size = Math.max(18f, itemPreview.h * 0.76f);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(itemPreview.x + itemPreview.w * 0.5f,
                itemPreview.y + itemPreview.h - 2f, 50f);
        GlStateManager.scale(-size, size, size);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        GlStateManager.rotate(8f, 1f, 0f, 0f);
        RenderHelper.enableStandardItemLighting();
        mc.getTextureManager().bindTexture(texture);
        STEVE_MODEL.renderCape(0.0625f);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        bridge.cosmetics.setPreviewing(false);
        bridge.cosmetics.clearPreview();
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
        ResourceLocation cape = bridge.cosmetics.capeTexture();
        if (cape != null) {
            mc.getTextureManager().bindTexture(cape);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, 0f, 0.125f);
            GlStateManager.rotate(6f, 1f, 0f, 0f);
            STEVE_MODEL.renderCape(unit);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }

    private void renderPlayer(int x, int y, int size, float yaw, EntityLivingBase player) {
        float bodyYaw = player.renderYawOffset;
        float rotationYaw = player.rotationYaw;
        float rotationPitch = player.rotationPitch;
        float previousHeadYaw = player.prevRotationYawHead;
        float headYaw = player.rotationYawHead;
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50f);
        GlStateManager.scale(-size, size, size);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        RenderHelper.enableStandardItemLighting();
        player.renderYawOffset = yaw;
        player.rotationYaw = yaw;
        player.rotationPitch = 0f;
        player.prevRotationYawHead = yaw;
        player.rotationYawHead = yaw;
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        renderManager.setPlayerViewY(180f);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(player, 0d, 0d, 0d, 0f, 1f);
        renderManager.setRenderShadow(true);
        player.renderYawOffset = bodyYaw;
        player.rotationYaw = rotationYaw;
        player.rotationPitch = rotationPitch;
        player.prevRotationYawHead = previousHeadYaw;
        player.rotationYawHead = headYaw;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }

    private final class EdgeCosmeticsBridge implements CosmeticsBridge {
        private final CosmeticManager cosmetics = CosmeticManager.getInstance();
        private volatile boolean purchasing;
        private volatile String status = "";

        public String i18n(String key) { return FPSMaster.i18n.get(key); }
        public String playerName() {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft.thePlayer == null ? "Steve" : minecraft.thePlayer.getName();
        }
        public List<CosmeticsBridge.Item> items() {
            List<CosmeticsBridge.Item> result = new ArrayList<>();
            for (CosmeticManager.CosmeticOption option : cosmetics.allOptions()) {
                boolean builtin = CosmeticManager.BUILTIN_WINGS_ID.equals(option.getId());
                result.add(new CosmeticsBridge.Item(
                        option.getId(),
                        builtin ? FPSMaster.i18n.get("cosmetics.wings.builtin") : option.getName(),
                        option.getDescription(),
                        option.getCategory(),
                        option.getPrice(),
                        cosmetics.isOwned(option.getId()),
                        cosmetics.isEquipped(option.getId()),
                        builtin
                ));
            }
            return result;
        }
        public void previewItem(String id) { cosmetics.preview(id); }
        public void equipItem(String id) { cosmetics.equip(id); }
        public boolean signedIn() { return AuthService.getInstance().isLoggedIn(); }
        public boolean purchasePending() { return purchasing; }
        public String statusMessage() { return status; }
        public void purchaseItem(String id) {
            if (purchasing) return;
            final long itemId;
            try {
                itemId = Long.parseLong(id);
            } catch (NumberFormatException ignored) {
                return;
            }
            purchasing = true;
            status = FPSMaster.i18n.get("cosmetics.purchasing");
            FPSMasterApiClient.getInstance().purchaseItem(itemId).whenComplete((response, error) -> {
                purchasing = false;
                if (error == null && response != null && response.isSuccess()) {
                    cosmetics.grantPurchasedAndEquip(id);
                    cosmetics.refreshOwned();
                    status = FPSMaster.i18n.get("cosmetics.purchase.success");
                } else {
                    status = error != null ? error.getMessage()
                            : response == null ? FPSMaster.i18n.get("cosmetics.purchase.failed") : response.getMessage();
                }
            });
        }
        public boolean capeEnabled() { return cosmetics.capeAnimationEnabled(); }
        public void setCapeEnabled(boolean enabled) { cosmetics.setCapeAnimationEnabled(enabled); }
        public float wingScale() { return cosmetics.wingScale(); }
        public void setWingScale(float scale) { cosmetics.setWingScale(scale); }
        public boolean wingScaleAdjustable() { return cosmetics.wingScaleAdjustable(); }
        public void paintItemPreview(UiFrame ui, CosmeticsBridge.Item item,
                                     float x, float y, float w, float h) {
            itemPreviews.add(new ItemPreview(item, x, y, w, h));
        }
        public void paintPlayerPreview(UiFrame ui, float x, float y, float w, float h, float yaw) {
            preview[0] = x; preview[1] = y; preview[2] = w; preview[3] = h; preview[4] = yaw;
        }
    }

    private static final class ItemPreview {
        private final CosmeticsBridge.Item item;
        private final float x;
        private final float y;
        private final float w;
        private final float h;

        private ItemPreview(CosmeticsBridge.Item item, float x, float y, float w, float h) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
