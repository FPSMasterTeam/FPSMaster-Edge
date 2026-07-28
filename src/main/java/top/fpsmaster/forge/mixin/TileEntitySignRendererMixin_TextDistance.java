package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.IChatComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.benchmark.Experiments;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Stops drawing sign text once it is too far away to read.
 *
 * <p>A sign is the only block in the game that pays a per-frame text cost: every visible one splits
 * its four lines into components, measures each for centring and pushes them through the font
 * renderer, every frame, at any distance inside render distance. The geometry of the sign itself is
 * a handful of quads; the text is the whole cost.
 *
 * <p>The cutoff is the one OptiFine uses, derived rather than picked: {@code 1.5 * windowHeight /
 * fov}, floored at 16 blocks. It is an angular threshold in disguise — it asks how far away a glyph
 * shrinks below roughly one pixel for this window and this field of view, so a taller window or a
 * narrower field of view pushes it further out. At 1280x720 and the default field of view it lands
 * at 16 blocks, where a sign character is about two pixels tall.
 *
 * <p>Redirecting the field read rather than the draw call is deliberate: an empty array makes the
 * loop not run at all, which skips the component splitting and the width measurement too. Skipping
 * only the draw would leave the layout work, which is the larger half.
 *
 * <p>A sign being edited always draws, whatever the distance, and so does every sign while the sign
 * editing screen is open — the player is looking at text they are typing.
 */
@Mixin(TileEntitySignRenderer.class)
public class TileEntitySignRendererMixin_TextDistance {

    @Unique
    private static final IChatComponent[] FPSMASTER_NO_TEXT = new IChatComponent[0];

    /**
     * Floor on the cutoff. Below this the saving is not worth the risk of a player noticing text
     * disappear on a sign they are walking up to.
     */
    @Unique
    private static final double FPSMASTER_MIN_TEXT_DISTANCE = 16.0d;

    /**
     * Decided once per sign at the top of the render, not per field read: vanilla reads
     * {@code signText} several times per sign and the decision must not be recomputed, nor the
     * counter incremented, once per read.
     */
    @Unique
    private boolean fpsmaster$cullText;

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySign;DDDFI)V",
            at = @At("HEAD"))
    private void fpsmaster$decideTextVisibility(TileEntitySign sign, double x, double y, double z,
                                                float partialTicks, int destroyStage, CallbackInfo ci) {
        fpsmaster$cullText = Experiments.active(Experiments.NO_SIGN_TEXT) || fpsmaster$isTooFar(sign);
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.signsRendered++;
            if (fpsmaster$cullText) {
                BenchCounters.signTextCulled++;
            }
        }
    }

    @Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySign;DDDFI)V",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/tileentity/TileEntitySign;signText:"
                            + "[Lnet/minecraft/util/IChatComponent;"))
    private IChatComponent[] fpsmaster$signText(TileEntitySign sign) {
        return fpsmaster$cullText ? FPSMASTER_NO_TEXT : sign.signText;
    }

    @Unique
    private boolean fpsmaster$isTooFar(TileEntitySign sign) {
        if (!Performance.using || !Performance.signTextCulling.getValue()) {
            return false;
        }
        if (sign.lineBeingEdited >= 0) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen instanceof GuiEditSign) {
            return false;
        }
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) {
            return false;
        }
        double fov = mc.gameSettings.fovSetting;
        if (fov < 1.0d) {
            fov = 1.0d;
        } else if (fov > 120.0d) {
            fov = 120.0d;
        }
        double limit = Math.max(1.5d * mc.displayHeight / fov, FPSMASTER_MIN_TEXT_DISTANCE);
        return sign.getDistanceSq(viewer.posX, viewer.posY, viewer.posZ) > limit * limit;
    }
}
