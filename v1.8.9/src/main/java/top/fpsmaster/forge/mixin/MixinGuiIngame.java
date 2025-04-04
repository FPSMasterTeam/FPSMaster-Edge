package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.features.impl.render.Crosshair;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {
    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void renderTooltipPost(ScaledResolution sr, float partialTicks, CallbackInfo callbackInfo) {
        EventDispatcher.dispatchEvent(new EventRender2D(partialTicks));
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

    @Inject(method = "renderBlockIndicator", at = @At("HEAD"))
    private void renderBlockIndicator(ScaledResolution sr, CallbackInfo ci) {
        if (top.fpsmaster.forge.Mod.blockIndicatorEnabled) {
            RayTraceResult result = Minecraft.getMinecraft().objectMouseOver;
            if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = result.getBlockPos();
                IBlockState state = Minecraft.getMinecraft().theWorld.getBlockState(pos);
                Block block = state.getBlock();

                // 绘制方块指示器
                GlStateManager.pushMatrix();
                RenderGlobal.drawSelectionBoundingBox(pos, 1.0F, 0.0F, 0.0F, 0.5F);
                GlStateManager.popMatrix();
            }
        }
    }
}