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
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.ui.mc.GuiMultiplayer;
import top.fpsmaster.ui.screens.music.MusicScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.uikit.screen.MenuBridge;
import top.fpsmaster.uikit.screen.SharedMainMenu;
import top.fpsmaster.uikit.widget.UiFrame;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.BezierEasing;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
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

    private enum Dialog {
        NONE, ADD_OFFLINE, MS_LOGIN
    }

    private boolean accountPopOpen;
    private Dialog dialog = Dialog.NONE;
    private TextField usernameField;
    private boolean usernameInvalid;
    private final AtomicBoolean msLoginCancel = new AtomicBoolean(false);
    private final AtomicInteger msLoginGeneration = new AtomicInteger();
    private volatile boolean msLoginBusy;
    private volatile String msUserCode = "";
    private volatile String msVerifyUrl = "";
    private volatile String msStatus = "";
    private volatile String msError = "";
    private volatile boolean msCopied;
    private long msCopiedUntil;

    /** Last server in the list, pinged once per menu session for the continue card. */
    private static ServerData continueServer;
    private static boolean continueServerLoaded;

    @Override
    public void initGui() {
        super.initGui();
        Backgrounds.initGui();
        animClock.reset();
        if (usernameField == null) {
            usernameField = new TextField(FPSMaster.fontManager.s14, false,
                    FPSMaster.i18n.get("mainmenu.account.username"), 0,
                    ClickGuiTheme.textPrimary().getRGB(), 16);
        }
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
        drawAccountPopover(mouseX, mouseY);

        if (firstBoot != 2) {
            FPSMaster.fontManager.s14.drawCenteredString(
                    FPSMaster.i18n.get(firstBoot == 0 ? "mainmenu.oldjava" : "mainmenu.javafail"),
                    guiWidth / 2f, 34f, ClickGuiTheme.danger().getRGB());
        }

        // Intro: world simply fades in from black; no lingering overlay art.
        if (intro < 1f) {
            Rects.fill(0, 0, guiWidth, guiHeight, new Color(10, 10, 10, (int) (255 * (1f - intro))));
        }

        if (dialog == Dialog.ADD_OFFLINE) {
            drawAddAccountDialog(mouseX, mouseY);
        } else if (dialog == Dialog.MS_LOGIN) {
            drawMicrosoftLoginDialog(mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------
    // Account popover (Edge-only overlay on the shared chip)
    // ------------------------------------------------------------------

    private void drawAccountPopover(int mouseX, int mouseY) {
        if (!accountPopOpen) {
            return;
        }
        UiFrame ui = EdgeUi.frame();
        float chipX = SharedMainMenu.CHIP_X;
        float chipY = SharedMainMenu.CHIP_Y;
        float chipH = SharedMainMenu.CHIP_H;
        float chipW = SharedMainMenu.chipWidth(ui, menuBridge);
        float popH = drawAccountPop(chipX, chipY + chipH + 4f, mouseX, mouseY);
        float zoneW = Math.max(chipW, 148f) + 4f;
        if (consumePressOutside(chipX - 2f, chipY - 2f, zoneW, chipH + 6f + popH + 4f) != null) {
            accountPopOpen = false;
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

    private float drawAccountPop(float x, float y, int mouseX, int mouseY) {
        AccountManager accounts = AccountManager.get();
        AccountManager.Account launcher = accounts.launcherAccount();
        List<AccountManager.Account> offline = accounts.getOfflineAccounts();

        float w = 148f;
        float rowH = 22f;
        int rows = (launcher != null ? 1 : 0) + offline.size();
        float h = 3f + rows * rowH + 4.5f + rowH * 2f + 3f;
        UiChrome.panel(x, y, w, h);

        float rowY = y + 3f;
        if (launcher != null) {
            drawAccountRow(x + 3f, rowY, w - 6f, rowH, launcher.name,
                    accounts.isLauncherAccountOnline()
                            ? FPSMaster.i18n.get("mainmenu.account.ms")
                            : FPSMaster.i18n.get("mainmenu.account.offline"),
                    accounts.isCurrentLauncherAccount(), null, mouseX, mouseY);
            rowY += rowH;
        }
        for (AccountManager.Account account : offline) {
            if (launcher != null && account.name.equalsIgnoreCase(launcher.name) && !account.isMicrosoft()) {
                continue;
            }
            drawAccountRow(x + 3f, rowY, w - 6f, rowH, account.name,
                    account.isMicrosoft()
                            ? FPSMaster.i18n.get("mainmenu.account.ms")
                            : FPSMaster.i18n.get("mainmenu.account.offline"),
                    account.name.equals(accounts.currentName()), account, mouseX, mouseY);
            rowY += rowH;
        }

        UiChrome.hairlineH(x + 7f, rowY + 2f, w - 14f);
        rowY += 4.5f;
        if (drawAddAccountAction(x, rowY, w, rowH, FPSMaster.i18n.get("mainmenu.account.ms.add"), mouseX, mouseY)) {
            accountPopOpen = false;
            startMicrosoftLogin();
        }
        rowY += rowH;
        if (drawAddAccountAction(x, rowY, w, rowH, FPSMaster.i18n.get("mainmenu.account.offline.add"), mouseX, mouseY)) {
            dialog = Dialog.ADD_OFFLINE;
            usernameField.setText("");
            usernameField.setFocused(true);
            usernameInvalid = false;
            accountPopOpen = false;
        }
        return h;
    }

    private boolean drawAddAccountAction(float x, float rowY, float w, float rowH, String label, int mouseX, int mouseY) {
        boolean addHover = Hover.is(x + 3f, rowY, w - 6f, rowH, mouseX, mouseY);
        if (addHover) {
            Rects.rounded(x + 3f, rowY, w - 6f, rowH, CARD_ROW_RADIUS, ClickGuiTheme.layerHover().getRGB(), false);
        }
        float boxX = x + 8f;
        float boxY = rowY + 4f;
        Rects.rounded(boxX - 0.5f, boxY - 0.5f, 15f, 15f, 5, ClickGuiTheme.strokeStrong().getRGB(), false);
        Rects.rounded(boxX, boxY, 14f, 14f, 4, ClickGuiTheme.glass().getRGB(), false);
        Icons.draw("plus", boxX + 3.5f, boxY + 3.5f, 7f,
                (addHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        FPSMaster.fontManager.getFont(13).drawString(label, x + 27f, rowY + 7f,
                (addHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        return consumePressInBounds(x + 3f, rowY, w - 6f, rowH, 0) != null;
    }

    private static final int CARD_ROW_RADIUS = 5;

    private void drawAccountRow(float x, float y, float w, float h, String name, String type,
                                boolean current, AccountManager.Account removable, int mouseX, int mouseY) {
        boolean hover = Hover.is(x, y, w, h, mouseX, mouseY);
        if (hover) {
            Rects.rounded(x, y, w, h, CARD_ROW_RADIUS, ClickGuiTheme.layerHover().getRGB(), false);
        }
        drawAvatarPlaceholder(x + 5f, y + 4f, 14f, name);
        FPSMaster.fontManager.getFont(13).drawString(name, x + 24f, y + 3f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(10).drawString(type, x + 24f, y + 12f,
                ClickGuiTheme.textDisabled().getRGB());

        float rightX = x + w - 16f;
        boolean removeHover = false;
        if (hover && removable != null) {
            removeHover = Hover.is(rightX, y + 5f, 12f, 12f, mouseX, mouseY);
            if (removeHover) {
                Rects.rounded(rightX - 1f, y + 4f, 14f, 14f, 4, ClickGuiTheme.dangerSoft().getRGB(), false);
            }
            Icons.draw("delete", rightX + 0.5f, y + 6.5f, 9f,
                    (removeHover ? ClickGuiTheme.danger() : ClickGuiTheme.textDisabled()).getRGB());
        } else if (current) {
            Icons.draw("check", rightX, y + 7f, 8f, ClickGuiTheme.accentText().getRGB());
        }

        if (removable != null && removeHover
                && consumePressInBounds(rightX, y + 5f, 12f, 12f, 0) != null) {
            AccountManager.get().remove(removable);
            return;
        }
        if (consumePressInBounds(x, y, w, h, 0) != null) {
            if (removable != null) {
                AccountManager.get().use(removable);
            } else {
                AccountManager.get().useLauncherAccount();
            }
            accountPopOpen = false;
        }
    }

    /** Skin head for the active account; deterministic tinted square for everyone else. */
    private void drawAvatarPlaceholder(float x, float y, float size, String name) {
        if (name.equals(AccountManager.get().currentName())) {
            drawAvatar(x, y, size);
            return;
        }
        int hue = Math.abs(name.hashCode()) % 360;
        Color color = Color.getHSBColor(hue / 360f, 0.45f, 0.75f);
        Rects.rounded(x, y, size, size, 4, color.getRGB(), false);
        FPSMaster.fontManager.getFont(11).drawCenteredString(
                name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(),
                x + size / 2f, y + size / 2f - 2.5f, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------
    // Add-account dialog (offline)
    // ------------------------------------------------------------------

    private void drawAddAccountDialog(int mouseX, int mouseY) {
        UiChrome.veil(guiWidth, guiHeight, 0.9f);
        float w = 190f;
        float h = 108f;
        float x = (guiWidth - w) / 2f;
        float y = (guiHeight - h) / 2f;
        UiChrome.panel(x, y, w, h);

        UiChrome.boldString(FPSMaster.fontManager.s16, FPSMaster.i18n.get("mainmenu.account.offline.title"),
                x + 13f, y + 12f, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(12).drawString(FPSMaster.i18n.get("mainmenu.account.offline.desc"),
                x + 13f, y + 24f, ClickGuiTheme.textSecondary().getRGB());

        float fieldY = y + 40f;
        UiChrome.inputBox(x + 13f, fieldY, w - 26f, 20f, true);
        usernameField.backGroundColor = 0;
        usernameField.fontColor = ClickGuiTheme.textPrimary().getRGB();
        usernameField.drawTextBox(x + 19f, fieldY + 1f, w - 38f, 18f);
        if (usernameInvalid) {
            FPSMaster.fontManager.getFont(11).drawString(FPSMaster.i18n.get("mainmenu.account.invalid"),
                    x + 13f, fieldY + 24f, ClickGuiTheme.danger().getRGB());
        }

        float btnY = y + h - 28f;
        float addW = 62f;
        float cancelW = 40f;
        if (UiChrome.buttonClicked(this, x + w - 13f - addW, btnY, addW, UiChrome.BTN_H, null,
                FPSMaster.i18n.get("mainmenu.account.add"), UiChrome.Style.PRIMARY, mouseX, mouseY)) {
            submitOfflineAccount();
        }
        if (UiChrome.buttonClicked(this, x + w - 13f - addW - 6f - cancelW, btnY, cancelW, UiChrome.BTN_H, null,
                FPSMaster.i18n.get("common.cancel"), UiChrome.Style.GHOST, mouseX, mouseY)) {
            closeDialog();
        }

        ScaledGuiScreen.PointerEvent press = consumePressInBounds(x + 13f, fieldY, w - 26f, 20f, 0);
        if (press != null) {
            usernameField.setFocused(true);
            usernameField.mouseClicked(press.x, press.y, 0);
        }
        if (consumePressOutside(x, y, w, h) != null) {
            closeDialog();
        }
    }

    private void submitOfflineAccount() {
        String name = usernameField.getText().trim();
        if (AccountManager.get().addAndUse(name)) {
            closeDialog();
        } else {
            usernameInvalid = true;
        }
    }

    private void closeDialog() {
        if (dialog == Dialog.MS_LOGIN) {
            msLoginCancel.set(true);
            msLoginGeneration.incrementAndGet();
        }
        dialog = Dialog.NONE;
        usernameField.setFocused(false);
    }

    private void startMicrosoftLogin() {
        dialog = Dialog.MS_LOGIN;
        msLoginCancel.set(false);
        msLoginBusy = true;
        msUserCode = "";
        msVerifyUrl = "";
        msStatus = FPSMaster.i18n.get("mainmenu.account.ms.starting");
        msError = "";
        msCopied = false;
        final int generation = msLoginGeneration.incrementAndGet();
        FPSMaster.async.runnable(new Runnable() {
            @Override
            public void run() {
                runMicrosoftLogin(generation);
            }
        });
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
                            dialog = Dialog.NONE;
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

    private void drawMicrosoftLoginDialog(int mouseX, int mouseY) {
        UiChrome.veil(guiWidth, guiHeight, 0.9f);
        float w = 220f;
        float h = 132f;
        float x = (guiWidth - w) / 2f;
        float y = (guiHeight - h) / 2f;
        UiChrome.panel(x, y, w, h);

        UiChrome.boldString(FPSMaster.fontManager.s16, FPSMaster.i18n.get("mainmenu.account.ms.title"),
                x + 13f, y + 12f, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(12).drawString(FPSMaster.i18n.get("mainmenu.account.ms.desc"),
                x + 13f, y + 24f, ClickGuiTheme.textSecondary().getRGB());

        String code = msUserCode == null || msUserCode.isEmpty()
                ? (msLoginBusy ? "····" : "—")
                : msUserCode;
        float codeY = y + 42f;
        boolean codeHover = Hover.is(x + 13f, codeY, w - 26f, 22f, mouseX, mouseY);
        Rects.rounded(x + 13f, codeY, w - 26f, 22f, 5,
                (codeHover ? ClickGuiTheme.layerHover() : ClickGuiTheme.glass()).getRGB(), false);
        FPSMaster.fontManager.s16.drawCenteredString(code, x + w / 2f, codeY + 7f,
                ClickGuiTheme.textPrimary().getRGB());
        if (!msUserCode.isEmpty() && consumePressInBounds(x + 13f, codeY, w - 26f, 22f, 0) != null) {
            GuiScreen.setClipboardString(msUserCode);
            msCopied = true;
            msCopiedUntil = System.currentTimeMillis() + 1500L;
        }

        String status;
        if (msError != null && !msError.isEmpty()) {
            status = msError;
        } else if (msCopied && System.currentTimeMillis() < msCopiedUntil) {
            status = FPSMaster.i18n.get("mainmenu.account.ms.copied");
        } else {
            status = msStatus;
        }
        if (status != null && !status.isEmpty()) {
            int color = (msError != null && !msError.isEmpty())
                    ? ClickGuiTheme.danger().getRGB()
                    : ClickGuiTheme.textSecondary().getRGB();
            FPSMaster.fontManager.getFont(11).drawString(status, x + 13f, codeY + 26f, color);
        }

        float btnY = y + h - 28f;
        float cancelW = 40f;
        float actionW = 72f;
        if (UiChrome.buttonClicked(this, x + w - 13f - cancelW, btnY, cancelW, UiChrome.BTN_H, null,
                FPSMaster.i18n.get("common.cancel"), UiChrome.Style.GHOST, mouseX, mouseY)) {
            closeDialog();
        }
        if (msError != null && !msError.isEmpty()) {
            if (UiChrome.buttonClicked(this, x + w - 13f - cancelW - 6f - actionW, btnY, actionW, UiChrome.BTN_H, null,
                    FPSMaster.i18n.get("mainmenu.account.ms.retry"), UiChrome.Style.PRIMARY, mouseX, mouseY)) {
                startMicrosoftLogin();
            }
        } else if (msVerifyUrl != null && !msVerifyUrl.isEmpty()) {
            if (UiChrome.buttonClicked(this, x + w - 13f - cancelW - 6f - actionW, btnY, actionW, UiChrome.BTN_H, null,
                    FPSMaster.i18n.get("mainmenu.account.ms.open"), UiChrome.Style.PRIMARY, mouseX, mouseY)) {
                openLink(msVerifyUrl);
            }
        }

        if (consumePressOutside(x, y, w, h) != null) {
            closeDialog();
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
        if (dialog == Dialog.MS_LOGIN) {
            if (keyCode == 1) {
                closeDialog();
            }
            return;
        }
        if (dialog == Dialog.ADD_OFFLINE) {
            if (keyCode == 1) {
                closeDialog();
                return;
            }
            if (keyCode == 28) {
                submitOfflineAccount();
                return;
            }
            usernameField.textboxKeyTyped(typedChar, keyCode);
            usernameInvalid = false;
            return;
        }
        if (keyCode == 1 && accountPopOpen) {
            accountPopOpen = false;
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
            return dialog == Dialog.NONE;
        }

        public boolean accountOpen() {
            return accountPopOpen;
        }

        public void account() {
            accountPopOpen = !accountPopOpen;
        }

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
            mc.displayGuiScreen(new MusicScreen());
        }

        public void backgrounds() {
            mc.displayGuiScreen(new BackgroundSelector());
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
    }
}
