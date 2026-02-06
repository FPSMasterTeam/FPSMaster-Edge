package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.*;

public class DragonWings extends Module {
    public BooleanSetting colored = new BooleanSetting("Colored", false);
    public BooleanSetting chroma = new BooleanSetting("Chroma", false);
    public NumberSetting scale = new NumberSetting("Scale", 100.0, 0.0, 100.0, 1.0);
    public ColorSetting color = new ColorSetting("Color", new Color(0, 0, 0));
    private RenderWings renderWings;

    public DragonWings() {
        super("DragonWings", Category.RENDER);
        this.addSettings(scale, color, colored, chroma);
    }

    @Override
    public void onEnable() {
        if (renderWings == null) {
            renderWings = new RenderWings();
        }
        EventDispatcher.registerListener(renderWings);
    }

    @Override
    public void onDisable() {
        EventDispatcher.unregisterListener(renderWings);
    }

    public float[] getColors() {
        if (!colored.getValue()) {
            return new float[]{1F, 1F, 1F};
        }

        Color color1 = chroma.getValue() ? Color.getHSBColor((System.currentTimeMillis() % 1000) / 1000F, 0.8F, 1F) : color.getColor();
        return new float[]{color1.getRed() / 255F, color1.getGreen() / 255F, color1.getBlue() / 255F};
    }

    /**
     * @author: https://github.com/Canelex/DragonWingsMod
     **/
    public static class RenderWings extends ModelBase {
        private final Minecraft mc;
        private final ResourceLocation location;
        private final ModelRenderer wing;
        private final ModelRenderer wingTip;
        private final boolean playerUsesFullHeight;
        private final Module wingsModule;

        public RenderWings() {
            this.mc = Minecraft.getMinecraft();
            this.location = new ResourceLocation("client/wings/wings.png");
            this.playerUsesFullHeight = false; //Loader.isModLoaded("animations");
            this.wingsModule = FPSMaster.moduleManager.getModule(DragonWings.class);

            // Set texture offsets.
            setTextureOffset("wing.bone", 0, 0);
            setTextureOffset("wing.skin", -10, 8);
            setTextureOffset("wingtip.bone", 0, 5);
            setTextureOffset("wingtip.skin", -10, 18);

            // Create wing model renderer.
            wing = new ModelRenderer(this, "wing");
            wing.setTextureSize(30, 30); // 300px / 10px
            wing.setRotationPoint(-2F, 0, 0);
            wing.addBox("bone", -10.0F, -1.0F, -1.0F, 10, 2, 2);
            wing.addBox("skin", -10.0F, 0.0F, 0.5F, 10, 0, 10);

            // Create wing tip model renderer.
            wingTip = new ModelRenderer(this, "wingtip");
            wingTip.setTextureSize(30, 30); // 300px / 10px
            wingTip.setRotationPoint(-10.0F, 0.0F, 0.0F);
            wingTip.addBox("bone", -10.0F, -0.5F, -0.5F, 10, 1, 1);
            wingTip.addBox("skin", -10.0F, 0.0F, 0.5F, 10, 0, 10);
            wing.addChild(wingTip); // Make the wingtip rotate around the wing.

        }

        @Subscribe
        public void onRenderPlayer(EventRender3D event) {
            EntityPlayer player;

            player = Minecraft.getMinecraft().thePlayer;
            if (player == null)
                return;
            if (!player.isInvisible() && mc.gameSettings.thirdPersonView != 0) // Should render wings onto this player?
            {
                renderWings(player, event.partialTicks);
            }
        }

        private void renderWings(EntityPlayer player, float partialTicks) {
            double scale = ((DragonWings) wingsModule).scale.getValue().doubleValue() / 100D;
            double rotate = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);

            GL11.glPushMatrix();
            GL11.glScaled(-scale, -scale, scale);
            GL11.glRotated(180 + rotate, 0, 1, 0); // Rotate the wings to be with the player.
            GL11.glTranslated(0, -(playerUsesFullHeight ? 1.45 : 1.25) / scale, 0); // Move wings correct amount up.
            GL11.glTranslated(0, 0, 0.2 / scale);

            if (player.isSneaking()) {
                GL11.glTranslated(0D, 0.125D / scale, 0D);
            }

            float[] colors = ((DragonWings) wingsModule).getColors();
            GL11.glColor3f(colors[0], colors[1], colors[2]);
            mc.getTextureManager().bindTexture(location);

            for (int j = 0; j < 2; ++j) {
                GL11.glEnable(GL11.GL_CULL_FACE);
                float f11 = (System.currentTimeMillis() % 1000) / 1000F * (float) Math.PI * 2.0F;
                this.wing.rotateAngleX = (float) Math.toRadians(-80F) - (float) Math.cos(f11) * 0.2F;
                this.wing.rotateAngleY = (float) Math.toRadians(20F) + (float) Math.sin(f11) * 0.4F;
                this.wing.rotateAngleZ = (float) Math.toRadians(20F);
                this.wingTip.rotateAngleZ = -((float) (Math.sin(f11 + 2.0F) + 0.5D)) * 0.75F;
                this.wing.render(0.0625F);
                GL11.glScalef(-1.0F, 1.0F, 1.0F);

                if (j == 0) {
                    GL11.glCullFace(1028);
                }
            }
            GL11.glCullFace(1029);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor3f(255F, 255F, 255F);
            GL11.glPopMatrix();
        }

        private float interpolate(float yaw1, float yaw2, float percent) {
            float f = (yaw1 + (yaw2 - yaw1) * percent) % 360;

            if (f < 0) {
                f += 360;
            }

            return f;
        }
    }
}
