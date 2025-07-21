package top.fpsmaster.modules.music;

import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.modules.music.netease.Music;
import top.fpsmaster.modules.music.netease.deserialize.MusicWrapper;
import top.fpsmaster.utils.awt.KMeansUtil;
import top.fpsmaster.utils.os.HttpRequest;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;

import static top.fpsmaster.utils.Utility.mc;

public class Track {
    Long id;
    String name;
    String picUrl;
    Color dominateColor;
    Color fontColor = Color.WHITE;
    LinkedList<AbstractMusic> musics = new LinkedList<>();
    ResourceLocation coverResource;
    boolean loaded = false;

    public Track(Long id, String name, String picUrl) {
        this.id = id;
        this.name = name;
        this.picUrl = picUrl;
    }

    public Color getDominateColor() {
        return dominateColor;
    }

    public void setDominateColor(Color dominateColor) {
        this.dominateColor = dominateColor;
    }

    public Color getFontColor() {
        return fontColor;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    public void loadTrack() {
        coverResource = new ResourceLocation("music/track/" + id);
        ThreadDownloadImageData downloadImageData = new ThreadDownloadImageData(null, null, coverResource, null);
        try {
            BufferedImage bufferedImageIn = HttpRequest.downloadImage(picUrl);
            downloadImageData.setBufferedImage(bufferedImageIn);
            mc.getTextureManager().loadTexture(coverResource, downloadImageData);
            dominateColor = KMeansUtil.getOneDominantColor(bufferedImageIn);
            float[] hsb = new float[3];
            Color.RGBtoHSB(dominateColor.getRed(), dominateColor.getGreen(), dominateColor.getBlue(), hsb);
            if (hsb[2] < 0.5) {
                fontColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1] * 0.1f, 0.8f));
            } else {
                fontColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1] * 0.1f, 0.2f));
            }
        } catch (Exception ignored) {
        }
    }

    public void loadMusic() {
        if (musics == null || musics.isEmpty()) {
            PlayList playList = MusicWrapper.searchList(id.toString());
            musics = playList.getMusics();
            loaded = true;
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public ResourceLocation getCoverResource() {
        return coverResource;
    }

    public void setCoverResource(ResourceLocation coverResource) {
        this.coverResource = coverResource;
    }

    public LinkedList<AbstractMusic> getMusics() {
        return musics;
    }

    public void setMusics(LinkedList<AbstractMusic> musics) {
        this.musics = musics;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }
}
