package top.fpsmaster.modules.account;

import com.google.gson.JsonObject;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import scala.Int;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.awt.GifUtil;
import top.fpsmaster.utils.os.HttpRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static top.fpsmaster.utils.Utility.mc;

public class Cosmetic {
    public int id;
    public String name;
    public String img;
    public String category;
    public double price;
    public boolean available;
    public String resource;
    public boolean loaded;
    public Integer frame = 0;
    public long frameTime = 0;
    public List<GifUtil.FrameData> frames = new ArrayList<>();

    public Cosmetic() {
    }

    public void load() {
        if (mc.theWorld == null) return;
        FPSMaster.async.runnable(() -> {
            if (resource.endsWith(".png")) {
                ResourceLocation textureLocation = new ResourceLocation("ornaments/" + id + "_resource");
                ThreadDownloadImageData downloadImageData = new ThreadDownloadImageData(null, null, textureLocation, null);
                try {
                    downloadImageData.setBufferedImage(HttpRequest.downloadImage(resource));
                    mc.getTextureManager().loadTexture(textureLocation, downloadImageData);
                } catch (IOException ignored) {
                }
            } else if (resource.endsWith(".gif")) {
                try {
                    InputStream inputStream = HttpRequest.downloadFile(resource);
                    frames.clear();
                    frames = GifUtil.convertGifToPng(inputStream);
                    for (GifUtil.FrameData frame : frames) {
                        ResourceLocation textureLocation = new ResourceLocation("ornaments/" + id + "_resource_" + frames.indexOf(frame));
                        ThreadDownloadImageData downloadImageData = new ThreadDownloadImageData(null, null, textureLocation, null);
                        downloadImageData.setBufferedImage(frame.image);
                        mc.getTextureManager().loadTexture(textureLocation, downloadImageData);
                    }
                    loaded = true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
