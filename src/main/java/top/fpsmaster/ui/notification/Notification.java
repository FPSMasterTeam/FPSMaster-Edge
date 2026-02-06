package top.fpsmaster.ui.notification;

import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.Easings;

import java.awt.*;

public class Notification {
    private final String title;
    private final String description;
    private final Type type;
    private final float displayTime; // Time for notification to stay on screen in seconds

    public Animator animation;
    private float positionY;
    private final float width;
    private final float height;
    private long startTime = -1L;
    private final AnimClock animClock = new AnimClock();
    private final Animator yAnimation = new Animator();

    public Notification(String title, String description, Type type, float time) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.displayTime = time;

        // Initialize the animation for the notification appearance
        this.animation = new Animator();
        this.animation.start(100.0, 0.0, 0.3f, Easings.QUAD_IN_OUT);
        this.yAnimation.set(0.0);

        // Calculate the size of the notification box based on title and description width
        this.width = 30f + Math.max(FPSMaster.fontManager.s16.getStringWidth(title), FPSMaster.fontManager.s16.getStringWidth(description));
        this.height = 30f;
    }

    // Draws the notification on screen
    public void draw(float x, float y) {
        double dt = animClock.tick();
        animation.update(dt);

        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }

        if (animation.get() == 0.0) {
            if (System.currentTimeMillis() - startTime > displayTime * 1000) {
                animation.animateTo(100.0, 0.3f, Easings.QUAD_IN_OUT);
            }
        }

        yAnimation.animateTo(y, 0.2f, Easings.QUAD_OUT);
        yAnimation.update(dt);
        this.positionY = (float) yAnimation.get();

        // Draw the notification background with animation effect
        Rects.rounded(
                (float) (x - (width * animation.get() / 100f)),
                positionY,
                width,
                height,
                new Color(0, 0, 0, 100)
        );

        // Draw the foreground (white bar) to indicate the notification duration
        Rects.rounded(
                (float) (x - (width * animation.get() / 100f)),
                positionY,
                width * Math.min(1f, (float) (System.currentTimeMillis() - startTime) / (1000f * displayTime)),
                height,
                new Color(255, 255, 255, 100)
        );

        // Draw the icon for the notification type
        Images.draw(
                new ResourceLocation("client/textures/noti/" + type.name().toLowerCase() + ".png"),
                (float) (x - (width * animation.get() / 100f) + 4),
                positionY + 8,
                14f,
                14f,
                -1
        );

        // Draw the title and description text
        FPSMaster.fontManager.s18.drawStringWithShadow(
                title,
                (float) (x - (width * animation.get() / 100f) + 20),
                positionY + 4,
                -1
        );

        FPSMaster.fontManager.s16.drawString(
                description,
                (float) (x - (width * animation.get() / 100f) + 20),
                positionY + 15,
                new Color(200, 200, 200).getRGB()
        );
    }

    public boolean isFinished() {
        return !animation.isRunning() && animation.get() == 100.0;
    }

    // Enum for notification types
    public enum Type {
        INFO,
        ERROR,
        WARNING
    }
}




