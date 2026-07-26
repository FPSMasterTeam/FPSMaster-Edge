package top.fpsmaster.forge.mixin;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Skips submitting particles that fall outside the view frustum.
 *
 * <p>Vanilla walks every particle in every layer and builds four vertices for each, whether or not
 * it is on screen. The existing ParticlesLimit setting does not help here: it caps
 * {@code particleEmitters}, which is a different list.
 *
 * <p>The frustum is built once per {@code renderParticles} call rather than per particle. Per
 * particle the test is six plane comparisons against a small box around its position — cheaper than
 * the vertex work it avoids, but only for particles that are actually off screen, so this is a win
 * on wide particle fields rather than on a screen already full of them.
 */
@Mixin(EffectRenderer.class)
public class EffectRendererMixin_ParticleCulling {

    /**
     * Half-extent of the box tested against the frustum. A particle's own bounding box can be
     * degenerate, while its rendered quad extends around its position, so the test uses a fixed
     * margin instead. Generous on purpose: culling something still visible is a rendering bug,
     * while failing to cull something invisible only costs a few vertices.
     */
    @Unique
    private static final double PARTICLE_CULL_MARGIN = 0.5d;

    @Unique
    private Frustum fpsmaster$frustum;

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void fpsmaster$endParticleSection(Entity entityIn, float partialTicks, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_PARTICLES);
        }
    }

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void fpsmaster$captureFrustum(Entity entityIn, float partialTicks, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_PARTICLES);
        }
        if (!Performance.using || !Performance.particleCulling.getValue()) {
            fpsmaster$frustum = null;
            return;
        }
        // Built from the live projection/modelview, which at this point in renderWorldPass is the
        // camera transform.
        //
        // The camera position is interpolated here rather than read from EntityFX.interpPos*:
        // vanilla assigns those inside renderParticles, after this HEAD injection, so reading them
        // would position the frustum with the previous frame's camera.
        fpsmaster$frustum = new Frustum(ClippingHelperImpl.getInstance());
        fpsmaster$frustum.setPosition(
                entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks,
                entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks,
                entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks);
    }

    @Redirect(
            method = "renderParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/EntityFX;renderParticle"
                            + "(Lnet/minecraft/client/renderer/WorldRenderer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void fpsmaster$cullParticle(EntityFX particle, WorldRenderer worldRenderer, Entity entity,
                                        float partialTicks, float rotationX, float rotationXZ,
                                        float rotationZ, float rotationYZ, float rotationXY) {
        if (fpsmaster$frustum != null && !fpsmaster$frustum.isBoxInFrustum(
                particle.posX - PARTICLE_CULL_MARGIN,
                particle.posY - PARTICLE_CULL_MARGIN,
                particle.posZ - PARTICLE_CULL_MARGIN,
                particle.posX + PARTICLE_CULL_MARGIN,
                particle.posY + PARTICLE_CULL_MARGIN,
                particle.posZ + PARTICLE_CULL_MARGIN)) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.particlesCulled++;
            }
            return;
        }
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.particlesRendered++;
        }
        particle.renderParticle(worldRenderer, entity, partialTicks, rotationX, rotationXZ,
                rotationZ, rotationYZ, rotationXY);
    }
}
