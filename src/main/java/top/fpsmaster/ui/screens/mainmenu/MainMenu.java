package top.fpsmaster.ui.screens.mainmenu;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.mc.GuiMultiplayer;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Backgrounds;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;

public class MainMenu extends ScaledGuiScreen {
    private static int firstBoot = 0;

    // Buttons for the main menu
    private final MenuButton singlePlayer;
    private final MenuButton multiPlayer;
    private final MenuButton options;
    private final MenuButton exit;

    private String info = "离线模式";

    private static final Animator startAnimation = new Animator();
    private static final Animator backgroundAnimation = new Animator();
    private final AnimClock animClock = new AnimClock();


    public MainMenu() {
        singlePlayer = new MenuButton("mainmenu.single", () -> mc.displayGuiScreen(new GuiSelectWorld(this)));
        multiPlayer = new MenuButton("mainmenu.multi", () -> mc.displayGuiScreen(new GuiMultiplayer()));
        options = new MenuButton("mainmenu.settings", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)));
        exit = new MenuButton("X", () -> mc.shutdown());
    }

    @Override
    public void initGui() {
        super.initGui();
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
        float stringWidth = FPSMaster.fontManager.s16.getStringWidth(mc.getSession().getUsername());
        Rects.rounded(10f, 10f, 30 + stringWidth, 20f, new Color(0, 0, 0, 60));
        Images.draw(new ResourceLocation("client/gui/screen/avatar.png"), 14f, 15f, 10f, 10f, -1);
        FPSMaster.fontManager.s16.drawString(mc.getSession().getUsername(), 28, 16, Color.WHITE.getRGB());


        // background theme button
        Rects.rounded(guiWidth - 22, 13, 12, 12, new Color(0, 0, 0, 60));
        Images.draw(new ResourceLocation("client/gui/screen/theme.png"), guiWidth - 20, 15f, 8f, 8f, -1);


        // Position buttons and render them
        float x = guiWidth / 2f - 50;
        float y = guiHeight / 2f - 30;
        singlePlayer.render(x, y, 100f, 20f, mouseX, mouseY);
        multiPlayer.render(x, y + 24f, 100f, 20f, mouseX, mouseY);
        options.render(x, y + 48f, 70f, 20f, mouseX, mouseY);
        exit.render(x + 74f, y + 48f, 26f, 20f, mouseX, mouseY);

        // Render copyright and other text info
        float w = FPSMaster.fontManager.s16.getStringWidth("Copyright Mojang AB. Do not distribute!");
        FPSMaster.fontManager.s16.drawString("Copyright Mojang AB. Do not distribute!", guiWidth - w - 4, guiHeight - 14, Color.WHITE.getRGB());

        // Display welcome message
        String welcome = EnumChatFormatting.GREEN + FPSMaster.i18n.get("mainmenu.single");
        FPSMaster.fontManager.s16.drawString(welcome, 4, guiHeight - 52, Color.WHITE.getRGB());

        FPSMaster.fontManager.s16.drawString(info, 4, guiHeight - 40, Color.WHITE.getRGB());

        // Render client info
        Rects.fill(0f, 0f, 0f, 0f, -1);
        FPSMaster.fontManager.s16.drawString(FPSMaster.COPYRIGHT, 4, guiHeight - 14, Color.WHITE.getRGB());
        FPSMaster.fontManager.s16.drawString(FPSMaster.CLIENT_NAME + " Client " + FPSMaster.CLIENT_VERSION + " (Minecraft " + FPSMaster.EDITION + ")", 4, guiHeight - 28, Color.WHITE.getRGB());
        if (firstBoot != 2) {
            FPSMaster.fontManager.s16.drawCenteredString(FPSMaster.i18n.get(firstBoot == 0 ? "mainmenu.oldjava" : "mainmenu.javafail"), width / 2f, height / 2f + 40, Color.WHITE.getRGB());
            FPSMaster.fontManager.s16.drawCenteredString(FPSMaster.i18n.get("mainmenu.javatip"), width / 2f, height / 2f + 50, Color.WHITE.getRGB());
        }
        Rects.fill(0, 0, width, height, new Color(20, 20, 20, (int) (255 - 255 * Math.max(0, (float) backgroundAnimation.get() - 0.5f))));
        Images.draw(new ResourceLocation("client/gui/logo.png"), guiWidth / 2f - 153 / 4f, guiHeight / 2f - 30 - 70 * ((float) Math.min(startAnimation.get(), 1)), 153 / 2f, 67f, -1);
    }

    @Override
    public void onClick(int mouseX, int mouseY, int mouseButton) {
        singlePlayer.mouseClick(mouseX, mouseY, mouseButton);
        multiPlayer.mouseClick(mouseX, mouseY, mouseButton);
        options.mouseClick(mouseX, mouseY, mouseButton);
        exit.mouseClick(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            if (Hover.is(guiWidth - 22, 13, 12, 12, mouseX, mouseY)) {
                if (FPSMaster.configManager.configure.getOrCreate("background", "new").equals("classic")) {
                    FPSMaster.configManager.configure.set("background", "new");
                } else {
                    FPSMaster.configManager.configure.set("background", "classic");
                }
            }
        }
    }
}




