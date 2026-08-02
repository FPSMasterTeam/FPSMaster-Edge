package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventMotionBlur;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.impl.interfaces.CustomTitles;
import top.fpsmaster.features.impl.interfaces.SaturationDisplay;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {
    @Shadow(remap = false)
    public static int right_height;

    @Inject(method = "renderGameOverlay", at = @At("HEAD"))
    private void fpsmasterBeginHud(float partialTicks, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_HUD);
        }
    }

    @Inject(method = "renderGameOverlay",at = @At("RETURN"))
    public void motionblur(float partialTicks, CallbackInfo ci){
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_HUD);
        }
        EventDispatcher.dispatchEvent(new EventMotionBlur());
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void renderTooltipPost(ScaledResolution sr, float partialTicks, CallbackInfo callbackInfo) {
        EventDispatcher.dispatchEvent(new EventRender2D(partialTicks));
    }

    @Shadow(remap = false)
    private boolean pre(RenderGameOverlayEvent.ElementType type) {
        throw new AssertionError();
    }

    /**
     * 接管原版饥饿条。
     *
     * <p>不在 HEAD 直接 cancel：renderFood 的第一句是 {@code if (pre(FOOD)) return;}，在它之前
     * cancel 会让 Pre(FOOD) 事件根本不派发，别的 HUD 模组的钩子就静默失效了。改成重定向这次
     * {@code pre} 调用：事件照常触发，别人已经取消时把占位交给他们，只有我们真正接管时才补上
     * renderFood 末尾那个被跳过的 {@code right_height += 10}，否则氧气条和坐骑血条会错位。
     */
    @Redirect(method = "renderFood", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z"),
            remap = false)
    private boolean fpsmaster$takeOverFood(GuiIngameForge self, RenderGameOverlayEvent.ElementType type) {
        if (pre(type)) {
            return true;
        }
        if (!SaturationDisplay.using) {
            return false;
        }
        right_height += 10;
        return true;
    }

    @Redirect(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    public void drawString(float x, float y, float z) {
        GlStateManager.translate(x + CustomTitles.getX(), y + CustomTitles.getY(), z);
    }

    @Redirect(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    public void scale(float x, float y, float z) {
        float scale = CustomTitles.getScale();
        GlStateManager.scale(x * scale, y * scale, z * scale);
    }
}



