package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerSelectionList;
import net.minecraft.client.network.ServerData;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen extends Gui {

    @Shadow
    protected abstract void keyTyped(char typedChar, int keyCode);

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    public abstract void drawBackground(int tint);

    private long lastClickTime = 0;
    private boolean doubleClicked = false;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton == 0) { // 左键
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250) {
                doubleClicked = true;
                lastClickTime = 0;
                // 检查是否在服务器列表中
                if (Minecraft.getMinecraft().currentScreen instanceof GuiMultiplayer) {
                    GuiMultiplayer guiMultiplayer = (GuiMultiplayer) Minecraft.getMinecraft().currentScreen;
                    if (guiMultiplayer.serverList != null && guiMultiplayer.serverList.getSelectedServer() >= 0) {
                        ServerData serverData = guiMultiplayer.serverList.getServerData(guiMultiplayer.serverList.getSelectedServer());
                        Minecraft.getMinecraft().connect(serverData);
                        ci.cancel();
                    }
                }
            } else {
                lastClickTime = currentTime;
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        doubleClicked = false;
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void drawBlockIndicator(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (top.fpsmaster.forge.Mod.blockIndicatorEnabled) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int scaledWidth = sr.getScaledWidth();
            int scaledHeight = sr.getScaledHeight();

            // 绘制方块指示器
            GlStateManager.pushMatrix();
            Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/block_indicator.png"), 
                top.fpsmaster.forge.Mod.blockIndicatorX, top.fpsmaster.forge.Mod.blockIndicatorY, top.fpsmaster.forge.Mod.blockIndicatorSize, top.fpsmaster.forge.Mod.blockIndicatorSize, -1);
            GlStateManager.popMatrix();

            // 绘制方块信息
            String blockName = "Block Name";
            String version = "Minecraft";
            Minecraft.getMinecraft().fontRendererObj.drawString(blockName, top.fpsmaster.forge.Mod.blockIndicatorX + top.fpsmaster.forge.Mod.blockIndicatorSize + 5, top.fpsmaster.forge.Mod.blockIndicatorY, 0xFFFFFF);
            Minecraft.getMinecraft().fontRendererObj.drawString(version, top.fpsmaster.forge.Mod.blockIndicatorX + top.fpsmaster.forge.Mod.blockIndicatorSize + 5, top.fpsmaster.forge.Mod.blockIndicatorY + 10, 0xFFFFFF);
        }
    }
}