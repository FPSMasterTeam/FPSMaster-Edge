package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.HudBreakdown;

/**
 * Times each part of the vanilla overlay by name.
 *
 * <p>The HUD section is 689us and our own components account for 3.2us of it, so almost all of it
 * belongs to the overlay Forge and vanilla draw. These are separate methods, which makes them
 * straightforward to charge individually — and unlike removing them, timing them cannot report zero
 * for work that was happening or hide cost that would simply move.
 *
 * <p>None of these nest, so one field is enough to carry the start time. The methods Forge adds
 * rather than overrides are not obfuscated, so their injections opt out of remapping; the two
 * that override vanilla keep it.
 */
@Mixin(GuiIngameForge.class)
public class GuiIngameForgeMixin_HudBreakdown {

    @Unique
    private long edge$started;

    @Unique
    private void edge$begin() {
        if (HudBreakdown.enabled()) {
            edge$started = System.nanoTime();
        }
    }

    @Unique
    private void edge$end(String name) {
        if (HudBreakdown.enabled() && edge$started != 0L) {
            HudBreakdown.record(name, System.nanoTime() - edge$started);
            edge$started = 0L;
        }
    }

    @Inject(method = "renderChat", at = @At("HEAD"), remap = false)
    private void edge$chatStart(int width, int height, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderChat", at = @At("RETURN"), remap = false)
    private void edge$chatEnd(int width, int height, CallbackInfo ci) {
        edge$end("chat");
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), remap = false)
    private void edge$listStart(int width, int height, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderPlayerList", at = @At("RETURN"), remap = false)
    private void edge$listEnd(int width, int height, CallbackInfo ci) {
        edge$end("player list");
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void edge$tooltipStart(ScaledResolution resolution, float partialTicks, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void edge$tooltipEnd(ScaledResolution resolution, float partialTicks, CallbackInfo ci) {
        edge$end("hotbar");
    }

    @Inject(method = "renderCrosshairs", at = @At("HEAD"), remap = false)
    private void edge$crossStart(int width, int height, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderCrosshairs", at = @At("RETURN"), remap = false)
    private void edge$crossEnd(int width, int height, CallbackInfo ci) {
        edge$end("crosshair");
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"))
    private void edge$bossStart(CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderBossHealth", at = @At("RETURN"))
    private void edge$bossEnd(CallbackInfo ci) {
        edge$end("boss health");
    }

    @Inject(method = "renderTitle", at = @At("HEAD"), remap = false)
    private void edge$titleStart(int width, int height, float partialTicks, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderTitle", at = @At("RETURN"), remap = false)
    private void edge$titleEnd(int width, int height, float partialTicks, CallbackInfo ci) {
        edge$end("title");
    }

    @Inject(method = "renderToolHightlight", at = @At("HEAD"), remap = false)
    private void edge$toolStart(ScaledResolution resolution, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderToolHightlight", at = @At("RETURN"), remap = false)
    private void edge$toolEnd(ScaledResolution resolution, CallbackInfo ci) {
        edge$end("tool highlight");
    }

    @Inject(method = "renderHUDText", at = @At("HEAD"), remap = false)
    private void edge$debugStart(int width, int height, CallbackInfo ci) {
        edge$begin();
    }

    @Inject(method = "renderHUDText", at = @At("RETURN"), remap = false)
    private void edge$debugEnd(int width, int height, CallbackInfo ci) {
        edge$end("debug text");
    }
}
