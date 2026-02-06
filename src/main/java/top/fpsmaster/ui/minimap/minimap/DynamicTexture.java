package top.fpsmaster.ui.minimap.minimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class DynamicTexture extends SimpleTexture
{
    public BufferedImage bufferedimage;
    public ByteBuffer buffer512;
    public ByteBuffer buffer256;
    public ByteBuffer buffer128;
    public ByteBuffer[] buffers;
    
    public ByteBuffer getBuffer(final int size) {
        switch (size) {
            case 128: {
                return this.buffer128;
            }
            case 256: {
                return this.buffer256;
            }
            default: {
                return this.buffer512;
            }
        }
    }
    
    public DynamicTexture(final ResourceLocation location) {
        super(location);
        this.buffer512 = BufferUtils.createByteBuffer(786432);
        this.buffer256 = BufferUtils.createByteBuffer(196608);
        this.buffer128 = BufferUtils.createByteBuffer(49152);
        this.buffers = new ByteBuffer[] { this.buffer128, this.buffer256, this.buffer256, this.buffer512 };
        this.loadTexture(Minecraft.getMinecraft().getResourceManager());
    }
    
    public void loadTexture(final IResourceManager par1ResourceManager) {
        this.bufferedimage = new BufferedImage(512, 512, 5);
        TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), this.bufferedimage, false, false);
    }
}



