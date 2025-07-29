package top.fpsmaster.ui.click.music;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.modules.music.*;
import top.fpsmaster.modules.music.netease.Music;
import top.fpsmaster.modules.music.netease.NeteaseApi;
import top.fpsmaster.modules.music.netease.NeteaseProfile;
import top.fpsmaster.modules.music.netease.deserialize.MusicWrapper;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.math.animation.Animation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;

import static top.fpsmaster.utils.Utility.mc;

public class NewMusicPanel {

    // Threads
    private static Thread loginThread = null;
    private static Thread songsLoadThread = null;
    private static Thread playThread = null;

    // login
    static boolean isWaitingLogin = false;
    static int loginCode;
    public static String nickname = "";
    private static String key;
    private static ArrayList<Track> dailyTracks = new ArrayList<>();
    private static ArrayList<Track> likedTracks = new ArrayList<>();

    static Track recommendTrack;
    static Thread loadThread = null;

    // tracks
    static Track currentTrack;

    public static Music playing;
    static NeteaseProfile profile;
    private static Track playingTrack;
    private static final Animation opacityAnimation = new Animation();
    // search
    public static TextField searchField = new TextField(FPSMaster.fontManager.s14, "搜索歌曲", new Color(56, 56, 56).getRGB(), new Color(200, 200, 200).getRGB(), 50, NewMusicPanel::search);
    static boolean searching = false;

    public static void search() {
        FPSMaster.async.runnable(() -> {
            searching = true;
            currentTrack = new Track(-1L, "", "");
            PlayList playList = MusicWrapper.searchSongs(searchField.getText());
            currentTrack.setMusics(playList.musics);
            currentTrack.setLoaded(true);
            searching = false;
        });
    }

    int mode = 0;

