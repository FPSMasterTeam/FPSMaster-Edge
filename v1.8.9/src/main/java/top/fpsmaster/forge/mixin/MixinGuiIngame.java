package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.features.impl.render.Crosshair;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.font.FontManager;

import java.awt.*;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {
    private static final ResourceLocation BUTTON_CLICK_SOUND = new ResourceLocation("gui.button.press");

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void renderTooltipPost(ScaledResolution sr, float partialTicks, CallbackInfo callbackInfo) {
        EventDispatcher.dispatchEvent(new EventRender2D(partialTicks));
        
        // 渲染服务器延迟信息
        renderPingInfo(sr);
    }

    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    protected void showCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Crosshair.using)
            cir.setReturnValue(false);
    }

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    public void scoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (Scoreboard.using)
            ci.cancel();
    }

    /**
     * 渲染服务器延迟信息
     */
    private void renderPingInfo(ScaledResolution sr) {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler != null) {
            int ping = netHandler.getPlayerInfo(Minecraft.getMinecraft().thePlayer.getUniqueID()).getResponseTime();
            String pingText = "延迟: " + ping + "ms";
            
            // 根据延迟值设置颜色
            int color;
            if (ping < 100) {
                color = Color.GREEN.getRGB();
            } else if (ping < 200) {
                color = Color.YELLOW.getRGB();
            } else {
                color = Color.RED.getRGB();
            }
            
            // 在屏幕右上角显示延迟
            FontManager.fontRegular.drawStringWithShadow(
                pingText,
                sr.getScaledWidth() - FontManager.fontRegular.getStringWidth(pingText) - 5,
                5,
                color
            );
        }
    }

    /**
     * 播放按钮点击音效
     */
    public static void playButtonClickSound() {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(BUTTON_CLICK_SOUND, 1.0F)
        );
    }

    /**
     * 缩小按钮渲染方法
     */
    public static void drawScaledButton(int x, int y, int width, int height, int color) {
        // 缩小按钮尺寸（原尺寸的80%）
        float scale = 0.8f;
        int scaledWidth = (int)(width * scale);
        int scaledHeight = (int)(height * scale);
        int xOffset = (width - scaledWidth) / 2;
        int yOffset = (height - scaledHeight) / 2;
        
        Render2DUtils.drawRoundedRect(
            x + xOffset,
            y + yOffset,
            x + xOffset + scaledWidth,
            y + yOffset + scaledHeight,
            2,
            color
        );
    }
}
