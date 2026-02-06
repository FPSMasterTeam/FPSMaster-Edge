package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.MiniMap;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.minimap.XaeroMinimap;
import top.fpsmaster.ui.minimap.animation.MinimapAnimation;
import top.fpsmaster.ui.minimap.interfaces.InterfaceHandler;
import top.fpsmaster.utils.render.Render2DUtils;

import java.io.IOException;

public class MiniMapComponent extends Component {

    private boolean loadedMinimap = false;
    private final XaeroMinimap minimap = new XaeroMinimap();

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
                178f / 2f,
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
        float partialTicks = ((IMinecraft) Minecraft.getMinecraft()).arch$getTimer().renderPartialTicks;
        InterfaceHandler.drawInterfaces(width, height, partialTicks);
        MinimapAnimation.tick();
        GL11.glPopMatrix();
        Render2DUtils.fixScale();
    }
}
