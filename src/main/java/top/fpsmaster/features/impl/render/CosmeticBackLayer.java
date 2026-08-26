package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.cosmetic.CosmeticManager;

/**
 * Draws the back cosmetic for every player the client can see.
 *
 * <p>This is a {@link LayerRenderer} rather than a world-render hook so that a back piece follows
 * the same body transform, lighting and sneak pose as the rest of the model, and so that it is
 * drawn for other players instead of only the local one. Which cosmetic each player gets is looked
 * up by UUID, so a remote loadout that has not resolved yet simply draws nothing.
 */
public final class CosmeticBackLayer implements LayerRenderer<AbstractClientPlayer> {
    /**
     * Distance in blocks from the feet up to where the wings anchor, and the layer origin's own
     * height. Their difference is the offset applied below; the layer starts at the latter because
     * the entity renderer has already translated there.
     */
    private static final float ANCHOR_HEIGHT = 1.25F;
    private static final float LAYER_ORIGIN_HEIGHT = 1.5078125F;
    private static final float BACK_OFFSET = 0.2F;
    private static final float SNEAK_DROP = 0.125F;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final DragonWingsRenderer wings;

    public CosmeticBackLayer(DragonWingsRenderer wings) {
        this.wings = wings;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (player.isInvisible() || !player.hasPlayerInfo()) {
            return;
        }
        boolean local = player == mc.thePlayer;
        // The local player's own back piece would sit inside the camera in first person, so it is
        // drawn only once the camera has pulled out.
        if (local && mc.gameSettings.thirdPersonView == 0) {
            return;
        }

        CosmeticManager.BackPiece piece = CosmeticManager.getInstance()
                .backPieceFor(player.getUniqueID(), local);
        if (piece == null) {
            return;
        }

        GL11.glPushMatrix();
        if (piece.elytra) {
            GL11.glScalef(piece.scale, piece.scale, piece.scale);
            CosmeticManager.getInstance().elytraRenderer().render(piece.texture, player.isSneaking());
        } else {
            renderWings(piece, player.isSneaking());
        }
        GL11.glPopMatrix();
    }

    /**
     * The wing model is authored around its own anchor point, so the translation is expressed in
     * unscaled blocks and divided back out: growing the wings must not also push them off the back.
     */
    private void renderWings(CosmeticManager.BackPiece piece, boolean sneaking) {
        float scale = piece.scale;
        GL11.glScalef(scale, scale, scale);
        float drop = LAYER_ORIGIN_HEIGHT - ANCHOR_HEIGHT + (sneaking ? SNEAK_DROP : 0f);
        GL11.glTranslatef(0f, drop / scale, BACK_OFFSET / scale);
        wings.renderLayer(piece.texture);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