    public static void init() {
        if (playThread == null) {
            playThread = new Thread(() -> {
                while (((IMinecraft) mc).arch$getRunning()) {
                    if (playing != null) {
                        // next song
                        if (MusicPlayer.getPlayProgress() > 0.999f) {
                            MusicPlayer.isPlaying = false;
                            nextSong();
                        }
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            playThread.start();
        }
        if (loadThread == null || !loadThread.isAlive()) {
            loadThread = new Thread(() -> {
                searching = true;
                if (profile == null)
                    profile = MusicWrapper.getProfile();
                if (profile != null) {
                    if (dailyTracks.isEmpty()) {
                        dailyTracks = MusicWrapper.getTracksDaily();
                    }
                    if (likedTracks.isEmpty()) {
                        likedTracks = MusicWrapper.getTracksLiked(profile.id);
                    }
                }
                searching = false;
                if (recommendTrack == null || recommendTrack.getMusics().isEmpty()) {
                    recommendTrack = new Track(0L, "日推", "");
                    recommendTrack.setMusics(MusicWrapper.getSongsFromDaily().musics);
                    recommendTrack.setLoaded(true);
                }
            });
            loadThread.start();
        }
    }

    private static void nextSong() {
        int i = playingTrack.getMusics().indexOf(playing);
        if (i + 1 >= playingTrack.getMusics().size()) {
            i = 0;
        } else {
            i++;
        }
        playing = (Music) playingTrack.getMusics().get(i);
        playing.play();
    }

    // containers
    static ScrollContainer scrollContainer = new ScrollContainer();
    static ScrollContainer songsContainer = new ScrollContainer();

    public static void draw(float x, int y, float width, float height, int mouseX, int mouseY, int scaleFactor) {
        Render2DUtils.drawImage(new ResourceLocation("client/gui/music.png"), x + 12, y + 14, 75, 16, -1);
        searchField.drawTextBox(x + 32, y + 14, 100, 16);
        if (Render2DUtils.isHovered(x + 12, y + 14, 16, 16, mouseX, mouseY) && consumeClick(0)) {
            currentTrack = null;
            isWaitingLogin = false;
            if (loginThread != null) {
                loginThread.interrupt();
                loginThread = null;
            }
        }
        if (profile == null) {
            int stringWidth = FPSMaster.fontManager.s16.getStringWidth(FPSMaster.i18n.get("music.notLoggedIn"));
            Color color = new Color(162, 162, 162);
            if (Render2DUtils.isHovered(x + width - stringWidth - 5, y + 15, stringWidth, 16f, mouseX, mouseY)) {
                color = new Color(234, 234, 234);
                if (consumeClick(0)) {
                    isWaitingLogin = true;
                    reloadImg();
                    loginCode = 801;
                }
            }
            FPSMaster.fontManager.s16.drawString(FPSMaster.i18n.get("music.notloggedin"), x + width - stringWidth - 10, y + 15, color.getRGB());
        } else {
            int stringWidth = FPSMaster.fontManager.s16.getStringWidth(profile.nickname);
            Render2DUtils.drawWebImage(profile.avatarUrl, x + width - stringWidth - 30, y + 14f, 16, 16);
            FPSMaster.fontManager.s16.drawString(profile.nickname, x + width - stringWidth - 10, y + 15, -1);
            if (Render2DUtils.isHovered(x + width - stringWidth - 10, y + 10, stringWidth, 16f, mouseX, mouseY)) {
                if (consumeClick(0)) {
                    isWaitingLogin = true;
                    reloadImg();
                    loginCode = 801;
                }
            }
        }
        if(isWaitingLogin) {
            ResourceLocation resourceLocation = new ResourceLocation("music/qr");
            Render2DUtils.drawImage(resourceLocation, x + width / 2 - 45, y + height / 2 - 45, 90f, 90f, -1);
        }else{
            if (searching) {
                FPSMaster.fontManager.s22.drawCenteredString("加载中...", x + width / 2, y + height / 2 - 20, -1);
            } else {
                if (currentTrack == null) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    Render2DUtils.doGlScissor(x + 12, y + 35, width - 12, height - 65, scaleFactor);
                    int finalY = (int) (y + scrollContainer.getScroll());
                    scrollContainer.draw(x + 12, y + 35, width - 12, height - 65, mouseX, mouseY, () -> {
                        String recommendSong = "";

                        if (recommendTrack != null && !recommendTrack.getMusics().isEmpty()) {
                            recommendSong = recommendTrack.getMusics().get(0).name;
                        }

                        int tX = 0;
                        int tY = 60;
                        FPSMaster.fontManager.s22.drawString("推荐歌单", x + 20, finalY + 40, -1);
                        if (recommendTrack != null)
                            drawTrack(recommendTrack, "每日推荐", "每日推荐，从『" + recommendSong + "』听起", x + 20, finalY + 60, mouseX, mouseY, new Color(255, 73, 73, 200));
                        tY = drawTracks(x, width, mouseX, mouseY, finalY, tX + 70, tY, dailyTracks);
                        tY += 120;
                        FPSMaster.fontManager.s22.drawString("收藏的歌单", x + 20, finalY + tY - 20, -1);
                        tY = drawTracks(x, width, mouseX, mouseY, finalY, tX, tY, likedTracks);
                        scrollContainer.setHeight(tY + 80);
                    });
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                } else {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    Render2DUtils.doGlScissor(x + 12, y + 35, width - 12, height - 65, scaleFactor);
                    if (!currentTrack.isLoaded() && (songsLoadThread == null || !songsLoadThread.isAlive())) {
                        songsLoadThread = new Thread(() -> {
                            searching = true;
                            currentTrack.loadMusic();
                            searching = false;
                        });
                        songsLoadThread.start();
                    }
                    if (currentTrack.isLoaded()) {
                        int finalY = (int) (y + songsContainer.getScroll());
                        songsContainer.draw(x + 12, y + 35, width - 12, height - 65, mouseX, mouseY, () -> {
                            int sY = 40;
                            for (AbstractMusic music : currentTrack.getMusics()) {
                                if (finalY + sY + 25 > y + 35 && finalY + sY < y + 35 + height - 65) {
                                    Music neteaseMusic = (Music) music;
                                    boolean isHovered = Render2DUtils.isHovered(x + 15, finalY + sY - 5, width - 25, 30, mouseX, mouseY);
                                    if (isHovered) {
                                        Render2DUtils.drawOptimizedRoundedRect(x + 15,finalY + sY - 5, width - 25, 30,new Color(255,255,255,50));
                                        if(consumeClick(0)){
                                            playing = (Music) music;
                                            playingTrack = currentTrack;
                                            music.play();
                                        }
                                    }
                                    if (neteaseMusic.isLoadedImage) {
                                        Render2DUtils.drawImage(new ResourceLocation("music/netease/" + neteaseMusic.id), x + 20, finalY + sY, 20f, 20f, -1);
                                    } else {
                                        Render2DUtils.drawOptimizedRoundedRect(x + 20, finalY + sY, 20f, 20f, new Color(200, 200, 200, 255));
                                    }
                                    FPSMaster.fontManager.s18.drawString(neteaseMusic.name, x + 45, finalY + sY, -1);
                                    FPSMaster.fontManager.s14.drawString(neteaseMusic.author, x + 45, finalY + sY + 10, new Color(200, 200, 200).getRGB());
                                }
                                sY += 31;
                            }
                            songsContainer.setHeight(sY);
                        });
                    }
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }

                Render2DUtils.drawRoundedRectImage(x + 1, y + height - 30, width - 2, 31, 10, new Color(0, 0, 0, 150));
                if (playing != null) {
                    int opacity;
                    opacityAnimation.start(0,255,0.2f,Type.EASE_IN_QUAD);
                    if(!opacityAnimation.isFinished()){
                        opacityAnimation.update();
                        opacity = (int) opacityAnimation.value;
                    } else {
                        opacity = 255;
                    }

                    UFontRenderer s14 = FPSMaster.fontManager.s14;
                    s14.drawString(s14.trimString(playing.name, 100, false), x + 30, y + height - 22, new Color(234, 234, 234, opacity).getRGB());
                    s14.drawString(s14.trimString(playing.author, 60, false), x + 30, y + height - 12, new Color(124, 124, 124, opacity).getRGB());

                    if (playing.isLoadedImage) {
                        Render2DUtils.drawImage(new ResourceLocation("music/netease/" + playing.id), x + 5, y + height - 25, 20f, 20f, new Color(255,255,255,opacity));
                    } else {
                        Render2DUtils.drawOptimizedRoundedRect(x + 5, y + height - 25, 20f, 20f, new Color(200, 200, 200, opacity));
                    }
                    // 进度条
                    if (Render2DUtils.isHovered(x + width / 2 - 80, y + height - 6, 160, 3, mouseX, mouseY)) {
                        Render2DUtils.drawRoundedRectImage(x + width / 2 - 80, y + height - 6, 160, 3, 3, new Color(95, 95, 95,opacity));
                        if (consumeClick(0)) {
                            playing.seek((mouseX - (x + width / 2 - 80)) / 160f);
                            if (!MusicPlayer.isPlaying)
                                MusicPlayer.play();
                        }
                    } else {
                        Render2DUtils.drawRoundedRectImage(x + width / 2 - 80, y + height - 6, 160, 3, 3, new Color(51, 51, 51,opacity));
                    }
                    float playProgress = MusicPlayer.getPlayProgress();
                    if (JLayerHelper.clip == null) {
                        Render2DUtils.drawRoundedRectImage(x + width / 2 - 80, y + height - 6, 160f * playing.downloadProgress, 3, 3, new Color(148, 148, 148,opacity));
                    } else {
                        Render2DUtils.drawRoundedRectImage(x + width / 2 - 80, y + height - 6, 160f * playProgress, 3, 3, new Color(225, 73, 73,opacity));
                    }

                    // 操作按钮
                    Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/music/previous.png"), x + width / 2 - 35, y + height - 25, 16f, 16f, new Color(234, 234, 234,opacity));
                    Render2DUtils.drawImage(MusicPlayer.isPlaying ? new ResourceLocation("client/gui/settings/music/pause.png") : new ResourceLocation("client/gui/settings/music/play.png"), x + width / 2 - 15, y + height - 26, 35 / 2f, 35 / 2f, new Color(255,255,255,opacity));
                    Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/music/next.png"), x + width / 2 + 5, y + height - 25, 16f, 16f, new Color(234, 234, 234,opacity));

                    // 音量键
                    Render2DUtils.drawImage(new ResourceLocation("client/textures/ui/volume.png"), x + width  - 62, y + height - 16, 7f, 7f, new Color(234, 234, 234,opacity));
                    Render2DUtils.drawRoundedRectImage(x + width  - 50, y + height - 13, 30f, 2f,1, new Color(108, 108, 108));
                    float volume = MusicPlayer.getVolume();
                    Render2DUtils.drawRoundedRectImage(x + width  - 50, y + height - 13, 30f * volume, 2f,1, new Color(255,255,255));
                    if (Render2DUtils.isHovered(x + width  - 50, y + height - 13, 30f, 2f, mouseX, mouseY) && Mouse.isButtonDown(0)){
                        float newVolume = (mouseX - (x + width  - 50)) / 30f;
                        MusicPlayer.setVolume(newVolume);
                    }


                    if (JLayerHelper.clip != null) {
                        if (Render2DUtils.isHovered(x + width / 2 - 15, y + height - 26, 35 / 2f, 35 / 2f, mouseX, mouseY) && consumeClick(0)) {
                            if (MusicPlayer.isPlaying)
                                MusicPlayer.pause();
                            else
                                MusicPlayer.play();
                        }

                        double duration = JLayerHelper.getDuration();
                        int minutes = (int) (duration * playProgress);
                        int seconds = (int) ((duration * playProgress - minutes) * 60);
                        String progress = minutes + ":" + seconds;
                        String total = (int) duration + ":" + (int) ((duration - (int) duration) * 60);
                        s14.drawString(progress, x + width / 2 - 80 - s14.getStringWidth(progress) - 2, y + height - 10, new Color(160, 160, 160,opacity).getRGB());
                        s14.drawString(total, x + width / 2 + 80 + 2, y + height - 10, new Color(160, 160, 160,opacity).getRGB());
                    }
                }
            }
        }

        mouseButton = -1;
    }

    private static int drawTracks(float x, float width, int mouseX, int mouseY, int finalY, int tX, int tY, ArrayList<Track> likedTracks) {
        for (Track track : likedTracks) {
            drawTrack(track, "", track.getName(), x + 20 + tX, finalY + tY, mouseX, mouseY, new Color(0, 0, 0, 100));
            tX += 70;
            if (tX >= width - 40) {
                tX = 0;
                tY += 90;
            }
        }
        return tY;
    }


    private static void drawTrack(Track track, String name, String desc, float x, float y, int mouseX, int mouseY, Color color) {
        if (Render2DUtils.isHovered(x, y, 60, 82, mouseX, mouseY) && consumeClick(0)) {
            currentTrack = track;
        }

        Render2DUtils.drawOptimizedRoundedRect(x, y, 60, 82, track.getDominateColor() == null ? new Color(202, 112, 112) : track.getDominateColor());
        if (track != null && !track.getPicUrl().isEmpty()) {
            Render2DUtils.drawImage(track.getCoverResource(), x, y, 60, 60, -1);
        } else {
            Render2DUtils.drawOptimizedRoundedRect(x, y, 60, 60, color);
        }
        FPSMaster.fontManager.s18.drawString(name, x + 5, y + 5, track.getFontColor().getRGB());
        String text = FPSMaster.fontManager.s16.trimString(desc, 60, false);
        FPSMaster.fontManager.s14.drawString(text, x + 2, y + 62, track.getFontColor().getRGB());
        if (desc.length() > text.length()) {
            String substring = desc.substring(text.length());
            substring = FPSMaster.fontManager.s16.trimString(substring, 60, false);
            FPSMaster.fontManager.s14.drawString(substring, x + 2, y + 72, track.getFontColor().getRGB());
        }

    }


    public static void keyTyped(char typedChar, int keyCode) {
        searchField.textboxKeyTyped(typedChar, keyCode);
    }


    static int mouseButton = -1;

    public static void mouseClicked(int mouseX, int mouseY, int btn) {
        mouseButton = btn;
        searchField.mouseClicked(mouseX, mouseY, btn);
    }

    public static boolean consumeClick(int btn) {
        boolean temp = mouseButton == btn;
        if (temp)
            mouseButton = -1;
        return temp;
    }


    private static void reloadImg() {
        loginThread = new Thread(() -> {
            while (isWaitingLogin) {
                try {
                    JsonObject loginStatus = MusicWrapper.getLoginStatus(key);
                    loginCode = loginStatus.get("code").getAsInt();
                    if (loginCode == 802) {
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
                    if (loginCode == 803) {
                        // parse cookie
                        String asString = loginStatus.get("cookie").getAsString();
                        String result = "MUSIC_U=" + subString(asString, "MUSIC_U=", ";") + "; " + "NMTID=" + subString(asString, "NMTID=", ";");
                        NeteaseApi.cookies = result;

                        try {
                            FileUtils.saveTempValue("cookies", NeteaseApi.cookies);
                            isWaitingLogin = false;
                            profile = MusicWrapper.getProfile();
                            return;
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
            System.out.println(key);
            if (key == null) return;
            String base64 = MusicWrapper.getQRCodeImg(key);
            // render base64 img data
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
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

    private static String subString(String input, String prefix, String suffix) {
        int startIndex = input.indexOf(prefix);
        if (startIndex != -1) {
            int endIndex = input.indexOf(suffix, startIndex + prefix.length());
            if (endIndex != -1) {
                return input.substring(startIndex + prefix.length(), endIndex);
            }
        }
        return "";
    }

}
