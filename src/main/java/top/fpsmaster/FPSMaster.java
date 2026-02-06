package top.fpsmaster;

import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.GlobalListener;
import top.fpsmaster.features.command.CommandManager;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.manager.ModuleManager;
import top.fpsmaster.font.FontManager;
import top.fpsmaster.modules.client.thread.ClientThreadPool;
import top.fpsmaster.modules.config.ConfigManager;
import top.fpsmaster.modules.i18n.Language;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.modules.music.netease.NeteaseApi;
import top.fpsmaster.ui.custom.ComponentsManager;
import top.fpsmaster.utils.git.GitInfo;
import top.fpsmaster.utils.io.FileUtils;

import java.io.File;

public class FPSMaster {

    public boolean hasOptifine;

    public static final String EDITION = "Edge";
    public static final String COPYRIGHT = "Copyright ©2020-2025  FPSMaster Team  All Rights Reserved.";

    public static FPSMaster INSTANCE = new FPSMaster();

    public static String CLIENT_NAME = "FPSMaster";
    public static String CLIENT_VERSION = "4.0.0";

    public static ModuleManager moduleManager = new ModuleManager();
    public static FontManager fontManager = new FontManager();
    public static ConfigManager configManager = new ConfigManager();
    public static GlobalListener submitter = new GlobalListener();
    public static CommandManager commandManager = new CommandManager();
    public static ComponentsManager componentsManager = new ComponentsManager();
    public static Language i18n = new Language();
    public static ClientThreadPool async = new ClientThreadPool(100);
    public static boolean development = false;
    public static boolean isLatest = true;
    public static boolean updateFailed = false;
    public static String latest = "";

    private static void checkDevelopment() {
        try {
            Class.forName("net.fabricmc.devlaunchinjector.Main");
            development = true;
        } catch (Throwable ignored) {
        }
    }

    public static String getClientTitle() {
        checkDevelopment();
        return CLIENT_NAME + " " + EDITION + " (" + GitInfo.getBranch() + " - " + GitInfo.getCommitIdAbbrev() + ")" + (development ? " - dev" : "");
    }

    private void initializeFonts() {
        ClientLogger.info("Initializing Fonts...");

        fontManager.load();
    }

    private void initializeLang() throws FileException {
        ClientLogger.info("Initializing I18N...");
        if (ClientSettings.language.getValue() == 1) {
            i18n.read("zh_cn");
        } else {
            i18n.read("en_us");
        }
    }

    private void initializeConfigures() throws Exception {
        ClientLogger.info("Initializing Config...");
        configManager.loadConfig("default");
        MusicPlayer.setVolume(Float.parseFloat(configManager.configure.getOrCreate("volume", "1")));
        NeteaseApi.cookies = FileUtils.readTempValue("cookies");
    }

    private void initializeMusic() {
        ClientLogger.info("Checking music cache...");
        long dirSize = FileUtils.getDirSize(FileUtils.artists);
        if (dirSize > 1024) {
            if (FileUtils.artists.delete()) {
                ClientLogger.info("Cleared img cache");
            } else {
                ClientLogger.error("Clear img cache failed");
            }
        }
        ClientLogger.info("Found image: " + dirSize + "mb");
        long dirSize1 = FileUtils.getDirSize(FileUtils.music);
        if (dirSize1 > 2048) {
            if (FileUtils.music.delete()) {
                ClientLogger.warn("Cleared music cache");
            } else {
                ClientLogger.error("Clear music cache failed");
            }
        }
        ClientLogger.info("Found music: " + dirSize1 + "mb");
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
    }


    private void checkOptifine() {
        try {
            Class.forName("optifine.Patcher");
            hasOptifine = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void checkUpdate() {
        isLatest = true;
        updateFailed = false;
        latest = GitInfo.getCommitId();
    }

    public void initialize() {
        try {
            initializeFonts();
            initializeMusic();
            initializeModules();
            initializeComponents();
            initializeConfigures();
            initializeCommands();
            initializeLang();
            checkUpdate();
            checkOptifine();
        } catch (Exception e) {
            ExceptionHandler.handle(e);
        }
    }


    public void shutdown() {
        try {
            ClientLogger.info("Saving configs");
            configManager.saveConfig("default");
        } catch (FileException e) {
            throw new RuntimeException(e);
        }
    }
}



