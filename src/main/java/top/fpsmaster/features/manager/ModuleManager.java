package top.fpsmaster.features.manager;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventKey;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.impl.interfaces.*;
import top.fpsmaster.features.impl.optimizes.*;
import top.fpsmaster.features.impl.render.*;
import top.fpsmaster.features.impl.utility.*;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.MainPanel;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public List<Module> modules = new ArrayList<>();
    public MainPanel mainPanel = new MainPanel();
    private boolean initialized;

    public <T extends Module> T getModule(Class<T> mod) {
        for (Module module : modules) {
            if (module.getClass() == mod) {
                return mod.cast(module);
            }
        }
        throw new IllegalStateException("Missing module: " + mod.getName());
    }

    public Module getModule(Class<?> mod, boolean unused) {
        for (Module module : modules) {
            if (module.getClass() == mod) {
                return module;
            }
        }
        throw new IllegalStateException("Missing module: " + mod.getName());
    }

    public <T extends Module> T requireModule(Class<T> mod) {
        return getModule(mod);
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
    }

    public void addModule(Module module) {
        modules.add(module);
    }

    public void removeModule(Module module) {
        modules.remove(module);
    }

    public void init() {
        if (initialized) {
            ClientLogger.warn("ModuleManager already initialized");
            return;
        }
        initialized = true;
        // Register listener
        EventDispatcher.registerListener(this);
        // Add modules
        modules.add(new ClientSettings());
        modules.add(new BetterScreen());
        modules.add(new Sprint());
        modules.add(new Replay());
        modules.add(new ToggleSneak());
        modules.add(new Performance());
        modules.add(new MotionBlur());
        modules.add(new SmoothZoom());
        modules.add(new FullBright());
        modules.add(new ItemPhysics());
        modules.add(new MinimizedBobbing());
        modules.add(new MoreParticles());
        modules.add(new ChatAvatars());
        modules.add(new FPSDisplay());
        modules.add(new SaturationDisplay());
        modules.add(new ArmorDisplay());
        modules.add(new BetterChat());
        modules.add(new BlockIndicator());
        modules.add(new ClockDisplay());
        modules.add(new ServerAddressDisplay());
        modules.add(new PlayTime());
        modules.add(new BetterFishingRod());
        modules.add(new ComboDisplay());
        modules.add(new CPSDisplay());
        modules.add(new PotionDisplay());
        modules.add(new ReachDisplay());
        modules.add(new Scoreboard());
        modules.add(new OldAnimations());
        modules.add(new HitColor());
        modules.add(new BlockOverlay());
        modules.add(new CleanView());
        modules.add(new FireModifier());
        modules.add(new FreeLook());
        modules.add(new AutoGG());
        modules.add(new AutoText());
        modules.add(new TimeChanger());
        modules.add(new TNTTimer());
        modules.add(new Hitboxes());
        modules.add(new LevelTag());
        modules.add(new Keystrokes());
        modules.add(new Crosshair());
        modules.add(new CustomTitles());
        modules.add(new CustomFOV());
        modules.add(new CustomFog());
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
        modules.add(new PerformanceHud());
        modules.add(new ModsList());
//        modules.add(new MiniMap());
        modules.add(new DirectionDisplay());
        modules.add(new DamageIndicator());
        modules.add(new TabOverlay());
        modules.add(new ItemCountDisplay());
        modules.add(new LyricsDisplay());
        modules.add(new SoundModifier());
        modules.add(new ParticlesModifier());

        modules.add(new HideIndicator());

        // Shared HUD appearance settings are appended centrally, after each module registered its own,
        // so every InterfaceModule ends up with a consistent panel regardless of what its constructor did.
        for (Module m : FPSMaster.moduleManager.modules) {
            if (m instanceof InterfaceModule) {
                ((InterfaceModule) m).registerCommonSettings();
            }
        }
    }
}
