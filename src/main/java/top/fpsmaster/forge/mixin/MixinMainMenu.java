package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.ui.screens.oobe.OobeScreen;

@Mixin(GuiMainMenu.class)
public class MixinMainMenu {

    /**
     * @author SuperSkidder
     * @reason replace
     */
    @Overwrite
    public void initGui(){
        // Under Forge-free launch, startGame displays GuiMainMenu before our startGame-RETURN
        // init runs — so initialize here first (idempotent) before OobeScreen captures fonts.
        if (Boolean.getBoolean("fpsmaster.noforge")) {
            FPSMaster.INSTANCE.initialize();
        }
        Minecraft.getMinecraft().displayGuiScreen(FPSMaster.configManager.configure.oobeCompleted ? new MainMenu() : new OobeScreen());
    }
}



