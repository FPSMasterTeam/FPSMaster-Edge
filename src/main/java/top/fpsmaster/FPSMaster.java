package top.fpsmaster;

import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.GlobalListener;
import top.fpsmaster.replay.director.DirectorWorldOverlay;
import top.fpsmaster.features.command.CommandManager;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.manager.ModuleManager;
import top.fpsmaster.font.FontManager;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.thread.ClientThreadPool;
import top.fpsmaster.modules.client.telemetry.EdgeTelemetryReporter;
import top.fpsmaster.modules.config.ConfigManager;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.i18n.Language;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.statistics.PlayTimeStatistics;
import top.fpsmaster.ui.custom.ComponentsManager;
import top.fpsmaster.utils.git.GitInfo;
import top.fpsmaster.utils.io.FileUtils;

import java.io.File;

import static top.fpsmaster.utils.core.Utility.mc;

public class FPSMaster {

    public boolean hasOptifine;

    public static final String EDITION = "Edge";
    public static final String COPYRIGHT = "Copyright ©2020-2026  FPSMaster Team  All Rights Reserved.";

    public static FPSMaster INSTANCE = new FPSMaster();

    public static String CLIENT_NAME = "FPSMaster";
    public static String CLIENT_VERSION = "1.0.2-beta";

    public static ModuleManager moduleManager = new ModuleManager();
    public static FontManager fontManager = new FontManager();
    public static ConfigManager configManager = new ConfigManager();
    public static GlobalListener submitter = new GlobalListener();
    public static CommandManager commandManager = new CommandManager();
    public static ComponentsManager componentsManager = new ComponentsManager();
    public static Language i18n = new Language();
    public static ClientThreadPool async = new ClientThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    public static EdgeTelemetryReporter telemetryReporter = new EdgeTelemetryReporter();
    public static boolean development = false;
    public static boolean defaultConfigExistedBeforeLoad = false;

    private static void checkDevelopment() {
        try {
            Class.forName("net.fabricmc.devlaunchinjector.Main");
            development = true;
        } catch (Throwable ignored) {
        }
    }

    public static boolean isDevelopment() {
        checkDevelopment();
        return development;
    }

    public static String getClientTitle() {
        checkDevelopment();
        return CLIENT_NAME + " " + EDITION + " (" + GitInfo.getBranch() + " - " + GitInfo.getCommitIdAbbrev() + ")" + (development ? " - dev" : "");
    }

    private void initializeAuth() {
        ClientLogger.info("Initializing Auth Service...");
        AuthService.getInstance().initialize();
    }

    private void initializeFonts() {
        ClientLogger.info("Initializing Fonts...");

        FileUtils.releaseFont("NotoSansSC-Regular.ttf");
        fontManager.load();
    }

    private void initializeLang() throws FileException {
        ClientLogger.info("Initializing I18N...");
        i18n.init();
        if (ClientSettings.language.getValue() == 1) {
            i18n.read("zh_cn");
        } else {
            i18n.read("en_us");
        }
    }

    private void initializeConfigures() throws Exception {
        ClientLogger.info("Initializing Config...");
        File defaultConfig = ConfigProfileUtils.getCurrentConfigFile();
        ConfigProfileUtils.migrateLegacyDefaultIfNeeded();
        defaultConfigExistedBeforeLoad = defaultConfig.exists();
        String activeProfile = ConfigProfileUtils.loadActiveProfileName();
        configManager.loadConfig(activeProfile);
    }


    private void initializeComponents() {
        ClientLogger.info("Initializing component...");
        componentsManager.init();
    }

    private void initializeCommands() {
        ClientLogger.info("Initializing commands");
        commandManager.init();
    }

    private void initializeModules() {
        moduleManager.init();
        submitter.init();
        DirectorWorldOverlay.init();
    }


    /**
     * With the Retina backing on, displayWidth doubles and a fixed vanilla guiScale (1-3) suddenly
     * means GUIs at half their previous visual size. Double it once so the first HiDPI launch looks
     * exactly like the last non-HiDPI one, just sharp. Auto (0) already adapts and is left alone.
     */
    private static boolean hidpiMigrationChecked = false;

    /** Runs on the first client tick — during startup Forge reloads options.txt after FML init
     * (finishMinecraftLoading -> loadOptions), which would undo the change if done any earlier. */
    public void migrateGuiScaleForHiDpi() {
        if (hidpiMigrationChecked) {
            return;
        }
        if (configManager == null || configManager.configure == null || mc.gameSettings == null) {
            return;
        }
        hidpiMigrationChecked = true;
        if (!top.fpsmaster.utils.system.HiDpi.active() || configManager.configure.hidpiGuiScaleMigrated) {
            return;
        }
        configManager.configure.hidpiGuiScaleMigrated = true;
        int guiScale = mc.gameSettings.guiScale;
        if (guiScale >= 1 && guiScale <= 3) {
            mc.gameSettings.guiScale = guiScale * 2;
            mc.gameSettings.saveOptions();
            ClientLogger.info("HiDPI: doubled vanilla guiScale " + guiScale + " -> " + mc.gameSettings.guiScale);
        }
    }

    private void checkOptifine() {
        try {
            Class.forName("optifine.Patcher");
            hasOptifine = true;
            ClientLogger.info("OptiFine detected (optifine.Patcher)");
        } catch (ClassNotFoundException ignored) {
            hasOptifine = false;
            ClientLogger.info("OptiFine not present");
        }
    }

    private static volatile boolean initialized = false;

    public void initialize() {
        // Idempotent: the Forge path enters here via FMLInitializationEvent, while the
        // Forge-free path (MixinMinecraft startGame RETURN) may also call it. Whoever wins
        // the race initializes; the loser is a no-op. Never initialize the client twice.
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            FileUtils.init(mc.mcDataDir);
            initializeAuth();  // Initialize auth service early to check for launcher tokens
            initializeFonts();
            initializeModules();
            initializeComponents();
            initializeConfigures();
            initializeCommands();
            initializeLang();
            checkOptifine();
        } catch (Exception e) {
            ExceptionHandler.handle(e);
        }
    }


    public void shutdown() {
        // Before anything else: an unfinished stream is an unreadable file.
        top.fpsmaster.replay.ReplayRecorder.instance().stop();
        PlayTimeStatistics.flush();
        // Release SMTC native session before tearing down async/audio
        try {
            top.fpsmaster.modules.music.MusicManager.get().shutdownSmtc();
        } catch (Throwable t) {
            ClientLogger.warn("SMTC shutdown skipped: " + t.getMessage());
        }
        telemetryReporter.shutdown();
        async.close();
        try {
            ClientLogger.info("Saving configs");
            configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
        } catch (FileException e) {
            ExceptionHandler.handleFileException(e, "Failed to save default config during shutdown");
        }
    }
}



