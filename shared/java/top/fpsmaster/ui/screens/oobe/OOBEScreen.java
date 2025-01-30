package top.fpsmaster.ui.screens.oobe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.ui.screens.oobe.impls.Login;
import top.fpsmaster.utils.math.animation.Animation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

public class OOBEScreen extends GuiScreen {

    private static final ArrayList<Scene> scenes = new ArrayList<>();
    private static Scene currentScene = null;
    private static int currentSceneIndex = 0;
    public static Animation switchAnimation = new Animation();

    @Override
    public void initGui() {
        super.initGui();
        if (scenes.isEmpty()) {
            scenes.add(new Login(true));
            currentScene = scenes.get(0);
            switchAnimation.start(0.0, 100.0, 0.3f, Type.EASE_IN_OUT_QUAD);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        switchAnimation.update();

        if (switchAnimation.value < 99.9 && currentSceneIndex > 0) {
            Scene previousScene = scenes.get(currentSceneIndex - 1);
            previousScene.drawScreen(mouseX, mouseY, partialTicks);

            Render2DUtils.drawRect(
                    0f, 0f, sr.getScaledWidth(), sr.getScaledHeight(),
                    new Color(0, 0, 0, Render2DUtils.limit(2 * switchAnimation.value))
            );

            GL11.glTranslatef((float) (sr.getScaledWidth() * (1 - switchAnimation.value / 100f)), 0f, 0f);
            currentScene.drawScreen(mouseX, mouseY, partialTicks);
            GL11.glTranslatef((float) (-sr.getScaledWidth() * (1 - switchAnimation.value / 100f)), 0f, 0f);
        } else {
            currentScene.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (switchAnimation.value > 99.9) {
            currentScene.mouseClick(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (switchAnimation.value > 99.9) {
            currentScene.keyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    public void nextScene() {
        if (currentSceneIndex < scenes.size() - 1) {
            currentSceneIndex++;
            currentScene = scenes.get(currentSceneIndex);
            switchAnimation.reset();
            switchAnimation.start(0.0, 100.0, 0.5f, Type.EASE_IN_OUT_QUAD);
        } else {
            currentSceneIndex = 0;
            currentScene = scenes.get(currentSceneIndex);
        }
    }
}
