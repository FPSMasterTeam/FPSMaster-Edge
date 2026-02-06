package top.fpsmaster.ui.notification;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.utils.render.gui.GuiScale;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    // List to store active notifications
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    // Add a notification using a Notification object
    public static void addNotification(Notification notification) {
        notifications.add(notification);
    }

    // Add a notification with a title, description, and duration
    public static void addNotification(String title, String description, float duration) {
        notifications.add(new Notification(title, description, Notification.Type.INFO, duration));
    }

    // Draw all active notifications on the screen
    public static void drawNotifications() {
        GL11.glPushMatrix();
        GuiScale.fixScale();

        float yPosition = 20f;

        // Loop through all notifications and render them
        for (Notification notification : notifications) {
            notification.draw(0f, yPosition);

            // Remove notifications that are fully animated (i.e., animation value is 100)
            if (notification.isFinished()) {
                notifications.remove(notification);
            }

            yPosition += 40f;
        }

        GL11.glPopMatrix();
    }

    // Getters for notifications list (if needed)
    public static CopyOnWriteArrayList<Notification> getNotifications() {
        return notifications;
    }
}



