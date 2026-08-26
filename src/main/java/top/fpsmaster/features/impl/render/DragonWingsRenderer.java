package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.cosmetic.CosmeticManager;

public final class DragonWingsRenderer extends ModelBase {
    private static final ResourceLocation BUILTIN_TEXTURE = new ResourceLocation("client/wings/wings.png");
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ModelRenderer wing;
    private final ModelRenderer wingTip;

    public DragonWingsRenderer() {
        setTextureOffset("wing.bone", 0, 0);
        setTextureOffset("wing.skin", -10, 8);
        setTextureOffset("wingtip.bone", 0, 5);
        setTextureOffset("wingtip.skin", -10, 18);

        wing = new ModelRenderer(this, "wing");
        wing.setTextureSize(30, 30);
        wing.setRotationPoint(-2f, 0f, 0f);
        wing.addBox("bone", -10f, -1f, -1f, 10, 2, 2);
        wing.addBox("skin", -10f, 0f, 0.5f, 10, 0, 10);

        wingTip = new ModelRenderer(this, "wingtip");
        wingTip.setTextureSize(30, 30);
        wingTip.setRotationPoint(-10f, 0f, 0f);
        wingTip.addBox("bone", -10f, -0.5f, -0.5f, 10, 1, 1);
        wingTip.addBox("skin", -10f, 0f, 0.5f, 10, 0, 10);
        wing.addChild(wingTip);
    }

    /**
     * Draws the wings for whichever player the surrounding layer is rendering. Positioning and
     * scale belong to the caller, which owns the player's transform.
     */
    public void renderLayer(ResourceLocation texture) {
        renderGeometry(texture);
    }

    public void renderPreview(float x, float y, float size, float yaw) {
        CosmeticManager cosmetics = CosmeticManager.getInstance();
        renderPreview(x, y, size, yaw, cosmetics.wingTexture(), cosmetics.wingScale());
    }

    public void renderPreview(float x, float y, float size, float yaw, ResourceLocation texture, float wingScale) {
        double scale = Math.max(0.01d, wingScale);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 50f);
        GL11.glScalef(-size, size, size);
        GL11.glRotatef(yaw, 0f, 1f, 0f);
        GL11.glScaled(scale, scale, scale);
        GL11.glTranslated(0d, -1.45d / scale, 0.2d / scale);
        renderGeometry(texture);
        GL11.glPopMatrix();
    }

    private void renderGeometry(ResourceLocation texture) {
        mc.getTextureManager().bindTexture(texture == null ? BUILTIN_TEXTURE : texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        for (int side = 0; side < 2; side++) {
            GL11.glEnable(GL11.GL_CULL_FACE);
            float phase = (System.currentTimeMillis() % 1000L) / 1000f * (float) Math.PI * 2f;
            wing.rotateAngleX = (float) Math.toRadians(-80f) - (float) Math.cos(phase) * 0.2f;
            wing.rotateAngleY = (float) Math.toRadians(20f) + (float) Math.sin(phase) * 0.4f;
            wing.rotateAngleZ = (float) Math.toRadians(20f);
            wingTip.rotateAngleZ = -(float) (Math.sin(phase + 2f) + 0.5d) * 0.75f;
            wing.render(0.0625f);
            GL11.glScalef(-1f, 1f, 1f);
            if (side == 0) GL11.glCullFace(GL11.GL_FRONT);
        }
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
