package top.fpsmaster.ui.screens.mainmenu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.AccountException;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.modules.account.MicrosoftAuth;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.ui.mc.GuiMultiplayer;
import top.fpsmaster.ui.screens.music.MusicScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.prism.screen.MenuBridge;
import top.fpsmaster.prism.screen.SharedAccountOverlay;
import top.fpsmaster.prism.screen.SharedMainMenu;
import top.fpsmaster.prism.widget.UiFrame;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.BezierEasing;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.system.OSUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Edge main menu, after docs/prototypes/main-menu.html: account capsule top-left, round action
 * buttons top-right, hero wordmark bottom-left, tile dock along the bottom.
 */
public class MainMenu extends ScaledGuiScreen {
    private static int firstBoot = 0;
    private static final Gson GSON = new Gson();
    private static final ResourceLocation DEFAULT_SKIN = new ResourceLocation("textures/entity/steve.png");
    private static final int SKIN_REQUEST_TIMEOUT_MS = 2500;
    private static final AtomicBoolean SKIN_LOADING = new AtomicBoolean(false);
    private static volatile ResourceLocation playerSkinTexture;
    private static volatile String loadedSkinPlayerId = "";
    private static volatile boolean playerSkinLoadFailed;

    private static final BezierEasing EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    /** Static so re-entering the menu (from settings etc.) does not replay the intro. */
    private static final Animator introAnimation = new Animator();
    private final AnimClock animClock = new AnimClock();
    private final MenuBridge menuBridge = new EdgeMenuBridge();
    private final SharedAccountOverlay accountOverlay = new SharedAccountOverlay();
    private final AtomicBoolean msLoginCancel = new AtomicBoolean(false);
    private final AtomicInteger msLoginGeneration = new AtomicInteger();
    private volatile boolean msLoginBusy;
    private volatile String msUserCode = "";
    private volatile String msVerifyUrl = "";
    private volatile String msStatus = "";
    private volatile String msError = "";

    /** Last server in the list, pinged once per menu session for the continue card. */
    private static ServerData continueServer;
    private static boolean continueServerLoaded;

    @Override
    public void initGui() {
        super.initGui();
        Backgrounds.initGui();
        animClock.reset();
        if (firstBoot == 0) {
            firstBoot = checkJavaVersion();
        }
        if (!continueServerLoaded) {
            continueServerLoaded = true;
            try {
                ServerList servers = new ServerList(mc);
                if (servers.countServers() > 0) {
                    continueServer = servers.getServerData(0);
                }
            } catch (RuntimeException exception) {
                ClientLogger.warn("Failed to read servers.dat for continue card: " + exception);
            }
        }
    }

    private static int checkJavaVersion() {
        String version = System.getProperty("java.version", "");
        if (!version.startsWith("1.8.0")) {
            return 2;
        }
        int underscore = version.indexOf('_');
        if (underscore < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(version.substring(underscore + 1)) >= 382 ? 2 : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);
        double dt = animClock.tick();
        if (!introAnimation.isRunning() && introAnimation.get() == 0.0) {
            introAnimation.start(0, 1, 0.6, EASE);
        }
        introAnimation.update(dt);
        float intro = (float) introAnimation.get();

        preloadPlayerSkinTexture();
        SharedMainMenu.draw(EdgeUi.frame(), menuBridge);
        accountOverlay.draw(EdgeUi.frame(), menuBridge);

        if (firstBoot != 2) {
            FPSMaster.fontManager.s14.drawCenteredString(
                    FPSMaster.i18n.get(firstBoot == 0 ? "mainmenu.oldjava" : "mainmenu.javafail"),
                    guiWidth / 2f, 34f, ClickGuiTheme.danger().getRGB());
        }

        // Intro: world simply fades in from black; no lingering overlay art.
        if (intro < 1f) {
            Rects.fill(0, 0, guiWidth, guiHeight, new Color(10, 10, 10, (int) (255 * (1f - intro))));
        }
    }

    private String accountTypeLabel(AccountManager accounts) {
        if (accounts.isCurrentOnline()) {
            return FPSMaster.i18n.get("mainmenu.account.ms");
        }
        return FPSMaster.i18n.get("mainmenu.account.offline");
    }

    private void drawAvatar(float x, float y, float size) {
        if (playerSkinTexture != null) {
            Images.playerHead(playerSkinTexture, x, y, (int) size, (int) size);
        } else if (playerSkinLoadFailed) {
            Images.playerHead(DEFAULT_SKIN, x, y, (int) size, (int) size);
        } else {
            Rects.rounded(x, y, size, size, 4, new Color(125, 141, 255).getRGB(), false);
        }
    }

