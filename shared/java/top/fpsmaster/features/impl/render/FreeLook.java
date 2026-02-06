package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BindSetting;

public class FreeLook extends Module {
    private final BindSetting bind = new BindSetting("bind", Keyboard.KEY_LMENU);

    public FreeLook() {
        super("FreeLook", Category.RENDER);
        addSettings(bind);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    @Subscribe
    public void onRender3D(EventRender3D e) {
        if (Minecraft.getMinecraft().currentScreen != null) return;

        if (!perspectiveToggled) {
                if (Keyboard.isKeyDown(bind.getValue())) {
                    perspectiveToggled = true;
                if (Minecraft.getMinecraft().thePlayer != null) {
                    cameraYaw = Minecraft.getMinecraft().thePlayer.rotationYaw;
                    cameraPitch = Minecraft.getMinecraft().thePlayer.rotationPitch;
                }
                previousPerspective = Minecraft.getMinecraft().gameSettings.hideGUI;
                Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
            }
        } else if (!Keyboard.isKeyDown(bind.getValue())) {
            perspectiveToggled = false;
            Minecraft.getMinecraft().gameSettings.thirdPersonView = previousPerspective ? 1 : 0;
        }
    }

    public static boolean using = false;
    public static boolean perspectiveToggled = false;
    public static float cameraYaw = 0f;
    public static float cameraPitch = 0f;
    private static boolean previousPerspective = false;

    public static float getCameraYaw() {
        return perspectiveToggled ? cameraYaw : Minecraft.getMinecraft().getRenderViewEntity().rotationYaw;
    }

    public static float getCameraPitch() {
        return perspectiveToggled ? cameraPitch : Minecraft.getMinecraft().getRenderViewEntity().rotationPitch;
    }

    public static float getCameraPrevYaw() {
        return perspectiveToggled ? cameraYaw : Minecraft.getMinecraft().getRenderViewEntity().prevRotationYaw;
    }

    public static float getCameraPrevPitch() {
        return perspectiveToggled ? cameraPitch : Minecraft.getMinecraft().getRenderViewEntity().prevRotationPitch;
    }

    public static boolean overrideMouse() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.inGameHasFocus && Display.isActive()) {
            if (!perspectiveToggled) {
                return true;
            }
            mc.mouseHelper.mouseXYChange();
            float f1 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
            float f2 = f1 * f1 * f1 * 8.0f;
            float f3 = mc.mouseHelper.deltaX * f2;
            float f4 = mc.mouseHelper.deltaY * f2;
            cameraYaw += f3 * 0.15f;
            cameraPitch += f4 * 0.15f;
            if (cameraPitch > 90.0f) {
                cameraPitch = 90.0f;
            }
            if (cameraPitch < -90.0f) {
                cameraPitch = -90.0f;
            }
        }
        return false;
    }
}
