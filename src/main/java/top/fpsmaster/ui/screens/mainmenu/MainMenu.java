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
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.UserInfo;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.ui.mc.GuiMultiplayer;
import top.fpsmaster.ui.screens.music.MusicScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.ui.screens.signin.SignInScreen;
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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

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
    /** 当前这次浏览器登录持有的回调监听器；取消时要把它关掉，否则端口一直占着。 */
    private volatile MicrosoftAuth.BrowserSession msSession;

    /** Last server in the list, pinged once per menu session for the continue card. */
    private static ServerData continueServer;
    private static boolean continueServerLoaded;

    @Override
    public void initGui() {
        super.initGui();
        Backgrounds.initGui();
        animClock.reset();
        // token 还有效但缓存为空时，账号浮层会显示「未知账号」。这里异步补一次，
        // 浮层每帧只读缓存（绝不能在 paint 里发请求，那是每帧一次 15 秒超时）。
        FPSMasterApiClient.getInstance().refreshUserInfoAsync();
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

    /** 串行化「关旧 socket → 绑新端口 → 发布会话」，见 {@link #runMicrosoftLogin}。 */
    private final Object msSessionLock = new Object();

    private void startMicrosoftLogin() {
        // 代际号必须在任何状态写入之前推进：旧 worker 的 catch/finally 用它判断
        // 「我还是当前那一代吗」，先写 msLoginBusy 再推进的话，旧 worker 会在这个
        // 窗口里看到代际相等，把新一轮刚设的状态清掉。
        final int generation = msLoginGeneration.incrementAndGet();
        msLoginCancel.set(false);
        msLoginBusy = true;
        msUserCode = "";
        msVerifyUrl = "";
        msStatus = FPSMaster.i18n.get("mainmenu.account.ms.starting");
        msError = "";
        closeMicrosoftSession();
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
        // ServerSocket 有 200ms 超时，光靠标志位 await 也会醒；关端口是为了立刻释放它，
        // 以及打断已经 accept 到、正卡在 readLine() 上的那条连接（见 MicrosoftAuth.BrowserSession）。
        closeMicrosoftSession();
    }

    private void closeMicrosoftSession() {
        synchronized (msSessionLock) {
            closeMicrosoftSessionLocked();
        }
    }

    private void closeMicrosoftSessionLocked() {
        MicrosoftAuth.BrowserSession session = msSession;
        msSession = null;
        if (session != null) {
            session.close();
        }
    }

    /**
     * 授权码 + PKCE 的浏览器登录，和 Nova 与启动器同一条路径。
     *
     * <p>这里不再用设备码：Azure 应用没有注册成公共/移动客户端，设备码端点会直接回
     * {@code AADSTS70002}，玩家永远拿不到那串 8 位验证码。浏览器流程里没有「验证码」这个东西，
     * 所以 {@code msUserCode} 保持为空，共享浮层会自动落到「已在浏览器打开」那条分支。
     */
    private void runMicrosoftLogin(int generation) {
        try {
            final String authorizeUrl;
            final MicrosoftAuth.BrowserSession browser;
            // 「关掉上一轮的 socket + 绑本轮的端口 + 发布状态」必须是一段不可分割的操作。
            // 分开做的话，连点两次登录时旧 worker 可能还占着 127.0.0.1:3389 却尚未把
            // session 发布出来，startMicrosoftLogin 的 close 扑了个空、新 worker 的 bind
            // 就撞上去：Linux/macOS 报「端口被占用」这条完全误导的提示，Windows 的
            // SO_REUSEADDR 更糟——两个 listener 同时活着，回调可能落到已取消的那一个。
            // 发布状态同样要在锁里：check 完再裸写就是 TOCTOU，过期 worker 会把
            // verifyUrl 覆盖成自己那条已死会话的地址，玩家点开浏览器后永远等不到回调。
            synchronized (msSessionLock) {
                if (msLoginCancel.get() || generation != msLoginGeneration.get()) {
                    return;
                }
                closeMicrosoftSessionLocked();
                browser = MicrosoftAuth.beginBrowserLogin();
                if (msLoginCancel.get() || generation != msLoginGeneration.get()) {
                    browser.close();
                    return;
                }
                msSession = browser;
                authorizeUrl = browser.authorizeUrl;
                msVerifyUrl = authorizeUrl;
                msStatus = FPSMaster.i18n.get("mainmenu.account.ms.waiting");
                msLoginBusy = false;
            }
            // 出锁到这里之间玩家可能已经取消或又点了一次登录：不重查一遍就会平白弹出一个
            // 属于上一轮的授权页，玩家在里面登完也没人收回调。
            if (msLoginCancel.get() || generation != msLoginGeneration.get()) {
                return;
            }
            // 打开浏览器是同步的、能耗上百毫秒，不能占着锁。用局部量而不是读字段，
            // 免得被后一轮改掉之后打开的是别人的授权地址。
            openLink(authorizeUrl);
            final MicrosoftAuth.MinecraftProfile profile = browser.await(new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    return msLoginCancel.get() || generation != msLoginGeneration.get();
                }
            });
            if (msLoginCancel.get() || generation != msLoginGeneration.get()) {
                return;
            }
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    AccountManager.get().addAndUseMicrosoft(profile);
                    accountOverlay.closeDialog();
                }
            });
        } catch (Exception exception) {
            // 收 Exception 而不是 IOException | AccountException：beginBrowserLogin /
            // await 里任何一个 RuntimeException（URI 非法、JSON 结构不对）漏出去，就是
            // 一条死在后台线程里的登录——界面永远停在「等待授权」，而玩家什么提示都没有。
            // 代际也要看：连点两次登录时，startMicrosoftLogin 里的 closeMicrosoftSession()
            // 会把旧 socket 关掉、旧 worker 立刻抛 "Socket closed"，而 msLoginCancel 此刻
            // 已被新一轮置回 false。不判代际就会把裸英文写进 msError、并清掉新一轮的状态行。
            if (msLoginCancel.get() || generation != msLoginGeneration.get()
                    || "cancelled".equals(exception.getMessage())) {
                return;
            }
            msError = localizeMicrosoftError(exception.getMessage());
            msStatus = "";
            ClientLogger.warn("Microsoft login failed: " + exception.getMessage());
        } finally {
            // 只清自己那一代的状态：旧 worker 收尾时如果无条件清空，会把新 worker
            // 刚写进 msSession 的 socket 抹掉，导致后续 cancel 关不到它、下一次 bind 撞端口。
            //
            // 判代和清理必须在同一把锁里：在锁外判完再清，中间插进来的 startMicrosoftLogin
            // 已经推进了代际、并在上面那个同步块里发布了新 socket，这一句照样会把它抹掉
            // ——正是这段注释要防的场景。顺带用 closeMicrosoftSessionLocked() 而不是裸置空，
            // 免得正常走完的那轮把 127.0.0.1 上的监听端口一直占到退出游戏。
            synchronized (msSessionLock) {
                if (generation == msLoginGeneration.get()) {
                    msLoginBusy = false;
                    closeMicrosoftSessionLocked();
                }
            }
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
        if (message.startsWith("PORT_IN_USE:")) {
            // 端口被占用是玩家自己能处理的（关掉远程桌面 / 设 FPSMASTER_MINECRAFT_REDIRECT_URL），
            // 所以这条必须完整可读，不能被下面的 96 字截断吃掉。
            return FPSMaster.i18n.get("mainmenu.account.ms.portbusy")
                    .replace("%s", message.substring("PORT_IN_USE:".length()));
        }
        if (message.toLowerCase(Locale.ROOT).contains("access_denied")) {
            return FPSMaster.i18n.get("mainmenu.account.ms.denied");
        }
        // AADSTS 的原文后面挂着 Trace ID / Correlation ID / 时间戳，浮层一行放不下也没意义。
        String shortened = message;
        int trace = shortened.indexOf(". Trace");
        if (trace < 0) {
            trace = shortened.indexOf(" Trace ID");
        }
        if (trace > 0) {
            shortened = shortened.substring(0, trace);
        }
        if (shortened.length() > 96) {
            shortened = shortened.substring(0, 96) + "\u2026";
        }
        return FPSMaster.i18n.get("mainmenu.account.ms.failed").replace("%s", shortened);
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
        releasePlayerSkinTexture();
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
            releasePlayerSkinTexture();
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

    /**
     * Client thread only. Each load registers a fresh {@code dynamic/fpsmaster_player_skin_*}, so
     * switching accounts without this leaves the previous head upload in the texture manager.
     */
    private static void releasePlayerSkinTexture() {
        ResourceLocation previous = playerSkinTexture;
        playerSkinTexture = null;
        if (previous == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.getTextureManager() != null) {
            minecraft.getTextureManager().deleteTexture(previous);
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

        /**
         * FPSMaster 产品账号那一行。Minecraft 账号在上面的列表里，这是两套账号体系。
         */
        public boolean showFpsAccount() { return true; }

        public boolean fpsSignedIn() { return AuthService.getInstance().isLoggedIn(); }

        public String fpsAccountName() {
            UserInfo user = FPSMasterApiClient.getInstance().cachedUser();
            if (user == null) {
                return "";
            }
            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                return user.getUsername();
            }
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                return user.getDisplayName();
            }
            return user.getEmail() == null ? "" : user.getEmail();
        }

        public void openFpsSignIn() {
            Minecraft.getMinecraft().displayGuiScreen(new SignInScreen(MainMenu.this));
        }

        public void drawAvatar(UiFrame ui, float x, float y, float size) {
            MainMenu.this.drawAvatar(x, y, size);
        }

        public void singleplayer() {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelectWorld(MainMenu.this));
        }

        public void multiplayer() {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMultiplayer());
        }

        public void settings() {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.displayGuiScreen(new GuiOptions(MainMenu.this, minecraft.gameSettings));
        }

        public void replays() {
            Minecraft.getMinecraft().displayGuiScreen(new ReplayScreen(MainMenu.this));
        }

        public void music() {
            Minecraft.getMinecraft().displayGuiScreen(new MusicScreen(MainMenu.this));
        }

        public void backgrounds() {
            Minecraft.getMinecraft().displayGuiScreen(new BackgroundSelector(MainMenu.this));
        }

        public void quit() {
            Minecraft.getMinecraft().shutdown();
        }

        public void continueConnect() {
            if (continueServer != null) {
                Minecraft minecraft = Minecraft.getMinecraft();
                minecraft.displayGuiScreen(new GuiConnecting(MainMenu.this, minecraft, continueServer));
            }
        }

        public void devtools() {
            Minecraft.getMinecraft().displayGuiScreen(new DevToolsScreen(MainMenu.this));
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
