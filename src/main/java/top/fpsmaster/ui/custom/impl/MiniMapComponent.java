package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.utils.render.draw.Images;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.MiniMap;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.minimap.XaeroMinimap;
import top.fpsmaster.ui.minimap.animation.MinimapAnimation;
import top.fpsmaster.ui.minimap.interfaces.InterfaceHandler;
import top.fpsmaster.utils.render.gui.GuiScale;

import java.io.IOException;

public class MiniMapComponent extends Component {

    private boolean loadedMinimap = false;
    private final XaeroMinimap minimap = new XaeroMinimap();

    public MiniMapComponent() {
        super(MiniMap.class);
        this.y = 0.3f;
        this.width = 75f;
        this.height = 75f;
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);

        // Drawn raw rather than through drawRect (it is a texture, not a panel), so scale is applied
        // to both the offset and the size here.
        Images.draw(
                new ResourceLocation("client/gui/minimapbg.png"),
                x + (width / 2 - 179 / 4f) * scale,
                y + (width / 2 - 179 / 4f) * scale,
                179f / 2f * scale,
                178f / 2f * scale,
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
        InterfaceHandler.drawInterfaces(width * scale, height * scale, partialTicks);
        MinimapAnimation.tick();
        GL11.glPopMatrix();
        GuiScale.fixScale();
    }
}




