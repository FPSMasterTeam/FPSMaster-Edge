package top.fpsmaster.ui.custom;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.ui.custom.impl.*;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.gui.GuiScale;

import java.util.ArrayList;

public class ComponentsManager {
    // List to hold all components
    public final ArrayList<Component> components = new ArrayList<>();

    // Variable to track drag lock state
    public String dragLock = "";

    // Initialize all components
    public void init() {
        components.add(new FPSDisplayComponent());
        components.add(new ArmorDisplayComponent());
        components.add(new ScoreboardComponent());
        components.add(new PotionDisplayComponent());
        components.add(new CPSDisplayComponent());
        components.add(new KeystrokesComponent());
        components.add(new ReachDisplayComponent());
        components.add(new ComboDisplayComponent());
        components.add(new InventoryDisplayComponent());
        components.add(new TargetHUDComponent());
        components.add(new PlayerDisplayComponent());
        components.add(new PingDisplayComponent());
        components.add(new CoordsDisplayComponent());
        components.add(new ModsListComponent());
        components.add(new MiniMapComponent());
        components.add(new SprintComponent());
        components.add(new ItemCountDisplayComponent());
    }

    // Get a component by its class type
    public Component getComponent(Class<? extends InterfaceModule> clazz) {
        return components.stream()
                .filter(component -> component.mod.getClass() == clazz)
                .findFirst()
                .orElse(null);
    }

    // Draw all components on the screen
    public void draw(int mouseX, int mouseY) {
        GL11.glPushMatrix();

        // Adjust mouse coordinates if fixed scale is enabled
        if (ClientSettings.fixedScale.getValue()) {
            ScaledResolution sr = new ScaledResolution(Utility.mc);
            int scaleFactor = ClientSettings.fixedScale.getValue() ? sr.getScaleFactor() : 2;
            float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
            float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;

            mouseX = mouseX * scaleFactor / 2;
            mouseY = mouseY * scaleFactor / 2;

            GuiScale.fixScale();
        }

        // Draw all components that should be displayed
        int finalMouseX = mouseX;
        int finalMouseY = mouseY;
        components.forEach(component -> {
            if (component.shouldDisplay()) {
                try {
                    component.display(finalMouseX, finalMouseY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        GL11.glPopMatrix();
    }
}



