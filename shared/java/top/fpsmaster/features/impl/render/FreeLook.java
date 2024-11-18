package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.wrapper.mods.WrapperFreeLook;

public class FreeLook extends Module {
    private BindSetting bind = new BindSetting("bind", Keyboard.KEY_LMENU);

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
                cameraYaw = ProviderManager.mcProvider.getPlayer().rotationYaw;
                cameraPitch = ProviderManager.mcProvider.getPlayer().rotationPitch;
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
        return WrapperFreeLook.getCameraYaw();
    }

    public static float getCameraPitch() {
        return WrapperFreeLook.getCameraPitch();
    }

    public static float getCameraPrevYaw() {
        return WrapperFreeLook.getCameraPrevYaw();
    }

    public static float getCameraPrevPitch() {
        return WrapperFreeLook.getCameraPrevPitch();
    }

    public static boolean overrideMouse() {
        return WrapperFreeLook.overrideMouse();
    }
}
