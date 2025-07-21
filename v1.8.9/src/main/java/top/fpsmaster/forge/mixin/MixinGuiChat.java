package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.java_websocket.enums.ReadyState;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

@Mixin(GuiChat.class)
public class MixinGuiChat extends GuiScreen {

    @Unique
    private static boolean irc = false;


    @Inject(method = "drawScreen", at = @At("HEAD"))
    public void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (FPSMaster.INSTANCE.wsClient != null && FPSMaster.INSTANCE.wsClient.getReadyState() == ReadyState.OPEN) {
            int width1 = mc.fontRendererObj.getStringWidth(FPSMaster.i18n.get("chat.mc"));
            int width2 = mc.fontRendererObj.getStringWidth(FPSMaster.i18n.get("chat.irc"));


            Gui.drawRect(2, this.height - 28, 2 + width1 + 4, this.height - 14, irc ? new Color(0, 0, 0, 180).getRGB() : new Color(80, 80, 80, 180).getRGB());
            mc.fontRendererObj.drawStringWithShadow(FPSMaster.i18n.get("chat.mc"), 4, this.height - 26, irc ? new Color(200, 200, 200).getRGB() : -1);

            Gui.drawRect(2 + width1 + 4, this.height - 28, 2 + width1 + 6 + width2 + 2, this.height - 14, irc ? new Color(80, 80, 80, 180).getRGB() : new Color(0, 0, 0, 180).getRGB());
            mc.fontRendererObj.drawStringWithShadow(FPSMaster.i18n.get("chat.irc"), 4 + width1 + 4, this.height - 26, irc ? -1 : new Color(200, 200, 200).getRGB());

            if (Mouse.isButtonDown(0)) {
                if (Render2DUtils.isHovered(2, this.height - 28, width1 + 4, 12, mouseX, mouseY)) {
                    irc = false;
                } else if (Render2DUtils.isHovered(2 + width1 + 4, this.height - 28, width2 + 2, 12, mouseX, mouseY)) {
                    irc = true;
                }
            }
        }
    }


    @Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;sendChatMessage(Ljava/lang/String;)V"))
    public void sendChatMessage(GuiChat instance, String message) {
        if (irc && FPSMaster.INSTANCE.wsClient != null && FPSMaster.INSTANCE.wsClient.getReadyState() == ReadyState.OPEN) {
            FPSMaster.INSTANCE.wsClient.sendMessage(message);
        } else {
            instance.sendChatMessage(message);
        }
    }
}
