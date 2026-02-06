package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class Scissor {
    public static void apply(float x, float y, float width, float height, int scaleFactor) {
        if (Minecraft.getMinecraft().currentScreen == null) {
            return;
        }
        width *= 1f / scaleFactor * 2;
        height *= 1f / scaleFactor * 2;
        y *= 1f / scaleFactor * 2;
        x *= 1f / scaleFactor * 2;
        GL11.glScissor(
                (int) (x * Minecraft.getMinecraft().displayWidth / Minecraft.getMinecraft().currentScreen.width),
                (int) (Minecraft.getMinecraft().displayHeight - (y + height) * Minecraft.getMinecraft().displayHeight / Minecraft.getMinecraft().currentScreen.height),
                (int) (width * Minecraft.getMinecraft().displayWidth / Minecraft.getMinecraft().currentScreen.width),
                (int) (height * Minecraft.getMinecraft().displayHeight / Minecraft.getMinecraft().currentScreen.height)
        );
    }
}
