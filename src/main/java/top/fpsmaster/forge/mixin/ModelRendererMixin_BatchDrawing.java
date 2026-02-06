package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;

@Mixin(ModelRenderer.class)
public class ModelRendererMixin_BatchDrawing {
    @Shadow
    private boolean compiled;

    @Unique
    private boolean patcher$compiledState;

    @Inject(method = "render", at = @At("HEAD"))
    private void patcher$resetCompiled(float j, CallbackInfo ci) {
        if (patcher$compiledState != Performance.batchModelRendering.getValue()) {
            this.compiled = false;
        }
    }

    @Inject(method = "compileDisplayList", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/Tessellator;getWorldRenderer()Lnet/minecraft/client/renderer/WorldRenderer;"))
    private void patcher$beginRendering(CallbackInfo ci) {
        this.patcher$compiledState = Performance.batchModelRendering.getValue();
        if (Performance.batchModelRendering.getValue()) {
            Tessellator.getInstance().getWorldRenderer().begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
        }
    }

    @Inject(method = "compileDisplayList", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEndList()V", remap = false))
    private void patcher$draw(CallbackInfo ci) {
        if (Performance.batchModelRendering.getValue()) {
            Tessellator.getInstance().draw();
        }
    }
}
