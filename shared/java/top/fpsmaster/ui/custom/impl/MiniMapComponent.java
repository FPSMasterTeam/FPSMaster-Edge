package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.MiniMap;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.minimap.XaeroMinimap;
import top.fpsmaster.ui.minimap.animation.MinimapAnimation;
import top.fpsmaster.ui.minimap.interfaces.InterfaceHandler;
import top.fpsmaster.utils.render.Render2DUtils;

import java.io.IOException;

public class MiniMapComponent extends Component {

    private boolean loadedMinimap = false;
    private XaeroMinimap minimap = new XaeroMinimap();

    public MiniMapComponent() {
        super(MiniMap.class);
        this.y = 0.3f;
        this.width = 75f;
        this.height = 75f;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);

        Render2DUtils.drawImage(
            new ResourceLocation("client/gui/minimapbg.png"),
            x + width / 2 - 179 / 4f,
            y + width / 2 - 179 / 4f,
            179f / 2f,
            179f / 2f,
            -1
        );

        GL11.glPushMatrix();
        if (!loadedMinimap) {
            loadedMinimap = true;
            try {
                minimap.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
        InterfaceHandler.drawInterfaces(width, height, ProviderManager.timerProvider.getRenderPartialTicks());
        MinimapAnimation.tick();
        GL11.glPopMatrix();
        Render2DUtils.fixScale();

        Render2DUtils.drawRect(x + width / 2 - 1, y + height / 2 - 1, 2f, 2f, -1);
    }
}
