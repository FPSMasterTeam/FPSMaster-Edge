package top.fpsmaster.ui.custom;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.ui.custom.impl.*;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.gui.GuiScale;
import top.fpsmaster.modules.logger.ClientLogger;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ComponentsManager {
    // List to hold all components
    public final ArrayList<Component> components = new ArrayList<>();

    // Variable to track drag lock state
    public String dragLock = "";

    // Initialize all components
    public void init() {
        addComponentSafely("FPSDisplayComponent", FPSDisplayComponent::new);
        addComponentSafely("ArmorDisplayComponent", ArmorDisplayComponent::new);
        addComponentSafely("ScoreboardComponent", ScoreboardComponent::new);
        addComponentSafely("PotionDisplayComponent", PotionDisplayComponent::new);
        addComponentSafely("CPSDisplayComponent", CPSDisplayComponent::new);
        addComponentSafely("KeystrokesComponent", KeystrokesComponent::new);
        addComponentSafely("ReachDisplayComponent", ReachDisplayComponent::new);
        addComponentSafely("ComboDisplayComponent", ComboDisplayComponent::new);
        addComponentSafely("InventoryDisplayComponent", InventoryDisplayComponent::new);
        addComponentSafely("TargetHUDComponent", TargetHUDComponent::new);
        addComponentSafely("PlayerDisplayComponent", PlayerDisplayComponent::new);
        addComponentSafely("PingDisplayComponent", PingDisplayComponent::new);
        addComponentSafely("CoordsDisplayComponent", CoordsDisplayComponent::new);
        addComponentSafely("ModsListComponent", ModsListComponent::new);
        addComponentSafely("MiniMapComponent", MiniMapComponent::new);
        addComponentSafely("SprintComponent", SprintComponent::new);
        addComponentSafely("BlockIndicatorComponent", BlockIndicatorComponent::new);
        addComponentSafely("ItemCountDisplayComponent", ItemCountDisplayComponent::new);
    }

    private void addComponentSafely(String name, Supplier<Component> supplier) {
        try {
            components.add(supplier.get());
        } catch (Throwable throwable) {
            ClientLogger.error("Failed to initialize component: " + name);
        }
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
        int scaleFactor = new net.minecraft.client.gui.ScaledResolution(Utility.mc).getScaleFactor();

        mouseX = mouseX * scaleFactor / 2;
        mouseY = mouseY * scaleFactor / 2;

        GuiScale.fixScale();

        // Draw all components that should be displayed
        int finalMouseX = mouseX;
        int finalMouseY = mouseY;
        components.forEach(component -> {
            if (component.shouldDisplay()) {
                try {
                    component.display(finalMouseX, finalMouseY);
                } catch (Exception e) {
                    ClientLogger.error("Failed to render component: " + component.mod.name);
                }
            }
        });

        GL11.glPopMatrix();
    }
}



