package top.fpsmaster.ui.notification;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.prism.overlay.NotificationCenter;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.gui.GuiScale;

/** Edge event adapter; notification behavior and rendering live in Prism. */
public final class NotificationManager {
    private static final NotificationCenter CENTER = new NotificationCenter();

    private NotificationManager() {
    }

    public static void addNotification(String title, String description, float duration) {
        addNotification(title, description, NotificationCenter.Type.INFO, duration);
    }

    public static void addNotification(String title, String description, NotificationCenter.Type type, float duration) {
        CENTER.add(title, description, type, duration);
    }

    public static void drawNotifications() {
        float[] bounds = GuiScale.getFixedBounds();
        GL11.glPushMatrix();
        GuiScale.fixScale();
        EdgeUi.beginOverlay(bounds[0], bounds[1]);
        try {
            CENTER.paint(EdgeUi.frame());
        } finally {
            EdgeUi.end();
            GL11.glPopMatrix();
        }
    }
}


