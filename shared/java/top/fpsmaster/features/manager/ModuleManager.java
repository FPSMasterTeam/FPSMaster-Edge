package top.fpsmaster.features.manager;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.features.impl.interfaces.*;
import top.fpsmaster.features.impl.optimizes.*;
import top.fpsmaster.features.impl.render.*;
import top.fpsmaster.features.impl.utility.*;
import top.fpsmaster.modules.dev.DevMode;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.ModuleRenderer;
import top.fpsmaster.ui.devspace.DevSpace;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public List<Module> modules = new ArrayList<>();
    public MainPanel mainPanel = new MainPanel();

    public Module getModule(Class<?> mod) {
        for (Module module : modules) {
            if (module.getClass() == mod) {
                return module;
            }
        }
        ClientLogger.warn("Missing module: " + mod.getName());
        return new Module("missing module", "mission", Category.Utility);
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (e.key == ClientSettings.keyBind.getValue()) {
            if (Minecraft.getMinecraft().currentScreen == null) {
                Minecraft.getMinecraft().displayGuiScreen(mainPanel);
            }
        }

        for (Module module : modules) {
            if (e.key == module.key) {
                module.toggle();
            }
        }
//        if (e.key == Keyboard.KEY_INSERT) {
//            Minecraft.getMinecraft().displayGuiScreen(new CosmeticScreen());
//        }
        if (e.key == Keyboard.KEY_INSERT && DevMode.INSTACE.dev) {
            Minecraft.getMinecraft().displayGuiScreen(new DevSpace());
        }
    }

    public void addModule(Module module) {
        modules.add(module);
        mainPanel.mods.add(new ModuleRenderer(module));
    }

    public void removeModule(Module module) {
        modules.remove(module);
        mainPanel.mods.removeIf(m -> m.mod == module);
    }

    public void init() {
        // Register listener
        EventDispatcher.registerListener(this);
        // Add modules
        modules.add(new ClientSettings());
        modules.add(new BetterScreen());
        modules.add(new Sprint());
        modules.add(new Performance());
        modules.add(new MotionBlur());
        modules.add(new SmoothZoom());
        modules.add(new WavyCape());
        modules.add(new FullBright());
        modules.add(new ItemPhysics());
        modules.add(new MinimizedBobbing());
        modules.add(new MoreParticles());
        modules.add(new FPSDisplay());
        modules.add(new IRC());
        modules.add(new ArmorDisplay());
        modules.add(new BetterChat());
        modules.add(new BetterFishingRod());
        modules.add(new ComboDisplay());
        modules.add(new CPSDisplay());
        modules.add(new PotionDisplay());
        modules.add(new ReachDisplay());
        modules.add(new Scoreboard());
        modules.add(new MusicOverlay());
        modules.add(new OldAnimations());
        modules.add(new HitColor());
        modules.add(new BlockOverlay());
        modules.add(new CleanView());
        modules.add(new DragonWings());
        modules.add(new FireModifier());
        modules.add(new FreeLook());
        modules.add(new AutoGG());
        modules.add(new LyricsDisplay());
        modules.add(new SkinChanger());
        modules.add(new TimeChanger());
        modules.add(new TNTTimer());
        modules.add(new Hitboxes());
        modules.add(new LevelTag());
        modules.add(new Keystrokes());
        modules.add(new Crosshair());
        modules.add(new CustomTitles());
        modules.add(new CustomFOV());
        modules.add(new InventoryDisplay());
        modules.add(new PlayerDisplay());
        modules.add(new TargetDisplay());
        modules.add(new NoHurtCam());
        modules.add(new NameProtect());
        modules.add(new RawInput());
        modules.add(new FixedInventory());
        modules.add(new NoHitDelay());
        modules.add(new PingDisplay());
        modules.add(new CoordsDisplay());
        modules.add(new ModsList());
        modules.add(new MiniMap());
        modules.add(new DirectionDisplay());
        modules.add(new DamageIndicator());
        modules.add(new TabOverlay());
        modules.add(new ItemCountDisplay());
        modules.add(new SoundModifier());
        modules.add(new ParticlesModifier());

        if (FPSMaster.EDITION.equals("1.12.2")) {
            modules.add(new HideIndicator());
        }

        for (Module m : FPSMaster.moduleManager.modules) {
            mainPanel.mods.add(new ModuleRenderer(m));
        }
    }
}
