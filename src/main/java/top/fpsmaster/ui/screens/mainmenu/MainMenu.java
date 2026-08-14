package top.fpsmaster.ui.screens.mainmenu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.account.AccountManager;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.mc.GuiMultiplayer;
import top.fpsmaster.ui.screens.music.MusicScreen;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.BezierEasing;
import top.fpsmaster.utils.math.anim.Easings;
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
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final float TILE_H = 64f;
    private static final float TILE_W = 59f;
    private static final float TILE_GAP = 6f;
    private static final float CONTINUE_W = 125f;
    private static final float QUIT_W = 42f;

    private enum Dialog {
        NONE, ADD_OFFLINE
    }

    private boolean accountPopOpen;
    private Dialog dialog = Dialog.NONE;
    private TextField usernameField;
    private boolean usernameInvalid;

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

        float heroX = guiWidth * 0.06f;

        drawHero(heroX, intro);
        drawDock(heroX, mouseX, mouseY, intro);
        drawFooter(heroX);
        drawTopActions(mouseX, mouseY);
        drawAccountZone(mouseX, mouseY);

        if (firstBoot != 2) {
            FPSMaster.fontManager.s14.drawCenteredString(
                    FPSMaster.i18n.get(firstBoot == 0 ? "mainmenu.oldjava" : "mainmenu.javafail"),
                    guiWidth / 2f, 34f, ClickGuiTheme.danger().getRGB());
        }

        // Intro: world simply fades in from black; no lingering overlay art.
        if (intro < 1f) {
            Rects.fill(0, 0, guiWidth, guiHeight, new Color(10, 10, 10, (int) (255 * (1f - intro))));
        }

        if (dialog != Dialog.NONE) {
            drawAddAccountDialog(mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------
    // Hero + footer
    // ------------------------------------------------------------------

    private void drawHero(float heroX, float intro) {
        float rise = 8f * (1f - intro);
        float editionBaseline = guiHeight - 109f + rise;
        float logoY = editionBaseline - 12f - 26f;
        float greetY = logoY - 14f;

        String name = AccountManager.get().currentName();
        FPSMaster.fontManager.s14.drawString(greeting(name), heroX, greetY,
                ClickGuiTheme.textSecondary().getRGB());
        drawTracked(FPSMaster.fontManager.getFont(52), "FPSMASTER", heroX, logoY, 2.6f,
                ClickGuiTheme.textPrimary().getRGB(), true);
        float editionW = drawTracked(FPSMaster.fontManager.getFont(12), "EDGE", heroX,
                editionBaseline, 2.4f, ClickGuiTheme.accentText().getRGB(), false);
        // Gradient rule after the edition tag, fading out to the right.
        float lineX = heroX + editionW + 5f;
        for (int i = 0; i < 24; i++) {
            int alpha = (int) (255 * (1f - i / 24f));
            Rects.fill(lineX + i, editionBaseline + 3f, 1f, 0.5f,
                    new Color(89, 101, 241, alpha).getRGB());
        }
    }

    private String greeting(String name) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String key = hour < 6 || hour >= 19 ? "mainmenu.greet.evening"
                : hour < 12 ? "mainmenu.greet.morning" : "mainmenu.greet.afternoon";
        return FPSMaster.i18n.get(key).replace("%s", name);
    }

    /** Draws text with letter tracking (prototype letter-spacing); returns total width. */
    private float drawTracked(top.fpsmaster.font.impl.UFontRenderer font, String text,
                              float x, float y, float tracking, int color, boolean bold) {
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            font.drawString(ch, cursor, y, color);
            if (bold) {
                font.drawString(ch, cursor + 0.5f, y, color);
            }
            cursor += font.getStringWidth(ch) + tracking;
        }
        return cursor - x - tracking;
    }

    private void drawFooter(float heroX) {
        int color = ClickGuiTheme.textDisabled().getRGB();
        top.fpsmaster.font.impl.UFontRenderer font = FPSMaster.fontManager.getFont(11);
        font.drawString("FPSMaster Edge " + FPSMaster.CLIENT_VERSION + " · Minecraft 1.8.9",
                heroX, guiHeight - 16f, color);
        String mojang = "Copyright Mojang AB. Do not distribute!";
        font.drawString(mojang, guiWidth - heroX - font.getStringWidth(mojang), guiHeight - 16f, color);
    }

    // ------------------------------------------------------------------
    // Dock
    // ------------------------------------------------------------------

    private void drawDock(float heroX, int mouseX, int mouseY, float intro) {
        float dockY = guiHeight - 32f - TILE_H + 6f * (1f - intro);
        int tileCount = FPSMaster.isDevelopment() ? 5 : 4;

        // Fit check, prototype flex-row style: drop the continue card first, then squeeze the
        // tiles, so a small window degrades instead of overlapping the quit tile.
        float available = guiWidth - heroX * 2f - QUIT_W - TILE_GAP;
        boolean showContinue = continueServer != null
                && CONTINUE_W + TILE_GAP + tileCount * (TILE_W + TILE_GAP) <= available + TILE_GAP;
        float tileW = TILE_W;
        float tilesSpan = (showContinue ? CONTINUE_W + TILE_GAP : 0f) + tileCount * (tileW + TILE_GAP) - TILE_GAP;
        if (tilesSpan > available) {
            tileW = Math.max(44f, (available - (showContinue ? CONTINUE_W + TILE_GAP : 0f)) / tileCount - TILE_GAP);
        }

        float x = heroX;
        if (showContinue) {
            drawContinueTile(x, dockY, mouseX, mouseY);
            x += CONTINUE_W + TILE_GAP;
        }
        x += drawTile(x, dockY, tileW, "box", "mainmenu.single", mouseX, mouseY,
                () -> mc.displayGuiScreen(new GuiSelectWorld(this)));
        x += drawTile(x, dockY, tileW, "globe", "mainmenu.multi", mouseX, mouseY,
                () -> mc.displayGuiScreen(new GuiMultiplayer()));
        x += drawTile(x, dockY, tileW, "replay", "mainmenu.replays", mouseX, mouseY,
                () -> mc.displayGuiScreen(new ReplayScreen(this)));
        x += drawTile(x, dockY, tileW, "sliders", "mainmenu.settings", mouseX, mouseY,
                () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)));
        if (FPSMaster.isDevelopment()) {
            x += drawTile(x, dockY, tileW, "wrench", "mainmenu.devtools", mouseX, mouseY,
                    () -> mc.displayGuiScreen(new DevToolsScreen(this)));
        }
        drawQuitTile(guiWidth - heroX - QUIT_W, dockY, mouseX, mouseY);
    }

    private void tileSurface(float x, float y, float width, boolean hovered, boolean danger) {
        float ty = hovered ? y - 1.5f : y;
        Rects.rounded(x - 0.5f, ty - 0.5f, width + 1f, TILE_H + 1f, UiChrome.PANEL_RADIUS + 1,
                (hovered ? ClickGuiTheme.strokeStrong() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(x, ty, width, TILE_H, UiChrome.PANEL_RADIUS,
                (hovered ? new Color(28, 28, 28, 230) : ClickGuiTheme.glass()).getRGB(), false);
        if (hovered) {
            float markTop = ty + TILE_H * 0.14f;
            Rects.rounded(x, markTop, 1.5f, TILE_H * 0.72f, 1,
                    (danger ? ClickGuiTheme.danger() : ClickGuiTheme.accent()).getRGB(), false);
        }
    }

    private float drawTile(float x, float y, float width, String icon, String labelKey,
                           int mouseX, int mouseY, Runnable action) {
        boolean hovered = Hover.is(x, y, width, TILE_H, mouseX, mouseY);
        tileSurface(x, y, width, hovered, false);
        float ty = hovered ? y - 1.5f : y;
        int iconColor = (hovered ? ClickGuiTheme.accentText() : ClickGuiTheme.textSecondary()).getRGB();
        Icons.draw(icon, x + 8f, ty + TILE_H - 31f, 11f, iconColor);
        FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get(labelKey), x + 8f, ty + TILE_H - 15f,
                ClickGuiTheme.textPrimary().getRGB());
        if (consumePressInBounds(x, y, width, TILE_H, 0) != null) {
            action.run();
        }
        return width + TILE_GAP;
    }

    private void drawContinueTile(float x, float y, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, CONTINUE_W, TILE_H, mouseX, mouseY);
        tileSurface(x, y, CONTINUE_W, hovered, false);
        float ty = hovered ? y - 1.5f : y;
        drawTracked(FPSMaster.fontManager.getFont(10), FPSMaster.i18n.get("mainmenu.continue"),
                x + 8f, ty + 8f, 0.7f, ClickGuiTheme.accentText().getRGB(), false);
        Icons.draw("play", x + CONTINUE_W - 15f, ty + 7f, 8f,
                (hovered ? ClickGuiTheme.accentText() : ClickGuiTheme.textSecondary()).getRGB());

        String serverName = continueServer.serverName == null || continueServer.serverName.trim().isEmpty()
                ? continueServer.serverIP : continueServer.serverName;
        UiChrome.boldString(FPSMaster.fontManager.s18, serverName, x + 8f, ty + TILE_H - 28f,
                ClickGuiTheme.textPrimary().getRGB());
        top.fpsmaster.font.impl.UFontRenderer meta = FPSMaster.fontManager.getFont(11);
        float metaY = ty + TILE_H - 13f;
        meta.drawString(continueServer.serverIP, x + 8f, metaY, ClickGuiTheme.textSecondary().getRGB());
        long ping = continueServer.pingToServer;
        if (ping > 0) {
            float ipW = meta.getStringWidth(continueServer.serverIP);
            UiChrome.pingBars(x + 8f + ipW + 5f, metaY + 5f, UiChrome.pingLevel(ping), UiChrome.pingColor(ping));
            meta.drawString(ping + "ms", x + 8f + ipW + 5f + 10f + 4f, metaY, ClickGuiTheme.textSecondary().getRGB());
        }
        if (consumePressInBounds(x, y, CONTINUE_W, TILE_H, 0) != null) {
            mc.displayGuiScreen(new GuiConnecting(this, mc, continueServer));
        }
    }

    private void drawQuitTile(float x, float y, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, QUIT_W, TILE_H, mouseX, mouseY);
        tileSurface(x, y, QUIT_W, hovered, true);
        float ty = hovered ? y - 1.5f : y;
        int iconColor = (hovered ? ClickGuiTheme.danger() : ClickGuiTheme.textDisabled()).getRGB();
        Icons.draw("power", x + QUIT_W / 2f - 5.5f, ty + 18f, 11f, iconColor);
        FPSMaster.fontManager.getFont(12).drawCenteredString(FPSMaster.i18n.get("mainmenu.quit"),
                x + QUIT_W / 2f, ty + 38f, ClickGuiTheme.textSecondary().getRGB());
        if (consumePressInBounds(x, y, QUIT_W, TILE_H, 0) != null) {
            mc.shutdown();
        }
    }

    // ------------------------------------------------------------------
    // Top actions (background / music)
    // ------------------------------------------------------------------

    private void drawTopActions(int mouseX, int mouseY) {
        float size = 19f;
        float y = 11f;
        float musicX = guiWidth - 12f - size;
        float bgX = musicX - size - 4f;

        boolean bgHover = Hover.is(bgX, y, size, size, mouseX, mouseY);
        UiChrome.pillIconButton(bgX, y, size, bgHover);
        Icons.draw("image", bgX + 5.5f, y + 5.5f, 8f,
                (bgHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        boolean musicHover = Hover.is(musicX, y, size, size, mouseX, mouseY);
        UiChrome.pillIconButton(musicX, y, size, musicHover);
        Icons.draw("music", musicX + 5.5f, y + 5.5f, 8f,
                (musicHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());

        if (consumePressInBounds(bgX, y, size, size, 0) != null) {
            mc.displayGuiScreen(new BackgroundSelector());
        }
        if (consumePressInBounds(musicX, y, size, size, 0) != null) {
            mc.displayGuiScreen(new MusicScreen());
        }
    }

    // ------------------------------------------------------------------
    // Account zone
    // ------------------------------------------------------------------

    private void drawAccountZone(int mouseX, int mouseY) {
        AccountManager accounts = AccountManager.get();
        String username = accounts.currentName();
        preloadPlayerSkinTexture();

        float chipX = 12f;
        float chipY = 11f;
        float chipH = 22f;
        float nameW = Math.max(FPSMaster.fontManager.s14.getStringWidth(username),
                FPSMaster.fontManager.getFont(11).getStringWidth(accountTypeLabel(accounts)));
        float chipW = 4f + 14f + 5f + nameW + 4f + 6.5f + 7f;

        boolean chipHover = Hover.is(chipX, chipY, chipW, chipH, mouseX, mouseY);
        Rects.rounded(chipX - 0.5f, chipY - 0.5f, chipW + 1f, chipH + 1f, (int) (chipH / 2f) + 1,
                (chipHover || accountPopOpen ? ClickGuiTheme.strokeStrong() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(chipX, chipY, chipW, chipH, (int) (chipH / 2f), ClickGuiTheme.glass().getRGB(), false);
        drawAvatar(chipX + 4f, chipY + 4f, 14f);
        FPSMaster.fontManager.s14.drawString(username, chipX + 23f, chipY + 3.5f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(11).drawString(accountTypeLabel(accounts), chipX + 23f, chipY + 12f,
                ClickGuiTheme.textSecondary().getRGB());
        Icons.draw("chev-d", chipX + chipW - 12f, chipY + chipH / 2f - 3.25f, 6.5f,
                ClickGuiTheme.textDisabled().getRGB());

        if (accountPopOpen) {
            float popH = drawAccountPop(chipX, chipY + chipH + 4f, mouseX, mouseY);
            // Click-away dismiss: anything outside the chip+popover column closes the popover
            // (and is consumed, so the first click never also activates what's beneath it).
            float zoneW = Math.max(chipW, 124f) + 4f;
            if (consumePressOutside(chipX - 2f, chipY - 2f, zoneW, chipH + 6f + popH + 4f) != null) {
                accountPopOpen = false;
            }
        }
        if (consumePressInBounds(chipX, chipY, chipW, chipH, 0) != null) {
            accountPopOpen = !accountPopOpen;
        }
    }

    private String accountTypeLabel(AccountManager accounts) {
        boolean current = accounts.isCurrentLauncherAccount();
        if (current && accounts.isLauncherAccountOnline()) {
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

        float w = 124f;
        float rowH = 22f;
        int rows = (launcher != null ? 1 : 0) + offline.size();
        float h = 3f + rows * rowH + 4.5f + rowH + 3f;
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
            if (launcher != null && account.name.equalsIgnoreCase(launcher.name)) {
                continue;
            }
            drawAccountRow(x + 3f, rowY, w - 6f, rowH, account.name,
                    FPSMaster.i18n.get("mainmenu.account.offline"),
                    account.name.equals(accounts.currentName()), account, mouseX, mouseY);
            rowY += rowH;
        }

        UiChrome.hairlineH(x + 7f, rowY + 2f, w - 14f);
        rowY += 4.5f;

        boolean addHover = Hover.is(x + 3f, rowY, w - 6f, rowH, mouseX, mouseY);
        if (addHover) {
            Rects.rounded(x + 3f, rowY, w - 6f, rowH, CARD_ROW_RADIUS, ClickGuiTheme.layerHover().getRGB(), false);
        }
        // dashed plus box
        float boxX = x + 8f;
        float boxY = rowY + 4f;
        Rects.rounded(boxX - 0.5f, boxY - 0.5f, 15f, 15f, 5, ClickGuiTheme.strokeStrong().getRGB(), false);
        Rects.rounded(boxX, boxY, 14f, 14f, 4, ClickGuiTheme.glass().getRGB(), false);
        Icons.draw("plus", boxX + 3.5f, boxY + 3.5f, 7f,
                (addHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        FPSMaster.fontManager.getFont(13).drawString(FPSMaster.i18n.get("mainmenu.account.add"),
                x + 27f, rowY + 7f,
                (addHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(x + 3f, rowY, w - 6f, rowH, 0) != null) {
            dialog = Dialog.ADD_OFFLINE;
            usernameField.setText("");
            usernameField.setFocused(true);
            usernameInvalid = false;
            accountPopOpen = false;
        }
        return h;
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
        dialog = Dialog.NONE;
        usernameField.setFocused(false);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
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
}
