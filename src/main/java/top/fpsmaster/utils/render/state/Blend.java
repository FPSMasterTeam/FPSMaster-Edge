package top.fpsmaster.utils.render.state;

import net.minecraft.client.renderer.GlStateManager;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class Blend {
    public static void begin() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void end() {
        GlStateManager.disableBlend();
    }
}

