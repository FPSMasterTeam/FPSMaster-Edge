package top.fpsmaster.ui.kit;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.effects.Blur;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.canvas.ImageHandle;
import top.fpsmaster.prism.host.UiHost;
import top.fpsmaster.prism.input.FrameInput;
import top.fpsmaster.prism.input.Input;

import java.awt.Color;

final class EdgeHost implements UiHost {
    private final ScaledGuiScreen screen;
    private final EdgeCanvas canvas = new EdgeCanvas();
    private final EdgeInput input;
    private final float width;
    private final float height;

    EdgeHost(ScaledGuiScreen screen, FrameInput fallback, float width, float height) {
        this.screen = screen;
        this.input = new EdgeInput(screen, fallback);
        this.width = width;
        this.height = height;
    }

    public Canvas canvas() {
        return canvas;
    }

    public Input input() {
        return input;
    }

    public FontHandle font(int size) {
        return new EdgeFont(FPSMaster.fontManager.getFont(size), size);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public long nowNanos() {
        return System.nanoTime();
    }

    public boolean blurEnabled() {
        return ClientSettings.blur.getValue();
    }

    public boolean animationsEnabled() {
        return ClientSettings.interfaceAnimations.getValue();
    }

    public void blurBehind(float x, float y, float w, float h, float radius) {
        Blur.area(x, y, w, h, Math.round(radius), Color.WHITE, 3, 3);
    }

    public ImageHandle image(String id) {
        return image(id, 11f);
    }

    public ImageHandle image(String id, float drawSize) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if ("brand".equals(id)) {
            return new EdgeImage(new ResourceLocation("textures/fpsmaster-icon.png"), 64, 64);
        }
        int px = Icons.pixelBucket(drawSize);
        ResourceLocation location = id.startsWith("modules/")
                ? new ResourceLocation("textures/gui/icons/" + px + "/" + id + ".png")
                : Icons.location(id, drawSize);
        return new EdgeImage(location, px, px);
    }

    ScaledGuiScreen screen() {
        return screen;
    }
}
