package top.fpsmaster.ui.kit;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.prism.canvas.ImageHandle;

final class EdgeImage implements ImageHandle {
    final ResourceLocation location;
    private final int width;
    private final int height;

    EdgeImage(ResourceLocation location, int width, int height) {
        this.location = location;
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
