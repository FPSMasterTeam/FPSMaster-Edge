package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.Experiments;
import top.fpsmaster.benchmark.SkyBreakdown;

/**
 * Times and optionally skips each kind of call the sky pass makes.
 *
 * <p>Subtraction failed to attribute this pass: no part removed on its own accounted for more than
 * 6% of it, while removing the whole method accounted for all of it, because some of the cost simply
 * moves to whatever draws next. Timing in place cannot move. Every call the method makes is routed
 * through here and charged to a bucket; whatever the section total exceeds the sum of the buckets by
 * is cost that belongs to no call at all.
 *
 * <pre>
 *   -Dedge.exp.skyBreakdown=true       time every call and report periodically
 *   -Dedge.exp.noSkyStateToggles=true  skip the fixed-function state changes
 * </pre>
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_SkyState {

    private static boolean skipState() {
        return Experiments.active(Experiments.NO_SKY_STATE_TOGGLES);
    }

    @Inject(method = "renderSky(FI)V", at = @At("RETURN"))
    private void skyPassEnded(float partialTicks, int pass, CallbackInfo callback) {
        if (SkyBreakdown.enabled()) {
            SkyBreakdown.endPass();
        }
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableTexture2D()V"))
    private void sky_disableTexture2D() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.disableTexture2D();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.disableTexture2D();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableTexture2D()V"))
    private void sky_enableTexture2D() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.enableTexture2D();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.enableTexture2D();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableFog()V"))
    private void sky_enableFog() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.enableFog();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.enableFog();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableFog()V"))
    private void sky_disableFog() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.disableFog();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.disableFog();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableAlpha()V"))
    private void sky_enableAlpha() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.enableAlpha();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.enableAlpha();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableAlpha()V"))
    private void sky_disableAlpha() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.disableAlpha();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.disableAlpha();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V"))
    private void sky_enableBlend() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.enableBlend();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.enableBlend();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;disableBlend()V"))
    private void sky_disableBlend() {
        if (skipState()) {
            return;
        }
        if (!SkyBreakdown.enabled()) {
            GlStateManager.disableBlend();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.disableBlend();
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;shadeModel(I)V"))
    private void sky_shadeModel(int mode) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.shadeModel(mode);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.shadeModel(mode);
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V"))
    private void sky_depthMask(boolean mask) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.depthMask(mask);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.depthMask(mask);
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V"))
    private void sky_pushMatrix() {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.pushMatrix();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.pushMatrix();
        SkyBreakdown.record(SkyBreakdown.MATRIX, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;popMatrix()V"))
    private void sky_popMatrix() {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.popMatrix();
            return;
        }
        long start = System.nanoTime();
        GlStateManager.popMatrix();
        SkyBreakdown.record(SkyBreakdown.MATRIX, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
    private void sky_rotate(float angle, float x, float y, float z) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.rotate(angle, x, y, z);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.rotate(angle, x, y, z);
        SkyBreakdown.record(SkyBreakdown.MATRIX, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    private void sky_translate(float x, float y, float z) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.translate(x, y, z);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.translate(x, y, z);
        SkyBreakdown.record(SkyBreakdown.MATRIX, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    private void sky_scale(float x, float y, float z) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.scale(x, y, z);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.scale(x, y, z);
        SkyBreakdown.record(SkyBreakdown.MATRIX, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFF)V"))
    private void sky_color3(float r, float g, float b) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.color(r, g, b);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.color(r, g, b);
        SkyBreakdown.record(SkyBreakdown.COLOR, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"))
    private void sky_color4(float r, float g, float b, float a) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.color(r, g, b, a);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.color(r, g, b, a);
        SkyBreakdown.record(SkyBreakdown.COLOR, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;tryBlendFuncSeparate(IIII)V"))
    private void sky_blendFunc(int a, int b, int c, int d) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.tryBlendFuncSeparate(a, b, c, d);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.tryBlendFuncSeparate(a, b, c, d);
        SkyBreakdown.record(SkyBreakdown.STATE, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V"))
    private void sky_callList(int list) {
        if (!SkyBreakdown.enabled()) {
            GlStateManager.callList(list);
            return;
        }
        long start = System.nanoTime();
        GlStateManager.callList(list);
        SkyBreakdown.record(SkyBreakdown.LIST, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;draw()V"))
    private void sky_draw(Tessellator tessellator) {
        if (!SkyBreakdown.enabled()) {
            tessellator.draw();
            return;
        }
        long start = System.nanoTime();
        tessellator.draw();
        SkyBreakdown.record(SkyBreakdown.DRAW, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private void sky_bind(TextureManager manager, ResourceLocation location) {
        if (!SkyBreakdown.enabled()) {
            manager.bindTexture(location);
            return;
        }
        long start = System.nanoTime();
        manager.bindTexture(location);
        SkyBreakdown.record(SkyBreakdown.BIND, System.nanoTime() - start);
        
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;getSkyColor(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/Vec3;"))
    private Vec3 sky_skyColor(WorldClient world, Entity entity, float partialTicks) {
        if (!SkyBreakdown.enabled()) {
            return world.getSkyColor(entity, partialTicks);
        }
        long start = System.nanoTime();
        Vec3 value = world.getSkyColor(entity, partialTicks);
        SkyBreakdown.record(SkyBreakdown.QUERY, System.nanoTime() - start);
        return value;
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;getStarBrightness(F)F"))
    private float sky_starBrightness(WorldClient world, float partialTicks) {
        if (!SkyBreakdown.enabled()) {
            return world.getStarBrightness(partialTicks);
        }
        long start = System.nanoTime();
        float value = world.getStarBrightness(partialTicks);
        SkyBreakdown.record(SkyBreakdown.QUERY, System.nanoTime() - start);
        return value;
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;getRainStrength(F)F"))
    private float sky_rainStrength(WorldClient world, float partialTicks) {
        if (!SkyBreakdown.enabled()) {
            return world.getRainStrength(partialTicks);
        }
        long start = System.nanoTime();
        float value = world.getRainStrength(partialTicks);
        SkyBreakdown.record(SkyBreakdown.QUERY, System.nanoTime() - start);
        return value;
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldProvider;calcSunriseSunsetColors(FF)[F"))
    private float[] sky_sunrise(WorldProvider provider, float angle, float partialTicks) {
        if (!SkyBreakdown.enabled()) {
            return provider.calcSunriseSunsetColors(angle, partialTicks);
        }
        long start = System.nanoTime();
        float[] value = provider.calcSunriseSunsetColors(angle, partialTicks);
        SkyBreakdown.record(SkyBreakdown.QUERY, System.nanoTime() - start);
        return value;
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;getCelestialAngle(F)F"))
    private float sky_celestial(WorldClient world, float partialTicks) {
        if (!SkyBreakdown.enabled()) {
            return world.getCelestialAngle(partialTicks);
        }
        long start = System.nanoTime();
        float value = world.getCelestialAngle(partialTicks);
        SkyBreakdown.record(SkyBreakdown.QUERY, System.nanoTime() - start);
        return value;
    }

    @Redirect(method = "renderSky(FI)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;getCelestialAngleRadians(F)F"))
    private float sky_celestialRad(WorldClient world, float partialTicks) {
        if (!SkyBreakdown.enabled()) {
            return world.getCelestialAngleRadians(partialTicks);
        }
        long start = System.nanoTime();
        float value = world.getCelestialAngleRadians(partialTicks);
        SkyBreakdown.record(SkyBreakdown.QUERY, System.nanoTime() - start);
        return value;
    }
}
