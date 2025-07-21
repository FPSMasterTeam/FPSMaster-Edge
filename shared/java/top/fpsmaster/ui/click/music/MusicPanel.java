package top.fpsmaster.ui.click.music;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.music.AbstractMusic;
import top.fpsmaster.modules.music.JLayerHelper;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.modules.music.PlayList;
import top.fpsmaster.modules.music.netease.Music;
import top.fpsmaster.modules.music.netease.NeteaseApi;
import top.fpsmaster.modules.music.netease.deserialize.MusicWrapper;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.io.File;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public class MusicPanel {

    private static PlayList displayList = new PlayList();
    private static PlayList recommendList = new PlayList();
    private static PlayList searchList = new PlayList();
    private static Thread searchThread = null;

    private static float playProgress = 0f;
    //    private static final SearchBox inputBox = new SearchBox(FPSMaster.i18n.get("music.search"), () -> {
//        searchThread = new Thread(MusicPanel::run);
//        searchThread.start();
//    });
    private static final TextField inputBox = new TextField(FPSMaster.fontManager.s16, FPSMaster.i18n.get("music.search"), new Color(40, 40, 40, 180).getRGB(), -1, 100, () -> {
        searchThread = new Thread(MusicPanel::run);
        searchThread.start();
    });

    private static final String[] pages = {"music.name", "music.list", "music.daily"};
    private static int curSearch = 0;
    private static boolean isWaitingLogin = false;
    private static String key = null;
    private static Thread loginThread = null;
    private static final ScrollContainer container = new ScrollContainer();

    public static int code = 801;
    public static String nickname = "Unknown";
    public static float x = 0f;
    public static float y = 0f;
    public static float width = 0f;
    public static float height = 0f;
    private static Thread playThread;

    public static void mouseClicked(int mouseX, int mouseY, int btn) {
        inputBox.mouseClicked(mouseX, mouseY, btn);
        if (searchThread == null || !searchThread.isAlive()) {
            float dY = y + 50 + container.getRealScroll();
            for (AbstractMusic music : displayList.musics) {
                if (Render2DUtils.isHovered(x, dY, width - 10f, 40f, mouseX, mouseY) && mouseY < y + height - 34 && mouseY > y + 34) {
                    if (Mouse.isButtonDown(0)) {
                        music.play();
                        MusicPlayer.playList.current = MusicPlayer.playList.musics.indexOf(music);
                    }
                }
                dY += 40f;
            }
        }

        // 搜索
        int xOffset = 0;
        for (int i = 0; i < pages.length; i++) {
            String page = pages[i];
            int width = FPSMaster.fontManager.s16.getStringWidth(FPSMaster.i18n.get(page)) + 10;
            if (Render2DUtils.isHovered(x + 95 + xOffset, y + 8, width, 14f, mouseX, mouseY)) {
                if (Mouse.isButtonDown(0)) {
                    curSearch = i;
                    if (curSearch == 2) {
                        MusicPlayer.playList.pause();
                        searchThread = new Thread(() -> {
                            recommendList = MusicWrapper.getSongsFromDaily();
                            displayList = recommendList;
                            setMusicList();
                        });
                        searchThread.start();
                    }
                }
            }
            xOffset += width;
        }

        // 操作栏
        AbstractMusic current = MusicPlayer.playList.current();
        if (Render2DUtils.isHovered(x, y + height - 30, width, 4f, mouseX, mouseY)) {
            if (Mouse.isButtonDown(0) && current != null) {
                if (!MusicPlayer.isPlaying) {
                    if (playThread != null && playThread.isAlive()) {
                        playThread.interrupt();
                    }
                    playThread = new Thread(
                            () -> {
                                MusicPlayer.playList.play();
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                current.seek((mouseX - x) / width);
                            }
                    );
                    MusicPlayer.isPlaying = true;
                } else {
                    current.seek((mouseX - x) / width);
                }
            }
        }
        if (Render2DUtils.isHovered(x + width / 2 - 35, y + height - 23, 16f, 16f, mouseX, mouseY) && btn == 0) {
            MusicPlayer.playList.previous();
        }
        if (Render2DUtils.isHovered(x + width / 2 + 5, y + height - 23, 16f, 16f, mouseX, mouseY) && btn == 0) {
            MusicPlayer.playList.next();
        }
        if (Render2DUtils.isHovered(x + width / 2 - 15, y + height - 23, 35 / 2f, 35 / 2f, mouseX, mouseY) && btn == 0) {
            if (!MusicPlayer.playList.musics.isEmpty()) {
                if (MusicPlayer.isPlaying) {
                    playProgress = MusicPlayer.getPlayProgress();
                    MusicPlayer.playList.pause();
                } else {
                    FPSMaster.async.runnable(() -> {
                        MusicPlayer.playList.current().play();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        MusicPlayer.playList.current().seek(playProgress);
                    });
                    MusicPlayer.isPlaying = true;
                }
            }
        }
        if (Render2DUtils.isHovered(x + width / 2 - 55, y + height - 21, 12f, 12f, mouseX, mouseY) && btn == 0) {
            if (MusicPlayer.mode < 2) {
                MusicPlayer.mode++;
                MusicPlayer.playList.setMusicList(displayList.musics);
            } else {
                MusicPlayer.mode = 0;
            }
        }
    }

    public static void keyTyped(char c, int keyCode) {
        inputBox.textboxKeyTyped(c, keyCode);
    }

    public static void draw(float x, float y, float width, float height, int mouseX, int mouseY, int scaleFactor) {
        if (isWaitingLogin) {
            FPSMaster.fontManager.s18.drawCenteredString("<", x + 20, y + 20, new Color(234, 234, 234).getRGB());
            if (Render2DUtils.isHovered(x + 20, y + 20, 20f, 20f, mouseX, mouseY) && Mouse.isButtonDown(0)) {
                isWaitingLogin = false;
            }
            try {
                ResourceLocation resourceLocation = new ResourceLocation("music/qr");
                Render2DUtils.drawImage(resourceLocation, x + width / 2 - 45, y + height / 2 - 45, 90f, 90f, -1);
            } catch (Exception e) {
                // Handle exception
            }
            String scan = "";
            switch (code) {
                case 801:
                    scan = "music.waitscan";
                    break;
                case 802:
                    scan = "music.waitconfirmation";
                    break;
                case 800:
                    reloadImg();
                    code = 801;
                    break;
                case 803:
                    isWaitingLogin = false;
                    break;
            }
            FPSMaster.fontManager.s18.drawCenteredString(FPSMaster.i18n.get(scan), x + width / 2, y + height / 2 + 60, new Color(234, 234, 234).getRGB());
            return;
        }
        MusicPanel.x = x;
        MusicPanel.y = y;
        MusicPanel.width = width;
        MusicPanel.height = height;
        if (displayList.musics.isEmpty() && searchThread == null) {
            searchThread = new Thread(() -> searchList = MusicWrapper.searchSongs("C418"));
            searchThread.start();
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(x, y, width, height - 30, scaleFactor);
        if (searchThread == null || !searchThread.isAlive()) {
            AtomicReference<Float> dY = new AtomicReference<>(y + 50 + container.getScroll());
            AtomicReference<Float> musicHeight = new AtomicReference<>(0f);

            Render2DUtils.drawRect(x, dY.get() - 6, width - 10, 0.5f, new Color(100, 100, 100, 50));
            FPSMaster.fontManager.s16.drawString("#", x + 12, dY.get() - 20, new Color(234, 234, 234).getRGB());
            FPSMaster.fontManager.s16.drawString("标题", x + 30, dY.get() - 20, new Color(234, 234, 234).getRGB());

            // music list
            container.draw(x, y + 50, width - 5, height - 80, mouseX, mouseY, () -> {
                for (int i = 0; i < displayList.musics.size(); i++) {
                    Music music = (Music) displayList.musics.get(i);
                    if (Render2DUtils.isHovered(x, dY.get(), width - 10f, 40f, mouseX, mouseY) && mouseY > y + 50 && mouseY < y + height - 34) {
                        Render2DUtils.drawOptimizedRoundedRect(x, dY.get(), width - 10, 40f, new Color(200, 200, 200, 50));
                    }
                    FPSMaster.fontManager.s16.drawCenteredString("" + i, x + 15, dY.get() + 15, new Color(234, 234, 234).getRGB());
                    if (dY.get() > y && dY.get() < y + height - 10) {
                        if (music.isLoadedImage) {
                            Render2DUtils.drawImage(new ResourceLocation("music/netease/" + music.id), x + 30, dY.get() + 10, 20f, 20f, -1);
                        } else {
                            Render2DUtils.drawOptimizedRoundedRect(x + 30, dY.get() + 10, 20f, 20f, new Color(200, 200, 200, 255));
                        }
                    }
                    FPSMaster.fontManager.s16.drawString(music.name + "  " + music.author, x + 60, dY.get() + 10, new Color(234, 234, 234).getRGB());
                    FPSMaster.fontManager.s16.drawString(music.author, x + 60, dY.get() + 20, new Color(162, 162, 162).getRGB());
                    dY.updateAndGet(v -> v + 40f);
                    musicHeight.updateAndGet(v -> v + 40f);
                }
                container.setHeight(musicHeight.get());
            });

        } else {
            FPSMaster.fontManager.s18.drawCenteredString("...", x + width / 2, y + 60, new Color(234, 234, 234).getRGB());
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 搜索
        inputBox.drawTextBox(x + 5, y + 8, 80f, 16f);

        // 分页
        int xOffset = 0;
        int pagesWidth = 0;
        for (String page : pages) {
            pagesWidth += FPSMaster.fontManager.s16.getStringWidth(FPSMaster.i18n.get(page)) + 10;
        }
        Render2DUtils.drawOptimizedRoundedRect(x + 90, y + 8, pagesWidth, 16f, new Color(50, 50, 50, 100).getRGB());
        for (String page : pages) {
            int stringWidth = FPSMaster.fontManager.s16.getStringWidth(FPSMaster.i18n.get(page));
            if (page.equals(pages[curSearch])) {
                Render2DUtils.drawOptimizedRoundedRect(x + 90 + xOffset, y + 8, stringWidth + 10, 16f, -1);
                FPSMaster.fontManager.s16.drawString(FPSMaster.i18n.get(page), x + 95 + xOffset, y + 12, new Color(50, 50, 50).getRGB());
            } else {
                FPSMaster.fontManager.s16.drawString(FPSMaster.i18n.get(page), x + 95 + xOffset, y + 12, -1);
            }
            xOffset += stringWidth + 10;
        }

        // login
        if (NeteaseApi.cookies.isEmpty()) {
            if (!nickname.isEmpty()) {
                nickname = "Unknown";
            }
            int stringWidth = FPSMaster.fontManager.s16.getStringWidth(FPSMaster.i18n.get("music.notLoggedIn"));
            if (Render2DUtils.isHovered(x + width - stringWidth - 5, y + 10, stringWidth, 16f, mouseX, mouseY)) {
                FPSMaster.fontManager.s16.drawString(FPSMaster.i18n.get("music.notloggedin"), x + width - stringWidth - 5, y + 10, new Color(234, 234, 234).getRGB());
                if (Mouse.isButtonDown(0)) {
                    isWaitingLogin = true;
                    reloadImg();
                    code = 801;
                }
            } else {
                FPSMaster.fontManager.s16.drawString(FPSMaster.i18n.get("music.notloggedin"), x + width - stringWidth - 5, y + 10, new Color(162, 162, 162).getRGB());
            }
        } else {
            int stringWidth = FPSMaster.fontManager.s16.getStringWidth(nickname);
            FPSMaster.fontManager.s16.drawString(nickname, x + width - stringWidth - 5, y + 10, -1);
            if (Render2DUtils.isHovered(x + width - stringWidth - 5, y + 10, stringWidth, 16f, mouseX, mouseY)) {
                if (Mouse.isButtonDown(0)) {
                    isWaitingLogin = true;
                    reloadImg();
                    code = 801;
                }
            }
        }

        // 操作栏
        AbstractMusic current = MusicPlayer.playList.current();
        Render2DUtils.drawRect(x, y + height - 30, width, 2f, new Color(58, 58, 58).getRGB());
        Render2DUtils.drawRect(x, y + height - 30, width * MusicPlayer.getPlayProgress(), 2f, -1);
        if (Render2DUtils.isHovered(x, y + height - 32, width, 4f, mouseX, mouseY)) {
            Render2DUtils.drawRect(x, y + height - 31f, width * MusicPlayer.getPlayProgress(), 4f, -1);
        }

        // 音量
        Render2DUtils.drawImage(new ResourceLocation("client/textures/ui/volume.png"), x + width - 50, y + height - 16, 7f, 7f, -1);
        Render2DUtils.drawRect(x + width - 40, y + height - 14, 30f, 2f, new Color(58, 58, 58).getRGB());
        Render2DUtils.drawRect(x + width - 40, y + height - 14, 30 * MusicPlayer.getVolume(), 2f, -1);
        if (Render2DUtils.isHovered(x + width - 40, y + height - 14, 30f, 2f, mouseX, mouseY)) {
            Render2DUtils.drawRect(x + width - 40, y + height - 14.5f, 30 * MusicPlayer.getVolume(), 3f, -1);
            if (Mouse.isButtonDown(0)) {
                float newVolume = (mouseX - x - width + 40) / 30f;
                MusicPlayer.setVolume(newVolume);
            }
        }
        int trimWidth = (int) (width / 2 - 100);
        if (!MusicPlayer.playList.musics.isEmpty() && current != null) {
            String name = FPSMaster.fontManager.s18.trimString(current.name + " - " + current.author, trimWidth, false);
            FPSMaster.fontManager.s18.drawString(name, x + 30, y + height - 23, new Color(234, 234, 234).getRGB());
            String progress = "0:00/0:00";
            if (JLayerHelper.clip != null) {
                double duration = JLayerHelper.getDuration();
                int minutes = (int) (duration * MusicPlayer.getPlayProgress());
                int seconds = (int) ((duration * MusicPlayer.getPlayProgress() - minutes) * 60);
                progress = minutes + ":" + seconds + "/" + (int) duration + ":" + (int) ((duration - (int) duration) * 60);
            }
            FPSMaster.fontManager.s16.drawString(progress, x + 30, y + height - 14, new Color(162, 162, 162).getRGB());
            if (MusicPlayer.playList.current() != null && ((Music) MusicPlayer.playList.current()).isLoadedImage) {
                Render2DUtils.drawImage(new ResourceLocation("music/netease/" + ((Music) MusicPlayer.playList.current()).id), x + 5, y + height - 24, 20f, 20f, -1);
            } else {
                Render2DUtils.drawOptimizedRoundedRect(x + 5, y + height - 24, 20f, 20f, new Color(200, 200, 200, 255));
            }
        }
        ResourceLocation res = new ResourceLocation("client/gui/settings/music/loop.png");
        switch (MusicPlayer.mode) {
            case 0:
                res = new ResourceLocation("client/gui/settings/music/shuffle.png");
                break;
            case 1:
                res = new ResourceLocation("client/gui/settings/music/order.png");
                break;
            case 2:
                res = new ResourceLocation("client/gui/settings/music/loop.png");
                break;
        }
        Render2DUtils.drawImage(res, x + width / 2 - 55, y + height - 21, 12f, 12f, new Color(234, 234, 234));
        Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/music/previous.png"), x + width / 2 - 35, y + height - 23, 16f, 16f, new Color(234, 234, 234));
        Render2DUtils.drawImage(MusicPlayer.isPlaying ? new ResourceLocation("client/gui/settings/music/pause.png") : new ResourceLocation("client/gui/settings/music/play.png"), x + width / 2 - 15, y + height - 23, 35 / 2f, 35 / 2f, -1);
        Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/music/next.png"), x + width / 2 + 5, y + height - 23, 16f, 16f, new Color(234, 234, 234));
    }

    private static String extractMiddleContent(String input, String prefix, String suffix) {
        int startIndex = input.indexOf(prefix);
        if (startIndex != -1) {
            int endIndex = input.indexOf(suffix, startIndex + prefix.length());
            if (endIndex != -1) {
                return input.substring(startIndex + prefix.length(), endIndex);
            }
        }
        return "";
    }

    private static void reloadImg() {
        loginThread = new Thread(() -> {
            while (isWaitingLogin) {
                try {
                    JsonObject loginStatus = MusicWrapper.getLoginStatus(key);
                    code = loginStatus.get("code").getAsInt();
                    if (code == 802) {
                        String element = loginStatus.get("nickname").getAsString();
                        if (element != null) {
                            nickname = element;
                            try {
                                FileUtils.saveTempValue("nickname", nickname);
                            } catch (FileException e) {
                                ExceptionHandler.handleFileException(e, "无法保存昵称");
                            }
                        }
                    }
                    if (code == 803) {
                        // parse cookie
                        String asString = loginStatus.get("cookie").getAsString();
                        String result = "MUSIC_U=" + extractMiddleContent(asString, "MUSIC_U=", ";") + "; " + "NMTID=" + extractMiddleContent(asString, "NMTID=", ";");
                        NeteaseApi.cookies = result;

                        try {
                            FileUtils.saveTempValue("cookies", NeteaseApi.cookies);
                            System.out.println("cookies: " + NeteaseApi.cookies);
                        } catch (FileException e) {
                            ExceptionHandler.handleFileException(e, "无法保存cookies");
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        FPSMaster.async.runnable(() -> {
            key = MusicWrapper.getQRKey();
            if (key == null) return;
            String base64 = MusicWrapper.getQRCodeImg(key);
            // render base64 img data
            net.minecraft.client.renderer.texture.TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            // base64 decode
            byte[] bytes = Base64.getDecoder().decode(base64);
            // create resource location
            ResourceLocation resourceLocation = new ResourceLocation("music/qr");
            File qr = new File(FileUtils.dir, "/music/qr.png");
            File qrf = new File(FileUtils.dir, "/music");
            qrf.mkdirs();
            try {
                FileUtils.saveFileBytes("/music/qr.png", bytes);
                ThreadDownloadImageData textureArt = new ThreadDownloadImageData(qr, null, null, null);
                textureManager.loadTexture(resourceLocation, textureArt);
                loginThread.start();
            } catch (FileException e) {
                ExceptionHandler.handleFileException(e, "无法保存二维码图片");
            }
        });
    }

    private static void setMusicList() {
        MusicPlayer.playList.setMusicList(displayList.musics);
    }

    private static void run() {
        if (!inputBox.getText().isEmpty()) {
            searchList = curSearch == 0 ? MusicWrapper.searchSongs(inputBox.getText()) : MusicWrapper.searchList(inputBox.getText());
            displayList = searchList;
            MusicPlayer.playList.pause();
            setMusicList();
            searchThread = null;
        }
    }
}
