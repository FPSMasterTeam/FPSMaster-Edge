package top.fpsmaster.ui.click.music;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.music.PlayList;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class NewMusicPanel {

    private static Thread playThread;
    private static final PlayList playList = new PlayList();
    private static final PlayList displayList = new PlayList();


    public static void draw(float x, int y, float width, float height, int mouseX, int mouseY, int scaleFactor) {
        Render2DUtils.drawImage(new ResourceLocation("client/gui/music.png"), x + 12, y + 14, 75, 16, -1);
        FPSMaster.fontManager.s18.drawString("SuperSkidder", x + width - 80, y + 15, -1);


        Render2DUtils.drawOptimizedRoundedRect(x + 20, y + 50, 100, 60, new Color(225,70,70));
        Render2DUtils.drawOptimizedRoundedRect(x + 20, y + 90, 100, 20, new Color(0,0,0,100));
        FPSMaster.fontManager.s24.drawString("每日推荐", x + 25, y + 55, -1);
        FPSMaster.fontManager.s14.drawString("每日推荐，从『花日』听起", x + 25, y + 95, -1);

        Render2DUtils.drawOptimizedRoundedRect(x + 140, y + 50, 100, 60, new Color(113, 113, 113));
        Render2DUtils.drawOptimizedRoundedRect(x + 140, y + 90, 100, 20, new Color(0,0,0,100));
        FPSMaster.fontManager.s24.drawString("本地音乐", x + 145, y + 55, -1);
        FPSMaster.fontManager.s14.drawString("共检测到22首本地音乐", x + 145, y + 95, -1);

        FPSMaster.fontManager.s22.drawString("收藏歌单", x + 20, y + 125, -1);

    }

    public static void keyTyped(char typedChar, int keyCode) {

    }

    public static void mouseClicked(int mouseX, int mouseY, int mouseButton) {

    }
}
