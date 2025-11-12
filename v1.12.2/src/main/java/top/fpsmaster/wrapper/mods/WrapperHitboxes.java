package top.fpsmaster.wrapper.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.forge.api.IRenderManager;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.wrapper.TimerProvider;
import top.fpsmaster.wrapper.WorldClientProvider;
import top.fpsmaster.wrapper.util.WrapperAxisAlignedBB;

import java.util.stream.Collectors;

public class WrapperHitboxes {
    public static void render(EventRender3D event, ColorSetting color) {
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        for (Entity entity : ProviderManager.worldClientProvider.getWorld().loadedEntityList.stream().filter(e -> e != ProviderManager.mcProvider.getPlayer() && !e.isInvisible()).collect(Collectors.toList())) {
            double d_0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) ProviderManager.timerProvider.getRenderPartialTicks();
            double d_1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) ProviderManager.timerProvider.getRenderPartialTicks();
            double d_2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) ProviderManager.timerProvider.getRenderPartialTicks();
            double x = d_0 - ((IRenderManager) Minecraft.getMinecraft().getRenderManager()).renderPosX();
            double y = d_1 - ((IRenderManager) Minecraft.getMinecraft().getRenderManager()).renderPosY();
            double z = d_2 - ((IRenderManager) Minecraft.getMinecraft().getRenderManager()).renderPosZ();
            WrapperAxisAlignedBB boundingBox = new WrapperAxisAlignedBB(entity.getEntityBoundingBox());
            RenderGlobal.drawBoundingBox(boundingBox.minX() - entity.posX + x, boundingBox.minY() - entity.posY + y, boundingBox.minZ() - entity.posZ + z, boundingBox.maxX() - entity.posX + x, boundingBox.maxY() - entity.posY + y, boundingBox.maxZ() - entity.posZ + z, color.getColor().getRed()/255f, color.getColor().getGreen()/255f, color.getColor().getBlue()/255f, color.getColor().getAlpha()/255f);
        }
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
    }
}
