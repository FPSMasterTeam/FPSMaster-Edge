package top.fpsmaster.ui.notification;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.math.animation.Animation;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class Notification {
    private final String title;
    private final String description;
    private final Type type;
    private final float displayTime; // Time for notification to stay on screen in seconds

    public Animation animation;
    private float positionY;
    private final float width;
    private final float height;
    private long startTime = -1L;

    public Notification(String title, String description, Type type, float time) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.displayTime = time;

        // Initialize the animation for the notification appearance
        this.animation = new Animation();
        this.animation.start(100.0, 0.0, 0.3f, top.fpsmaster.utils.math.animation.Type.EASE_IN_OUT_QUAD);

        // Calculate the size of the notification box based on title and description width
        this.width = 30f + Math.max(FPSMaster.fontManager.s16.getStringWidth(title), FPSMaster.fontManager.s16.getStringWidth(description));
        this.height = 30f;
    }

    // Draws the notification on screen
    public void draw(float x, float y) {
        // Update animation progress
        animation.update();

        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }

        if (animation.value == 0.0) {
            if (System.currentTimeMillis() - startTime > displayTime * 1000) {
                animation.start(0.0, 100.0, 0.3f, top.fpsmaster.utils.math.animation.Type.EASE_IN_OUT_QUAD);
            }
        }

        // Smooth transition for Y position
        this.positionY = (float) AnimationUtils.base(this.positionY, y, 0.2);

        // Draw the notification background with animation effect
        Render2DUtils.drawOptimizedRoundedRect(
                (float) (x - (width * animation.value / 100f)),
                positionY,
                width,
                height,
                new Color(0, 0, 0, 100)
        );

        // Draw the foreground (white bar) to indicate the notification duration
        Render2DUtils.drawOptimizedRoundedRect(
                (float) (x - (width * animation.value / 100f)),
                positionY,
                width * Math.min(1f, (float) (System.currentTimeMillis() - startTime) / (1000f * displayTime)),
                height,
                new Color(255, 255, 255, 100)
        );

        // Draw the icon for the notification type
        Render2DUtils.drawImage(
                new ResourceLocation("client/textures/noti/" + type.name().toLowerCase() + ".png"),
                (float) (x - (width * animation.value / 100f) + 4),
                positionY + 8,
                14f,
                14f,
                -1
        );

        // Draw the title and description text
        FPSMaster.fontManager.s18.drawStringWithShadow(
                title,
                (float) (x - (width * animation.value / 100f) + 20),
                positionY + 4,
                -1
        );

        FPSMaster.fontManager.s16.drawString(
                description,
                (float) (x - (width * animation.value / 100f) + 20),
                positionY + 15,
                new Color(200, 200, 200).getRGB()
        );
    }

    // Enum for notification types
    public enum Type {
        INFO,
        ERROR,
        WARNING
    }
}
