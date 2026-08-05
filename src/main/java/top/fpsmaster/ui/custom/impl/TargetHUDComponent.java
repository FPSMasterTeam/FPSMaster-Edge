package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.entity.AbstractClientPlayer;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.entity.player.EntityPlayer;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.TargetDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.math.anim.ColorAnimator;

import java.awt.*;

public class TargetHUDComponent extends Component {

    private float animation = 0f;
    private float healthPer = 0f;
    private final ColorAnimator colorAnimation = new ColorAnimator();

    public TargetHUDComponent() {
        super(TargetDisplay.class);
        allowScale = true;
    }

    @Override
    public boolean isVisibleForAlignment() {
        return super.isVisibleForAlignment() && TargetDisplay.target != null && animation > 0.05f;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        if (TargetDisplay.target == null)
            return;
        // Get the target or player if chat is open
        EntityPlayer target1 = TargetDisplay.target;
        if (Utility.mc.ingameGUI.getChatGUI().getChatOpen()) {
            target1 = Utility.mc.thePlayer;
        }
        if (target1 == null) return;
        // Set width and height
        String name = target1.getDisplayName().getFormattedText();
        if (name.length() > 20 && TargetDisplay.omit.getValue()) {
            name = name.substring(0, 20) + "..";
        }

        animation = (TargetDisplay.target.isDead || (System.currentTimeMillis() - TargetDisplay.lastHit > 5000 && target1 != Utility.mc.thePlayer))
                ? (float) AnimMath.base(animation, 0.0, 0.1)
                : (float) AnimMath.base(animation, 1, 0.1);

        float health = target1.getHealth();
        float maxHealth = target1.getMaxHealth();

        healthPer = (float) AnimMath.base(healthPer, (health / maxHealth), 0.1);

        if (TargetDisplay.targetHUD.getMode() == 0) {
            width = (30 + getStringWidth(16, name));
            height = 30f;

            // Set color based on health percentage
            if (health >= maxHealth * 0.8) {
                colorAnimation.base(new Color(50, 255, 55, (int) (animation * 80)));
            } else if (health > maxHealth * 0.5) {
                colorAnimation.base(new Color(255, 255, 55, (int) (animation * 80)));
            } else {
                colorAnimation.base(new Color(255, 55, 55, (int) (animation * 80)));
            }

            // Draw elements if animation is greater than 1
            if (animation > 0.05) {
                // Panel goes through drawRect so it honours the Background toggle and scale; the health
                // bar and head stay raw because they are content, not decoration, and must not vanish
                // when the user turns the background off. Their sizes are scaled explicitly.
                drawRect(x, y, width, height, new Color(0, 0, 0, (int) animation * 80));
                Rects.rounded(Math.round(x), Math.round(y), Math.round(healthPer * width * scale), Math.round(height * scale), colorAnimation.getColor());
                drawString(16, name, x + 27 * scale, y + 5 * scale, -1);
                Images.playerHead((AbstractClientPlayer) target1, x + 5 * scale, y + 5 * scale, Math.round(20 * scale), Math.round(20 * scale));
            }
        } else if (TargetDisplay.targetHUD.getValue() == 1) {
            width = (50 + getStringWidth(16, name));
            height = 40f;

            // Set color based on health percentage
            if (health >= maxHealth * 0.8) {
                colorAnimation.base(new Color(50, 255, 155, (int) (animation * 220)));
            } else if (health > maxHealth * 0.5) {
                colorAnimation.base(new Color(255, 255, 85, (int) (animation * 220)));
            } else {
                colorAnimation.base(new Color(255, 75, 75, (int) (animation * 220)));
            }

            // Draw elements if animation is greater than 1
            if (animation > 0.05) {
                drawRect(x, y, width, height, new Color(0, 0, 0, (int) (animation * 120)));
                Rects.roundedImage(Math.round(x + 10 * scale), Math.round(y + 30 * scale), Math.round(healthPer * (width - 20) * scale), Math.round(4 * scale), 2, colorAnimation.getColor());
                drawString(18, name, x + 24 * scale, y + 8 * scale, new Color(255, 255, 255, (int) (animation * 255)).getRGB());
                Images.playerHead((AbstractClientPlayer) target1, x + 10 * scale, y + 8 * scale, Math.round(12 * scale), Math.round(12 * scale));
            }
        }
    }
}


