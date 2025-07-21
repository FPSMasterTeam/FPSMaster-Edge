package top.fpsmaster;

import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.GlobalListener;
import top.fpsmaster.features.command.CommandManager;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.manager.ModuleManager;
import top.fpsmaster.font.FontManager;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.modules.client.ClientThreadPool;
import top.fpsmaster.modules.client.ClientUsersManager;
import top.fpsmaster.modules.config.ConfigManager;
import top.fpsmaster.modules.i18n.Language;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.lua.LuaManager;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.modules.music.netease.NeteaseApi;
import top.fpsmaster.ui.click.music.MusicPanel;
import top.fpsmaster.ui.click.music.NewMusicPanel;
import top.fpsmaster.ui.custom.ComponentsManager;
import top.fpsmaster.ui.screens.oobe.OOBEScreen;
import top.fpsmaster.utils.GitInfo;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.os.HttpRequest;
import top.fpsmaster.utils.thirdparty.github.UpdateChecker;
import top.fpsmaster.websocket.client.WsClient;
import top.fpsmaster.wrapper.Constants;

import java.io.File;

public class FPSMaster {

    public boolean hasOptifine;
    public boolean loggedIn;
    public WsClient wsClient;

    public static final String phase = "beta";
    public static final String SERVICE_API = "https://service.fpsmaster.top";

    public static final String EDITION = Constants.EDITION;
    public static final String COPYRIGHT = "Copyright ©2020-2025  FPSMaster Team  All Rights Reserved.";

    public static FPSMaster INSTANCE = new FPSMaster();

    public static String CLIENT_NAME = "FPSMaster";
    public static String CLIENT_VERSION = "v4";

    public static ModuleManager moduleManager = new ModuleManager();
    public static FontManager fontManager = new FontManager();
    public static ConfigManager configManager = new ConfigManager();
    public static OOBEScreen oobeScreen = new OOBEScreen();
    public static AccountManager accountManager = new AccountManager();
    public static ClientUsersManager clientUsersManager = new ClientUsersManager();
    public static GlobalListener submitter = new GlobalListener();
    public static CommandManager commandManager = new CommandManager();
    public static ComponentsManager componentsManager = new ComponentsManager();
    public static LuaManager luaManager = new LuaManager();
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
        return CLIENT_NAME + " Client (" + phase + ") " + Constants.VERSION + " (" + GitInfo.getBranch() + " - " + GitInfo.getCommitIdAbbrev() + ")" + (development ? " - dev" : "");
    }

    private void initializeFonts() {
        ClientLogger.info("Initializing Fonts...");
        // add more fonts and add fallback font
        File file = new File(FileUtils.fonts, "harmony_bold.ttf");
        if (!file.exists()) {
            ClientLogger.info("Downloading Fonts...");
            HttpRequest.downloadFile("https://13430.kstore.space/harmony_bold.ttf", file.getAbsolutePath());
        }

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
        MusicPanel.nickname = FileUtils.readTempValue("nickname");
        NewMusicPanel.nickname = FileUtils.readTempValue("nickname");
        accountManager.autoLogin();
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

    private void initializePlugins() throws FileException {
        luaManager.init();
    }

    private void checkOptifine() {
        try {
            Class.forName("optifine.Patcher");
            hasOptifine = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void checkUpdate() {
        if (development) {
            isLatest = true;
            return;
        }
        async.runnable(() -> {
            String s = UpdateChecker.getLatestVersion();
            if (s == null || s.isEmpty()) {
                s = UpdateChecker.getLatestVersion();
                if (s == null || s.isEmpty()) {
                    isLatest = false;
                    updateFailed = true;
                    ClientLogger.error("获取最新版本信息失败");
                    return;
                }
            }
            s = s.trim();
            latest = s;
            isLatest = GitInfo.getCommitId().equals(s);
        });
    }

    public void initialize() {
        try {
            initializeFonts();
            initializeMusic();
            initializeModules();
            initializeComponents();
            initializeConfigures();
            initializeCommands();
            initializePlugins();
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
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.close(200, "Shutdown");
            }
        } catch (FileException e) {
            throw new RuntimeException(e);
        }
    }
}