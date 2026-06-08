package top.fpsmaster.ui.screens.mainmenu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.mc.GuiMultiplayer;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainMenu extends ScaledGuiScreen {
    private static int firstBoot = 0;
    private static final Gson GSON = new Gson();
    private static final ResourceLocation DEFAULT_AVATAR = new ResourceLocation("client/gui/screen/avatar.png");
    private static final ResourceLocation DEFAULT_SKIN = new ResourceLocation("textures/entity/steve.png");
    private static final int SKIN_REQUEST_TIMEOUT_MS = 2500;
    private static final AtomicBoolean SKIN_LOADING = new AtomicBoolean(false);
    private static volatile ResourceLocation playerSkinTexture;
    private static volatile String loadedSkinPlayerId = "";
    private static volatile boolean playerSkinLoadFailed;

    // Buttons for the main menu
    private final MenuButton singlePlayer;
    private final MenuButton multiPlayer;
    private final MenuButton options;
    private final MenuButton exit;
    private final MenuButton devTools;
    private static final Animator startAnimation = new Animator();
    private static final Animator backgroundAnimation = new Animator();
    private final AnimClock animClock = new AnimClock();


    public MainMenu() {
        singlePlayer = new MenuButton("mainmenu.single", () -> mc.displayGuiScreen(new GuiSelectWorld(this)));
        multiPlayer = new MenuButton("mainmenu.multi", () -> mc.displayGuiScreen(new GuiMultiplayer()));
        options = new MenuButton("mainmenu.settings", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)));
        exit = new MenuButton("X", () -> mc.shutdown());
        devTools = new MenuButton("DevTools", () -> mc.displayGuiScreen(new DevToolsScreen(this)));
        devTools.setText("DevTools", false);
    }

    @Override
    public void initGui() {
        super.initGui();
        Backgrounds.initGui();
        animClock.reset();
        if (firstBoot == 0) {
            // Check Java Version
            String version = System.getProperty("java.version");
            String major = version.split("_")[0];
            String minor = version.split("_")[1];
            if (major.equals("1.8.0")) {
                try {
                    int minorVersion = Integer.parseInt(minor);
                    if (minorVersion >= 382) {
                        firstBoot = 2;
                    }
                } catch (NumberFormatException e) {
                    firstBoot = 1;
                }
            } else {
                firstBoot = 2;
            }
        }
//        if (!MusicPlayer.playList.getMusics().isEmpty()) {
//            if (MusicPlayer.isPlaying) {
//                MusicPlayer.playList.pause();
//            }
//        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);
        double dt = animClock.tick();
        if (!startAnimation.isRunning() && startAnimation.get() == 0.0) {
            startAnimation.start(0, 1.1, 1.5f, Easings.QUINT_OUT);
        }
        startAnimation.update(dt);
        if (startAnimation.get() >= 0.5) {
            if (!backgroundAnimation.isRunning() && backgroundAnimation.get() == 0.0) {
                backgroundAnimation.start(0, 1.5, 2.0f, Easings.LINEAR);
            }
            backgroundAnimation.update(dt);
        }


        // Display user info and avatar
        String username = mc.getSession().getUsername();
        preloadPlayerSkinTexture();
        float stringWidth = FPSMaster.fontManager.s16.getStringWidth(username);
        Rects.rounded(10, 10, Math.round(30 + stringWidth), 20, new Color(0, 0, 0, 60));
        if (playerSkinTexture != null) {
            Images.playerHead(playerSkinTexture, 14f, 15f, 10, 10);
        } else if (playerSkinLoadFailed) {
            Images.playerHead(DEFAULT_SKIN, 14f, 15f, 10, 10);
        } else {
            Images.draw(DEFAULT_AVATAR, 14f, 15f, 10f, 10f, -1);
        }
        FPSMaster.fontManager.s16.drawString(username, 28, 16, Color.WHITE.getRGB());


        // background selector button
        Rects.rounded(Math.round(guiWidth - 22), 13, 12, 12, new Color(0, 0, 0, 60));
        Images.draw(new ResourceLocation("client/gui/screen/theme.png"), guiWidth - 20, 15f, 8f, 8f, -1);


        // Position buttons and render them
        float x = guiWidth / 2f - 50;
        float y = guiHeight / 2f - 30;
        singlePlayer.renderInScreen(this, x, y, 100f, 20f, mouseX, mouseY);
        multiPlayer.renderInScreen(this, x, y + 24f, 100f, 20f, mouseX, mouseY);
        options.renderInScreen(this, x, y + 48f, 70f, 20f, mouseX, mouseY);
        exit.renderInScreen(this, x + 74f, y + 48f, 26f, 20f, mouseX, mouseY);
        if (FPSMaster.isDevelopment()) {
            devTools.renderInScreen(this, x, y + 72f, 100f, 20f, mouseX, mouseY);
        }

        // Render copyright and other text info
        float w = FPSMaster.fontManager.s16.getStringWidth("Copyright Mojang AB. Do not distribute!");
        FPSMaster.fontManager.s16.drawString("Copyright Mojang AB. Do not distribute!", guiWidth - w - 4, guiHeight - 14, Color.WHITE.getRGB());

        // Render client info
        Rects.fill(0f, 0f, 0f, 0f, -1);
        FPSMaster.fontManager.s16.drawString(FPSMaster.COPYRIGHT, 4, guiHeight - 14, Color.WHITE.getRGB());
        FPSMaster.fontManager.s16.drawString(FPSMaster.CLIENT_NAME + " Client " + FPSMaster.CLIENT_VERSION + " (Minecraft " + FPSMaster.EDITION + ")", 4, guiHeight - 28, Color.WHITE.getRGB());
        if (firstBoot != 2) {
            FPSMaster.fontManager.s16.drawCenteredString(FPSMaster.i18n.get(firstBoot == 0 ? "mainmenu.oldjava" : "mainmenu.javafail"), guiWidth / 2f, guiHeight / 2f + 40, Color.WHITE.getRGB());
            FPSMaster.fontManager.s16.drawCenteredString(FPSMaster.i18n.get("mainmenu.javatip"), guiWidth / 2f, guiHeight / 2f + 50, Color.WHITE.getRGB());
        }
        Rects.fill(0, 0, guiWidth, guiHeight, new Color(20, 20, 20, (int) (255 - 255 * Math.max(0, (float) backgroundAnimation.get() - 0.5f))));
        Images.drawSmooth(new ResourceLocation("client/gui/logo.png"), guiWidth / 2f - 84f / 2f, guiHeight / 2f - 30 - 70 * ((float) Math.min(startAnimation.get(), 1)), 84f, 65f, -1);
        handlePendingClick();
    }

    public static void preloadPlayerSkinTexture() {
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
        FPSMaster.async.runnable(() -> {
            try {
                String skinUrl = readSkinUrl(normalizedPlayerId);
                if (skinUrl == null || skinUrl.trim().isEmpty()) {
                    fallbackToDefaultSkin(normalizedPlayerId);
                    return;
                }
                BufferedImage skinImage = readImage(skinUrl);
                if (skinImage == null || skinImage.getWidth() < 64 || skinImage.getHeight() < 32) {
                    fallbackToDefaultSkin(normalizedPlayerId);
                    return;
                }
                BufferedImage textureImage = ensureArgbImage(skinImage);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (!normalizedPlayerId.equals(loadedSkinPlayerId)) {
                        return;
                    }
                    playerSkinTexture = Minecraft.getMinecraft()
                            .getTextureManager()
                            .getDynamicTextureLocation("fpsmaster_player_skin_" + normalizedPlayerId, new DynamicTexture(textureImage));
                    playerSkinLoadFailed = false;
                });
            } catch (Exception exception) {
                ClientLogger.warn("Failed to load main menu player skin from Mojang API");
                fallbackToDefaultSkin(normalizedPlayerId);
            } finally {
                SKIN_LOADING.set(false);
            }
        });
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

    private void handlePendingClick() {
        if (consumePressInBounds(guiWidth - 22, 13, 12, 12, 0) != null) {
            mc.displayGuiScreen(new BackgroundSelector());
        }
    }
}




