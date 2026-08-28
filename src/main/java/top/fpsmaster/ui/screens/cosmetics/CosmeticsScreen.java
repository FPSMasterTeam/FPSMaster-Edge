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
import top.fpsmaster.cosmetic.RemoteCosmeticService;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.ApiResponse;
import top.fpsmaster.modules.client.api.model.UserInfo;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.CosmeticsBridge;
import top.fpsmaster.prism.screen.SharedCosmetics;
import top.fpsmaster.prism.widget.UiFrame;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    public void initGui() {
        super.initGui();
        // 余额要显示在表头和确认弹窗上，缓存可能是上次开客户端时拉的。
        //
        // 放在 initGui() 而不是构造里：从这里跳去登录界面时传的 parent 就是本实例，
        // 登录完回来构造函数不会再跑一遍，余额会一直停在「未知」。改窗口大小也会重跑
        // initGui()，所以走带节流的那条，拖窗口不会变成一串请求。
        FPSMasterApiClient.getInstance().refreshProfileIfStale();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        itemPreviews.clear();
        if (cosmetics.draw(EdgeUi.frame(), bridge)) {
            mc.displayGuiScreen(parent);
            return;
        }
        // 弹窗开着就不画预览了。饰品缩略图和玩家模型都在 2D 通道之后画（弹窗是 2D 通道
        // 里最后一笔），顺序改不了，所以它们必然盖在确认框上——默认窗口尺寸下缩略图正好
        // 糊住价格和余额那两行。模态本来就该压住底下的东西，这里跟着一起收。
        if (cosmetics.blocking()) return;
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
        if (keyCode == Keyboard.KEY_ESCAPE) {
            // 弹窗挡着的时候 ESC 先关弹窗：直接退出整个界面会让人以为自己已经买了。
            if (cosmetics.blocking()) cosmetics.closeDialog();
            else mc.displayGuiScreen(parent);
        } else super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        bridge.cosmetics.setPreviewing(false);
        bridge.cosmetics.clearPreview();
        RemoteCosmeticService.getInstance().flush();
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
        private volatile String status = "";

        @Override
        public String i18n(String key) { return FPSMaster.i18n.get(key); }
        @Override
        public String playerName() {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft.thePlayer == null ? "Steve" : minecraft.thePlayer.getName();
        }
        @Override
        public List<CosmeticsBridge.Item> items() {
            List<CosmeticsBridge.Item> result = new ArrayList<>();
            for (CosmeticManager.CosmeticOption option : cosmetics.allOptions()) {
                boolean builtin = CosmeticManager.BUILTIN_WINGS_ID.equals(option.getId());
                // The last four carry the item's scale policy: the shared screen reads the slider
                // range off the equipped back item, which is what keeps a fixed-size cosmetic
                // (every non-wings category, elytra included) locked at its authored size.
                result.add(new CosmeticsBridge.Item(
                        option.getId(),
                        builtin ? FPSMaster.i18n.get("cosmetics.wings.builtin") : option.getName(),
                        option.getDescription(),
                        option.getCategory(),
                        option.getPrice(),
                        cosmetics.isOwned(option.getId()),
                        cosmetics.isEquipped(option.getId()),
                        builtin,
                        option.getDefaultScale(),
                        option.isScaleAdjustable(),
                        option.getMinScale(),
                        option.getMaxScale()
                ));
            }
            return result;
        }
        @Override
        public void previewItem(String id) { cosmetics.preview(id); }
        @Override
        public void equipItem(String id) { cosmetics.equip(id); }
        @Override
        public boolean signedIn() { return AuthService.getInstance().isLoggedIn(); }
        // 加 @Override 不只是风格：CosmeticsBridge.balance() 是带默认实现的，
        // 签名一旦漂了这里会静默退回默认的空串，表头上的余额就永远是空的。
        @Override
        public String balance() {
            UserInfo user = FPSMasterApiClient.getInstance().cachedUser();
            String value = user == null ? null : user.getWalletBalance();
            return value == null ? "" : value;
        }
        // 余额不足弹窗每帧都会调这个，节流全靠 refreshProfileIfStale 自己那道 5 秒闸。
        @Override
        public void refreshBalance() {
            FPSMasterApiClient.getInstance().refreshProfileIfStale(5000L);
        }
        @Override
        public void openSignIn() {
            // 从饰品界面进登录界面，登录完退回来的就是饰品界面本身，购买按钮当场就活了。
            Minecraft.getMinecraft().displayGuiScreen(
                    new top.fpsmaster.ui.screens.signin.SignInScreen(CosmeticsScreen.this));
        }
        @Override
        public boolean purchasePending() { return FPSMasterApiClient.getInstance().purchaseInProgress(); }
        @Override
        public String statusMessage() { return status; }
        @Override
        public void openCustomFolder() { cosmetics.openCustomDirectory(); }
        @Override
        public void purchaseItem(String id) {
            // 解析不了的 id 以前是静默 return：玩家点了「确认购买」，弹窗关掉、余额没动、
            // 一个字都不显示，看起来就是「客户端坏了」。目录里正常商品的 id 都是数字，
            // 走到这儿说明后端发了个这个版本认不出来的东西，得说一声。
            final long itemId;
            try {
                itemId = Long.parseLong(id);
            } catch (NumberFormatException ignored) {
                status = FPSMaster.i18n.get("cosmetics.purchase.failed");
                return;
            }
            // null = 已经有一单在途（可能是上一个饰品界面下的），不重复下单。
            CompletableFuture<ApiResponse<JsonObject>> request =
                    FPSMasterApiClient.getInstance().purchaseItem(itemId);
            if (request == null) {
                return;
            }
            status = FPSMaster.i18n.get("cosmetics.purchasing");
            request.whenComplete((response, error) -> {
                if (error == null && response != null && response.isSuccess()) {
                    cosmetics.grantPurchasedAndEquip(id);
                    cosmetics.refreshOwned();
                    // 下单响应是一张订单，不带新余额，只能自己再拉一次 profile，
                    // 否则表头一直显示扣款前的数。
                    FPSMasterApiClient.getInstance().refreshProfileNow();
                    status = FPSMaster.i18n.get("cosmetics.purchase.success");
                } else {
                    status = error != null ? error.getMessage()
                            : response == null ? FPSMaster.i18n.get("cosmetics.purchase.failed") : response.getMessage();
                }
            });
        }
        @Override
        public String syncStatus() { return RemoteCosmeticService.getInstance().syncStatusKey(); }
        @Override
        public boolean capeEnabled() { return cosmetics.capeAnimationEnabled(); }
        @Override
        public void setCapeEnabled(boolean enabled) { cosmetics.setCapeAnimationEnabled(enabled); }
        @Override
        public float wingScale() { return cosmetics.wingScale(); }
        @Override
        public void setWingScale(float scale) { cosmetics.setWingScale(scale); }
        @Override
        public boolean wingScaleAdjustable() { return cosmetics.wingScaleAdjustable(); }
        @Override
        public void paintItemPreview(UiFrame ui, CosmeticsBridge.Item item,
                                     float x, float y, float w, float h) {
            itemPreviews.add(new ItemPreview(item, x, y, w, h));
        }
        @Override
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
