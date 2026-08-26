package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Elytra-shaped back cosmetic.
 *
 * <p>1.8.9 has no elytra of its own, so this is purely a cosmetic silhouette: the geometry matches
 * the shape players recognise and the pose is picked from stance alone, since there is no gliding
 * state to read. The proportions and texture layout follow the later vanilla model so that artwork
 * authored against a standard elytra template maps correctly.
 */
public final class ElytraRenderer extends ModelBase {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ModelRenderer leftWing;
    private final ModelRenderer rightWing;

    public ElytraRenderer() {
        textureWidth = 64;
        textureHeight = 32;

        leftWing = new ModelRenderer(this, 22, 0);
        leftWing.addBox(-10.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);

        rightWing = new ModelRenderer(this, 22, 0);
        rightWing.mirror = true;
        rightWing.addBox(0.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
    }

    /**
     * Draws the wings in player-model space, where the caller has already applied the body rotation
     * and the cosmetic scale.
     */
    public void render(ResourceLocation texture, boolean sneaking) {
        if (texture == null) {
            return;
        }
        mc.getTextureManager().bindTexture(texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glPushMatrix();
        GL11.glTranslatef(0f, 0f, 0.125f);
        pose(sneaking);
        leftWing.render(0.0625F);
        rightWing.render(0.0625F);
        GL11.glPopMatrix();
    }

    /**
     * Spread and droop of the two halves. Sneaking folds them in and tips them forward so they do
     * not clip through the crouched body.
     */
    private void pose(boolean sneaking) {
        float spread = sneaking ? 0.6981317F : 0.2617994F;
        float droop = sneaking ? -0.7853982F : -0.2617994F;
        float pitch = sneaking ? -0.3F : 0.0F;
        float lift = sneaking ? 0.08726646F : 0.0F;

        leftWing.rotateAngleX = pitch;
        leftWing.rotateAngleY = spread;
        leftWing.rotateAngleZ = droop;

        rightWing.rotateAngleX = pitch;
        rightWing.rotateAngleY = -spread;
        rightWing.rotateAngleZ = -droop;

        leftWing.rotationPointX = 5.0F;
        leftWing.rotationPointY = lift;
        rightWing.rotationPointX = -5.0F;
        rightWing.rotationPointY = lift;
    }
}
