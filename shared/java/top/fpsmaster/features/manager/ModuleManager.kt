package top.fpsmaster.features.manager

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import top.fpsmaster.FPSMaster
import top.fpsmaster.event.EventDispatcher.registerListener
import top.fpsmaster.event.Subscribe
import top.fpsmaster.event.events.*
import top.fpsmaster.modules.logger.Logger.warn
import top.fpsmaster.features.impl.interfaces.*
import top.fpsmaster.features.impl.optimizes.*
import top.fpsmaster.features.impl.render.*
import top.fpsmaster.features.impl.utility.*
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.ui.click.MainPanel
import top.fpsmaster.ui.click.modules.ModuleRenderer
import java.util.*

class ModuleManager {
    var modules = ArrayList<Module>()
    private val mainPanel: MainPanel = MainPanel(true)
    fun getModule(mod: Class<*>): Module {
        for (module in modules) {
            if (module.javaClass == mod) {
                return module
            }
        }
        warn("Missing module:" + mod.name)
        return Module("missing module", "mission", Category.Utility)
    }

    @Subscribe
    fun onKey(e: EventKey) {
        if (e.key == ClientSettings.keyBind.value) if (Minecraft.getMinecraft().currentScreen == null) Minecraft.getMinecraft()
            .displayGuiScreen(mainPanel)
        for (module in modules) {
            if (e.key == module.key) {
                module.toggle()
            }
        }
    }

    fun addModule(module: Module) {
        modules.add(module)
        mainPanel.mods.add(ModuleRenderer(module))
    }

    fun removeModule(module: Module) {
        modules.remove(module)
        mainPanel.mods.removeIf { it.mod == module }
    }

    fun init() {

        // register listener
        registerListener(this)
        // add mods
        modules.add(ClientSettings())
        modules.add(BetterScreen())
        modules.add(Sprint())
        modules.add(Performance())
        modules.add(MotionBlur())
        modules.add(SmoothZoom())
        modules.add(FullBright())
        modules.add(ItemPhysics())
        modules.add(MinimizedBobbing())
        modules.add(MoreParticles())
        modules.add(FPSDisplay())
        modules.add(IRC())
        modules.add(ArmorDisplay())
        modules.add(BetterChat())
        modules.add(ComboDisplay())
        modules.add(CPSDisplay())
        modules.add(PotionDisplay())
        modules.add(ReachDisplay())
        modules.add(Scoreboard())
        modules.add(MusicOverlay())
        modules.add(OldAnimations())
        modules.add(HitColor())
        modules.add(BlockOverlay())
        modules.add(DragonWings())
        modules.add(FireModifier())
        modules.add(FreeLook())
        modules.add(LyricsDisplay())
        modules.add(SkinChanger())
        modules.add(TimeChanger())
        modules.add(TNTTimer())
        modules.add(Hitboxes())
        modules.add(LevelTag())
        modules.add(Keystrokes())
        modules.add(Crosshair())
        modules.add(CustomFOV())
//        modules.add(TabOverlay())
        modules.add(InventoryDisplay())
        modules.add(PlayerDisplay())
        modules.add(TargetDisplay())
        modules.add(NoHurtCam())
        modules.add(NameProtect())
//        modules.add(ChatBot())
//        modules.add(Translator())
        modules.add(RawInput())
        modules.add(FixedInventory())
        modules.add(NoHitDelay())
        modules.add(PingDisplay())
        modules.add(CoordsDisplay())
        modules.add(ModsList())
        modules.add(ClientCommand())
        modules.add(MiniMap())
        modules.add(DirectionDisplay())

        if (ProviderManager.constants.getVersion() == "1.12.2") {
            modules.add(HideIndicator())
        }
        for (m in FPSMaster.moduleManager.modules) {
            mainPanel.mods.add(ModuleRenderer(m))
        }
    }
}
