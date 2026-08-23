package top.fpsmaster.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.hud.HudEditorBridge;
import top.fpsmaster.prism.hud.SharedHudEditor;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.GuiScale;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Edge host for the shared Prism HUD editor. */
public final class HudEditorScreen extends ScaledGuiScreen {
    private final SharedHudEditor editor = new SharedHudEditor();
    private final EdgeHudEditorBridge bridge = new EdgeHudEditorBridge();

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        for (Component component : FPSMaster.componentsManager.components) {
            if (component.shouldDisplay()) component.measurePreview();
        }
        editor.draw(EdgeUi.frame(), bridge);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            editor.close(bridge);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private final class EdgeHudEditorBridge implements HudEditorBridge {
        public String i18n(String key) {
            String translated = FPSMaster.i18n.get(key);
            if (!key.equals(translated)) return translated;
            if ("hud.editor.title".equals(key)) return "HUD Editor";
            if ("hud.editor.done".equals(key)) return "Done";
            return key;
        }

        public List<Item> items() {
            float[] hud = GuiScale.getFixedBounds();
            Viewport viewport = viewport(hud[0], hud[1]);
            List<Item> result = new ArrayList<Item>();
            ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
            for (Component component : FPSMaster.componentsManager.components) {
                if (!component.shouldDisplay() || component.width <= 0f || component.height <= 0f) continue;
                float[] pos = component.getRealPosition(resolution);
                result.add(new Item(component.mod.name, label(component), viewport.x + pos[0] * viewport.scale,
                        viewport.y + pos[1] * viewport.scale, component.width * viewport.scale,
                        component.height * viewport.scale,
                        component.scale, Component.MIN_SCALE, Component.MAX_SCALE, component.allowScale));
            }
            return result;
        }

        public void paintPreview(String id, float x, float y, float scale) {
            Component component = find(id);
            if (component == null) return;
            float[] hud = GuiScale.getFixedBounds();
            Viewport viewport = viewport(hud[0], hud[1]);
            float oldScale = component.scale;
            component.scale = scale * viewport.scale;
            component.drawPreview(x, y);
            component.scale = oldScale;
        }

        public void setPlacement(String id, float x, float y, float scale, float surfaceWidth, float surfaceHeight) {
            Component component = find(id);
            if (component == null) return;
            float[] hud = GuiScale.getFixedBounds();
            Viewport viewport = viewport(hud[0], hud[1]);
            component.setRealPosition((x - viewport.x) / viewport.scale,
                    (y - viewport.y) / viewport.scale,
                    hud[0], hud[1], scale);
        }

        public void disable(String id) {
            Component component = find(id);
            if (component != null) component.mod.set(false);
        }

        public void save() {
            try {
                FPSMaster.configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
            } catch (FileException exception) {
                ClientLogger.error("Failed to save HUD layout: " + exception.getMessage());
            }
        }

        public void close() {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.displayGuiScreen(null);
            minecraft.setIngameFocus();
        }

        private Component find(String id) {
            for (Component component : FPSMaster.componentsManager.components) {
                if (component.mod.name.equals(id)) return component;
            }
            return null;
        }

        private String label(Component component) {
            String translated = FPSMaster.i18n.get(component.mod.name.toLowerCase());
            return translated.equals(component.mod.name.toLowerCase()) ? component.mod.name : translated;
        }

        private Viewport viewport(float hudWidth, float hudHeight) {
            float contentHeight = guiHeight - SharedHudEditor.CONTENT_TOP;
            float scale = Math.min(guiWidth / hudWidth, contentHeight / hudHeight);
            return new Viewport((guiWidth - hudWidth * scale) / 2f,
                    SharedHudEditor.CONTENT_TOP + (contentHeight - hudHeight * scale) / 2f, scale);
        }
    }

    private static final class Viewport {
        final float x;
        final float y;
        final float scale;

        Viewport(float x, float y, float scale) {
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }
}