    private void startMicrosoftLogin() {
        msLoginCancel.set(false);
        msLoginBusy = true;
        msUserCode = "";
        msVerifyUrl = "";
        msStatus = FPSMaster.i18n.get("mainmenu.account.ms.starting");
        msError = "";
        final int generation = msLoginGeneration.incrementAndGet();
        FPSMaster.async.runnable(new Runnable() {
            @Override
            public void run() {
                runMicrosoftLogin(generation);
            }
        });
    }

    private void cancelMicrosoftLogin() {
        msLoginCancel.set(true);
        msLoginGeneration.incrementAndGet();
        msLoginBusy = false;
    }

    private void runMicrosoftLogin(int generation) {
        try {
            MicrosoftAuth.DeviceLogin start = MicrosoftAuth.startDeviceLogin();
            if (msLoginCancel.get() || generation != msLoginGeneration.get()) {
                return;
            }
            msUserCode = start.userCode;
            msVerifyUrl = start.browserUrl();
            msStatus = FPSMaster.i18n.get("mainmenu.account.ms.waiting");
            msLoginBusy = false;
            openLink(msVerifyUrl);
            long deadline = System.currentTimeMillis() + Math.max(30, start.expiresIn) * 1000L;
            int interval = Math.max(1, start.interval);
            while (!msLoginCancel.get() && generation == msLoginGeneration.get()
                    && System.currentTimeMillis() < deadline) {
                MicrosoftAuth.PollResult poll = MicrosoftAuth.pollDeviceLogin(start.deviceCode);
                if (msLoginCancel.get() || generation != msLoginGeneration.get()) {
                    return;
                }
                if (poll.account != null) {
                    final MicrosoftAuth.MinecraftProfile profile = poll.account;
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            AccountManager.get().addAndUseMicrosoft(profile);
                            accountOverlay.closeDialog();
                        }
                    });
                    return;
                }
                if ("denied".equals(poll.status)) {
                    msError = FPSMaster.i18n.get("mainmenu.account.ms.denied");
                    msStatus = "";
                    return;
                }
                if ("expired".equals(poll.status)) {
                    msError = FPSMaster.i18n.get("mainmenu.account.ms.expired");
                    msStatus = "";
                    return;
                }
                if (poll.interval > 0) {
                    interval = poll.interval;
                }
                sleepInterruptibly(interval * 1000L);
            }
            if (!msLoginCancel.get() && (msError == null || msError.isEmpty())) {
                msError = FPSMaster.i18n.get("mainmenu.account.ms.expired");
                msStatus = "";
            }
        } catch (IOException | AccountException exception) {
            if (msLoginCancel.get()) {
                return;
            }
            msError = localizeMicrosoftError(exception.getMessage());
            msStatus = "";
            ClientLogger.warn("Microsoft login failed: " + exception.getMessage());
        } finally {
            msLoginBusy = false;
        }
    }

    private String localizeMicrosoftError(String message) {
        if (message == null || message.isEmpty()) {
            return FPSMaster.i18n.get("mainmenu.account.ms.failed").replace("%s", "unknown");
        }
        if ("NO_JAVA_LICENSE".equals(message)) {
            return FPSMaster.i18n.get("mainmenu.account.ms.nolicense");
        }
        if ("NO_JAVA_PROFILE".equals(message)) {
            return FPSMaster.i18n.get("mainmenu.account.ms.noprofile");
        }
        return FPSMaster.i18n.get("mainmenu.account.ms.failed").replace("%s", message);
    }

    private void sleepInterruptibly(long millis) {
        long waited = 0L;
        while (waited < millis && !msLoginCancel.get()) {
            try {
                Thread.sleep(200L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            waited += 200L;
        }
    }

    private void openLink(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url.trim()));
            }
        } catch (Exception exception) {
            ClientLogger.warn("Failed to open Microsoft login URL: " + exception.getMessage());
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 && (accountOverlay.popOpen() || accountOverlay.blocking())) {
            cancelMicrosoftLogin();
            accountOverlay.close();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    // ------------------------------------------------------------------
    // Player skin loading (async, unchanged behaviour)
    // ------------------------------------------------------------------

    public static void preloadPlayerSkinTexture() {
        if (OSUtil.isAndroid()) {
            playerSkinLoadFailed = true;
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getSession() == null) {
            return;
        }
        String playerId = minecraft.getSession().getPlayerID();
        if (playerId == null || playerId.trim().isEmpty()) {
            return;
        }
        String normalizedPlayerId = playerId.replace("-", "");
        if (normalizedPlayerId.equals(loadedSkinPlayerId) && (playerSkinTexture != null || playerSkinLoadFailed || SKIN_LOADING.get())) {
            return;
        }
        if (!SKIN_LOADING.compareAndSet(false, true)) {
            return;
        }
        loadedSkinPlayerId = normalizedPlayerId;
        playerSkinTexture = null;
        playerSkinLoadFailed = false;
        try {
            FPSMaster.async.runnable(() -> loadPlayerSkinTexture(normalizedPlayerId));
        } catch (RuntimeException exception) {
            ClientLogger.warn("Failed to schedule main menu player skin loading");
            fallbackToDefaultSkin(normalizedPlayerId);
            SKIN_LOADING.set(false);
        }
    }

    private static void loadPlayerSkinTexture(String playerId) {
        boolean registrationQueued = false;
        try {
            String skinUrl = readSkinUrl(playerId);
            if (skinUrl == null || skinUrl.trim().isEmpty()) {
                fallbackToDefaultSkin(playerId);
                return;
            }
            BufferedImage skinImage = readImage(skinUrl);
            if (skinImage == null || skinImage.getWidth() < 64 || skinImage.getHeight() < 32) {
                fallbackToDefaultSkin(playerId);
                return;
            }
            BufferedImage textureImage = ensureArgbImage(skinImage);
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.getTextureManager() == null) {
                fallbackToDefaultSkin(playerId);
                return;
            }
            registrationQueued = true;
            try {
                minecraft.addScheduledTask(() -> registerPlayerSkinTexture(playerId, textureImage));
            } catch (RuntimeException exception) {
                registrationQueued = false;
                throw exception;
            }
        } catch (Exception exception) {
            ClientLogger.warn("Failed to load main menu player skin from Mojang API");
            fallbackToDefaultSkin(playerId);
        } finally {
            if (!registrationQueued) {
                SKIN_LOADING.set(false);
            }
        }
    }

    private static void registerPlayerSkinTexture(String playerId, BufferedImage textureImage) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.getTextureManager() == null || !playerId.equals(loadedSkinPlayerId)) {
                return;
            }
            playerSkinTexture = minecraft.getTextureManager()
                    .getDynamicTextureLocation("fpsmaster_player_skin_" + playerId, new DynamicTexture(textureImage));
            playerSkinLoadFailed = false;
        } catch (RuntimeException exception) {
            ClientLogger.warn("Failed to register main menu player skin texture");
            fallbackToDefaultSkin(playerId);
        } finally {
            SKIN_LOADING.set(false);
        }
    }

    private static void fallbackToDefaultSkin(String playerId) {
        if (playerId.equals(loadedSkinPlayerId)) {
            playerSkinTexture = null;
            playerSkinLoadFailed = true;
        }
    }

    private static BufferedImage ensureArgbImage(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private static String readSkinUrl(String playerId) throws Exception {
        JsonObject profile = readJson("https://sessionserver.mojang.com/session/minecraft/profile/" + playerId);
        JsonArray properties = profile.getAsJsonArray("properties");
        if (properties == null) {
            return "";
        }
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(property.get("name").getAsString()) || !property.has("value")) {
                continue;
            }
            String decoded = new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8);
            JsonObject decodedJson = GSON.fromJson(decoded, JsonObject.class);
            if (decodedJson == null || !decodedJson.has("textures") || !decodedJson.get("textures").isJsonObject()) {
                continue;
            }
            JsonObject textures = decodedJson.getAsJsonObject("textures");
            if (textures.has("SKIN") && textures.get("SKIN").isJsonObject()) {
                JsonObject skin = textures.getAsJsonObject("SKIN");
                if (skin.has("url") && !skin.get("url").isJsonNull()) {
                    return skin.get("url").getAsString();
                }
            }
        }
        return "";
    }

    private static JsonObject readJson(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(SKIN_REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(SKIN_REQUEST_TIMEOUT_MS);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static BufferedImage readImage(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(SKIN_REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(SKIN_REQUEST_TIMEOUT_MS);
        try (InputStream inputStream = connection.getInputStream()) {
            return ImageIO.read(inputStream);
        }
    }

    private final class EdgeMenuBridge implements MenuBridge {
        public String i18n(String key) {
            return FPSMaster.i18n.get(key);
        }

        public String edition() {
            return "EDGE";
        }

        public String version() {
            return FPSMaster.CLIENT_VERSION;
        }

        public String minecraftLabel() {
            return "Minecraft 1.8.9";
        }

        public String playerName() {
            return AccountManager.get().currentName();
        }

        public String accountTypeLabel() {
            return MainMenu.this.accountTypeLabel(AccountManager.get());
        }

        public ContinueServer continueServer() {
            if (continueServer == null) {
                return null;
            }
            String name = continueServer.serverName == null || continueServer.serverName.trim().isEmpty()
                    ? continueServer.serverIP : continueServer.serverName;
            return new ContinueServer(name, continueServer.serverIP, continueServer.pingToServer);
        }

        public boolean showReplays() {
            return true;
        }

        public boolean showDevtools() {
            return FPSMaster.isDevelopment();
        }

        public boolean interactive() {
            return !accountOverlay.blocking();
        }

        public boolean accountOpen() {
            return accountOverlay.popOpen();
        }

        public void account() {
            accountOverlay.togglePop();
        }

        public List<AccountRow> accounts() {
            AccountManager manager = AccountManager.get();
            List<AccountRow> rows = new ArrayList<AccountRow>();
            AccountManager.Account launcher = manager.launcherAccount();
            if (launcher != null) {
                rows.add(new AccountRow("launcher", launcher.name,
                        manager.isLauncherAccountOnline() ? i18n("mainmenu.account.ms") : i18n("mainmenu.account.offline"),
                        manager.isCurrentLauncherAccount(), false, manager.isLauncherAccountOnline()));
            }
            for (AccountManager.Account account : manager.getOfflineAccounts()) {
                if (launcher != null && account.name.equalsIgnoreCase(launcher.name) && !account.isMicrosoft()) continue;
                rows.add(new AccountRow(accountId(account), account.name,
                        i18n(account.isMicrosoft() ? "mainmenu.account.ms" : "mainmenu.account.offline"),
                        account.name.equalsIgnoreCase(manager.currentName()), true, account.isMicrosoft()));
            }
            return rows;
        }

        public void selectAccount(String id) {
            if ("launcher".equals(id)) AccountManager.get().useLauncherAccount();
            else {
                AccountManager.Account account = findAccount(id);
                if (account != null) AccountManager.get().use(account);
            }
        }

        public void removeAccount(String id) {
            AccountManager.Account account = findAccount(id);
            if (account != null) AccountManager.get().remove(account);
        }

        public boolean addOffline(String name) {
            return AccountManager.get().addAndUse(name);
        }

        public void startMicrosoftLogin() {
            MainMenu.this.startMicrosoftLogin();
        }

        public void openMicrosoftUrl() {
            openLink(msVerifyUrl);
        }

        public void copyMicrosoftCode() {
            if (msUserCode != null && !msUserCode.isEmpty()) GuiScreen.setClipboardString(msUserCode);
        }

        public void retryMicrosoftLogin() {
            MainMenu.this.startMicrosoftLogin();
        }

        public void cancelMicrosoftLogin() {
            MainMenu.this.cancelMicrosoftLogin();
        }

        public String microsoftCode() { return msUserCode == null ? "" : msUserCode; }
        public String microsoftStatus() { return msStatus == null ? "" : msStatus; }
        public String microsoftError() { return msError == null ? "" : msError; }
        public boolean microsoftBusy() { return msLoginBusy; }
        public boolean microsoftHasUrl() { return msVerifyUrl != null && !msVerifyUrl.isEmpty(); }

        public void drawAvatar(UiFrame ui, float x, float y, float size) {
            MainMenu.this.drawAvatar(x, y, size);
        }

        public void singleplayer() {
            mc.displayGuiScreen(new GuiSelectWorld(MainMenu.this));
        }

        public void multiplayer() {
            mc.displayGuiScreen(new GuiMultiplayer());
        }

        public void settings() {
            mc.displayGuiScreen(new GuiOptions(MainMenu.this, mc.gameSettings));
        }

        public void replays() {
            mc.displayGuiScreen(new ReplayScreen(MainMenu.this));
        }

        public void music() {
            mc.displayGuiScreen(new MusicScreen(MainMenu.this));
        }

        public void backgrounds() {
            mc.displayGuiScreen(new BackgroundSelector(MainMenu.this));
        }

        public void quit() {
            mc.shutdown();
        }

        public void continueConnect() {
            if (continueServer != null) {
                mc.displayGuiScreen(new GuiConnecting(MainMenu.this, mc, continueServer));
            }
        }

        public void devtools() {
            mc.displayGuiScreen(new DevToolsScreen(MainMenu.this));
        }

        private String accountId(AccountManager.Account account) {
            return (account.isMicrosoft() ? "ms:" : "offline:")
                    + (account.uuid == null || account.uuid.isEmpty() ? account.name : account.uuid);
        }

        private AccountManager.Account findAccount(String id) {
            for (AccountManager.Account account : AccountManager.get().getOfflineAccounts()) {
                if (accountId(account).equals(id)) return account;
            }
            return null;
        }
    }
}
