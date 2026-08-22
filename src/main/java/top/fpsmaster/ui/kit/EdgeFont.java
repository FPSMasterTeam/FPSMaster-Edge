package top.fpsmaster.ui.kit;

import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.prism.canvas.FontHandle;

final class EdgeFont implements FontHandle {
    final UFontRenderer renderer;
    private final int size;

    EdgeFont(UFontRenderer renderer, int size) {
        this.renderer = renderer;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public float measure(String text) {
        return renderer.getStringWidth(text == null ? "" : text);
    }

    public float lineHeight() {
        return size * 0.5f;
    }
}
