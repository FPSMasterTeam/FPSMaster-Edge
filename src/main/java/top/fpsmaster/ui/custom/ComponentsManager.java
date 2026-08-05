package top.fpsmaster.ui.custom;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.ui.custom.impl.*;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.render.gui.GuiScale;
import top.fpsmaster.benchmark.HudBreakdown;
import top.fpsmaster.modules.logger.ClientLogger;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ComponentsManager {
    // List to hold all components
    public final ArrayList<Component> components = new ArrayList<>();

    /** What a drag gesture is doing to {@link #dragTarget}. */
    public enum DragMode {
        MOVE,
        RESIZE
    }

    /**
     * The component currently being dragged, or {@code null}. Previously this was the module's name as
     * a String, which cost a string compare per component per frame and could collide when a component
     * fell back to a synthesised module. More importantly the "mouse released → unlock" rule was
     * evaluated inside each component's own display(), so if every HUD element happened to be hidden
     * mid-drag the lock was never cleared and nothing could be dragged again.
     */
    public Component dragTarget;

    public DragMode dragMode = DragMode.MOVE;

    // Initialize all components
    public void init() {
        addComponentSafely("FPSDisplayComponent", FPSDisplayComponent::new);
        addComponentSafely("SaturationDisplayComponent", SaturationDisplayComponent::new);
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
        addComponentSafely("PerformanceHudComponent", PerformanceHudComponent::new);
        addComponentSafely("ModsListComponent", ModsListComponent::new);
        addComponentSafely("MiniMapComponent", MiniMapComponent::new);
        addComponentSafely("SprintComponent", SprintComponent::new);
        addComponentSafely("ToggleSneakComponent", ToggleSneakComponent::new);
        addComponentSafely("BlockIndicatorComponent", BlockIndicatorComponent::new);
        addComponentSafely("PlayTimeComponent", PlayTimeComponent::new);
        addComponentSafely("ClockDisplayComponent", ClockDisplayComponent::new);
        addComponentSafely("ServerAddressDisplayComponent", ServerAddressDisplayComponent::new);
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

    /** A component failing this many frames in a row is switched off rather than left to spin. */
    private static final int DISABLE_AFTER_FAILURES = 60;

    /**
     * Per-frame failures are swallowed on purpose: {@code mc.thePlayer}/{@code theWorld} go null while
     * changing worlds and several components dereference them unguarded, so without this one NPE would
     * take down the whole EventRender2D dispatch. What must not happen is logging the same line 60
     * times a second, so output is rate-limited and the exception itself is finally recorded.
     */
    private void onComponentFailure(Component component, String phase, Throwable throwable) {
        int failures = ++component.renderFailures;
        // Full detail for the first few, then only on powers of two.
        if (failures <= 3 || Integer.bitCount(failures) == 1) {
            ClientLogger.error("Failed to " + phase + " component " + component.mod.name
                    + " (failure #" + failures + ")", throwable);
        }
        if (failures == DISABLE_AFTER_FAILURES) {
            ClientLogger.error("Disabling component " + component.mod.name
                    + " after " + failures + " consecutive failures");
            component.mod.set(false);
        }
        // An exception can leave scissor/stencil/blend enabled and bleed into the rest of the frame.
        resetRenderState();
    }

    private void resetRenderState() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Sizes every visible component for this frame. Must run before anything reads width/height —
     * anchoring, hover testing, drag clamping and the blur mask all do, and all of them used to see
     * the previous frame's values. Called once per frame from GlobalListener, ahead of both the blur
     * mask pass and the draw pass.
     */
    public void measureAll() {
        components.forEach(component -> {
            if (component.shouldDisplay()) {
                try {
                    component.measure();
                } catch (Throwable throwable) {
                    onComponentFailure(component, "measure", throwable);
                }
            }
        });
    }

    public void drawBackgroundMasks() {
        GL11.glPushMatrix();
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(Utility.mc);
        GuiScale.fixScale();
        components.forEach(component -> {
            if (component.shouldDisplay()) {
                try {
                    component.drawBlurMask(sr);
                } catch (Throwable throwable) {
                    onComponentFailure(component, "mask", throwable);
                }
            }
        });
        GL11.glPopMatrix();
    }

    // Draw all components on the screen
    public void draw(int mouseX, int mouseY) {
        GL11.glPushMatrix();

        // Adjust mouse coordinates if fixed scale is enabled.
        // Build the ScaledResolution once per frame and reuse it for every component's
        // position math, instead of each component allocating its own (previously twice each).
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(Utility.mc);
        int scaleFactor = sr.getScaleFactor();

        mouseX = mouseX * scaleFactor / 2;
        mouseY = mouseY * scaleFactor / 2;

        GuiScale.fixScale();

        // Releasing the button ends any drag — decided once per frame here rather than inside each
        // component, so it holds even when no component is visible to run the check.
        if (!org.lwjgl.input.Mouse.isButtonDown(0)) {
            dragTarget = null;
        }

        // Draw all components that should be displayed
        int finalMouseX = mouseX;
        int finalMouseY = mouseY;
        components.forEach(component -> {
            if (component.shouldDisplay()) {
                long started = HudBreakdown.enabled() ? System.nanoTime() : 0L;
                try {
                    component.display(sr, finalMouseX, finalMouseY);
                    component.renderFailures = 0;
                } catch (Throwable throwable) {
                    onComponentFailure(component, "render", throwable);
                }
                if (started != 0L) {
                    HudBreakdown.record(component.mod.name, System.nanoTime() - started);
                }
            }
        });

        if (dragTarget != null) {
            if (!dragTarget.shouldDisplay()) {
                dragTarget = null;
            } else {
                // Alignment guides go on top of everything so the dragged component cannot occlude them.
                dragTarget.drawGuides();
            }
        }

        GL11.glPopMatrix();
    }
}



