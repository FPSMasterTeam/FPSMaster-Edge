package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.AbstractResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Mixin(AbstractResourcePack.class)
public abstract class AbstractResourcePackMixin_DownscaleImages {

    @Shadow
    protected abstract InputStream getInputStreamByName(String name) throws IOException;

    @Inject(method = "getPackImage", at = @At("HEAD"), cancellable = true)
    private void patcher$downscalePackImage(CallbackInfoReturnable<BufferedImage> cir) throws IOException {
        BufferedImage image = TextureUtil.readBufferedImage(this.getInputStreamByName("pack.png"));
        if (image == null) {
            cir.setReturnValue(null);
            return;
        }

        // 如果图片尺寸已经小于等于64x64，直接返回原图
        if (image.getWidth() <= 64 && image.getHeight() <= 64) {
            cir.setReturnValue(image);
            return;
        }

        // 正常缩放其他图片
        BufferedImage downscaledIcon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = downscaledIcon.createGraphics();
        try {
            // 设置更好的渲染质量
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, 64, 64, null);
        } finally {
            graphics.dispose();
        }
        cir.setReturnValue(downscaledIcon);
    }
}



