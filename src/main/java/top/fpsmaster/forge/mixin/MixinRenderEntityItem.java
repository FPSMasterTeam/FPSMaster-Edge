package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.render.ItemPhysics;

import java.util.Random;

@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem extends MixinRender{

    @Shadow
    protected abstract int func_177078_a(ItemStack stack);

    @Unique
    Random arch$random = new Random();

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V",at = @At("HEAD"), cancellable = true)
    public void preRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci){
        ItemStack itemstack = entity.getEntityItem();
        if (ItemPhysics.using) {
            double rotation1 = 5.5D;
            int i;
            if (itemstack != null && itemstack.getItem() != null) {
                i = Item.getIdFromItem(itemstack.getItem()) + itemstack.getMetadata();
            } else {
                i = 187;
            }
            arch$random.setSeed(i);
            this.bindTexture(TextureMap.locationBlocksTexture);
            Minecraft.getMinecraft().getRenderManager().renderEngine.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
            GlStateManager.enableRescaleNormal();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.pushMatrix();
            IBakedModel ibakedmodel = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(itemstack);
            boolean flag1 = ibakedmodel.isGui3d();
            boolean is3D = ibakedmodel.isGui3d();
            int j = this.func_177078_a(itemstack);
            GlStateManager.translate((float) x, (float) y, (float) z);
            if (ibakedmodel.isGui3d()) {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
            }

            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(entity.rotationYaw, 0.0F, 0.0F, 1.0F);
            if (is3D) {
                GlStateManager.translate(0.0D, 0.0D, -0.08D);
            } else {
                GlStateManager.translate(0.0D, 0.0D, -0.04D);
            }

            if (is3D || Minecraft.getMinecraft().getRenderManager().options != null) {
                double rotation;
                if (is3D) {
                    if (!entity.onGround) {
                        rotation = rotation1 * 2.0D;
                        rotation /= 10;
                        entity.rotationPitch = (float) ((double) entity.rotationPitch + rotation);
                    }
                } else if (!Double.isNaN(entity.posX) && !Double.isNaN(entity.posY) && !Double.isNaN(entity.posZ) && entity.worldObj != null) {
                    if (entity.onGround) {
                        entity.rotationPitch = 0.0F;
                    } else {
                        rotation = rotation1 * 2.0D;
                        rotation /= 10;
                        entity.rotationPitch = (float) ((double) entity.rotationPitch + rotation);
                    }
                }

                GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            for (int k = 0; k < j; ++k) {
                GlStateManager.pushMatrix();
                if (flag1) {
                    if (k > 0) {
                        float f7 = (arch$random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        float f9 = (arch$random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        float f6 = (arch$random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        GlStateManager.translate(f7, f9, f6);
                    }

                    Minecraft.getMinecraft().getRenderItem().renderItem(itemstack, ibakedmodel);
                    GlStateManager.popMatrix();
                } else {
                    Minecraft.getMinecraft().getRenderItem().renderItem(itemstack, ibakedmodel);
                    GlStateManager.popMatrix();
                    GlStateManager.translate(0.0F, 0.0F, 0.05375F);
                }
            }

            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            this.bindTexture(TextureMap.locationBlocksTexture);
            Minecraft.getMinecraft().getRenderManager().renderEngine.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
            ci.cancel();
        }
    }


}
