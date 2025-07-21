package top.fpsmaster.ui.custom.impl;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.features.impl.interfaces.MusicOverlay;
import top.fpsmaster.modules.music.AbstractMusic;
import top.fpsmaster.modules.music.MusicPlayer;
import top.fpsmaster.modules.music.netease.Music;
import top.fpsmaster.ui.click.music.NewMusicPanel;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class MusicComponent extends Component {

    private static float songProgress = 0f;

    public MusicComponent() {
        super(MusicOverlay.class);
        x = 0.01f;
        y = 0.01f;
        position = Position.RT;
    }

    private void drawSong(float x, float y, float width, float height) {
        Music current = NewMusicPanel.playing;
        if (current == null) {
            return;
        }
        drawRect(x, y, width, height, mod.backgroundColor.getColor());
        drawRect(x, y, songProgress, height, MusicOverlay.progressColor.getColor());
        songProgress = (float) AnimationUtils.base(songProgress, (6 + (width - 6) * MusicPlayer.getPlayProgress()), 0.1);

        if (current.isLoadedImage) {
            Render2DUtils.drawImage(
                    new ResourceLocation("music/netease/" + current.id),
                    x + 5,
                    y + 5,
                    height - 10,
                    height - 10,
                    -1
            );
        }

        drawString(18, current.name, x + 40, y + 6, new Color(234, 234, 234).getRGB());
        drawString(16, current.author, x + 40, y + 18, new Color(162, 162, 162).getRGB());
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);

        Music current = NewMusicPanel.playing;
        if (current != null) {
            float width = Math.max(
                    getStringWidth(18, current.name), getStringWidth(18, current.author)
            );
            drawSong(x, y, width + 60, 40f);
            this.width = width + 60;
            height = 40f;
        }
    }
}
