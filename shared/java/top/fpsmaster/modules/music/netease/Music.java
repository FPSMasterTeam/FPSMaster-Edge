package top.fpsmaster.modules.music.netease;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.music.AbstractMusic;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.modules.music.netease.deserialize.MusicWrapper;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.os.HttpRequest;

import java.io.File;

public class Music extends AbstractMusic {
    public boolean isLoadedImage = false;
    String imgURL;
    String musicURL;
    public String id;
    public static Thread downloadThread;
    public float downloadProgress;

    public Music(long id, String name, String artists, String picUrl) {
        this.name = name;
        this.author = artists;
        this.imgURL = picUrl;
        this.id = Long.toString(id);
        // Load music with a separate thread
        new Thread(this::loadMusic).start();
    }

    public void loadMusic() {
        try {
            if (Wrappers.world().getWorld() == null) return;
            File artist = new File(FileUtils.artists, FileUtils.fixName(name + "(" + id + ").png"));
            if (!artist.exists()) {
                HttpRequest.downloadFile(imgURL + "?param=90y90", artist.getAbsolutePath());
            }
            Minecraft.getMinecraft().getTextureManager()
                    .loadTexture(new ResourceLocation("music/netease/" + id), new ThreadDownloadImageData(artist, null, null, null));
            isLoadedImage = true;
        } catch (Exception e) {
            ClientLogger.error("music", "Load failed " + name + "(" + id + ")");
        }
    }

    @Override
    public void play() {
        MusicPlayer.stop();
        MusicWrapper.loadLyrics(this);
        File flac = new File(FileUtils.music, FileUtils.fixName(name + "(" + id + ").flac"));
        File mp3 = new File(FileUtils.music, FileUtils.fixName(name + "(" + id + ").mp3"));
        if (flac.exists() || mp3.exists()) {
            new Thread(() -> {
                if (flac.exists()) {
                    MusicPlayer.playFile(flac.getAbsolutePath());
                } else {
                    MusicPlayer.playFile(mp3.getAbsolutePath());
                }
                MusicPlayer.isPlaying = true;
            }).start();
        } else {
            downloadThread = new Thread(this::download);
            downloadThread.start();
        }
        MusicPlayer.startTime = System.currentTimeMillis();
    }

    private void download() {
        try {
            musicURL = MusicWrapper.getSongUrl(id);
            File download = musicURL.endsWith(".flac") ?
                    new File(FileUtils.music, FileUtils.fixName(name + "(" + id + ").flac")) :
                    new File(FileUtils.music, FileUtils.fixName(name + "(" + id + ").mp3"));
            if (!download.exists()) {
                HttpRequest.downloadFile(musicURL, download.getAbsolutePath(), (downloadedBytes, totalBytes) -> {
                    this.downloadProgress = downloadedBytes * 1.0f / totalBytes;
                });
                play();
            }
        } catch (Exception e) {
            ClientLogger.warn("Download failed " + name + "(" + id + ")");
        }
    }
}
