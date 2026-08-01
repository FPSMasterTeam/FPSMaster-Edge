package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.mixin.accessor.RenderAccessor;

/**
 * Hides item frames, or just the maps in them.
 *
 * <p>Two separate switches because they are not the same cost. A framed item is one small model; a
 * framed map is a full map render, its texture and every marker drawn on it, and a wall of them is
 * most of what item frames ever cost. Someone who wants the map wall to stop costing anything does
 * not necessarily want the frames to vanish off the wall.
 *
 * <p>The name label survives, for the same reason as on armour stands: a named item frame is a
 * label, and vanilla only draws that label as the last act of {@code doRender}.
 */
@Mixin(RenderItemFrame.class)
public class RenderItemFrameMixin_Hide {

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItemFrame;DDDFF)V",
            at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideFrame(EntityItemFrame entity, double x, double y, double z,
                                     float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!Performance.using || !Performance.hideItemFrames.getValue()) {
            return;
        }
        // The same offsets vanilla uses, so a hidden frame's label does not move.
        ((RenderAccessor) this).fpsmaster$invokeRenderName(entity,
                x + entity.facingDirection.getFrontOffsetX() * 0.3F,
                y - 0.25D,
                z + entity.facingDirection.getFrontOffsetZ() * 0.3F);
        ci.cancel();
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideMap(EntityItemFrame itemFrame, CallbackInfo ci) {
        if (!Performance.using || !Performance.hideMapsInItemFrames.getValue()) {
            return;
        }
        ItemStack displayed = itemFrame.getDisplayedItem();
        if (displayed != null && displayed.getItem() instanceof ItemMap) {
            ci.cancel();
        }
    }
}
