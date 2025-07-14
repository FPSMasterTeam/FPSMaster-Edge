package top.fpsmaster.ui.custom.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.TargetDisplay;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class TargetHUDComponent extends Component {

    private float animation = 0f;
    private float healthWidth = 0f;
    private final ColorAnimation colorAnimation = new ColorAnimation();

    public TargetHUDComponent() {
        super(TargetDisplay.class);
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);

        if (TargetDisplay.targetHUD.getMode() != 0) return;
        if (TargetDisplay.target == null)
            return;
        // Get the target or player if chat is open
        EntityPlayer target1 = TargetDisplay.target;
        if (Utility.mc.ingameGUI.getChatGUI().getChatOpen()) {
            target1 = ProviderManager.mcProvider.getPlayer();
        }

        if (target1 == null) return;

        // Set width and height
        String name = target1.getDisplayName().getFormattedText();

        if (name.length() > 12 && TargetDisplay.omit.getValue()) {
            name = name.substring(0, 10) + "..";
        }
        width = (30 + FPSMaster.fontManager.s16.getStringWidth(name));
        height = 30f;

        // Update animation based on target's health and last hit time
        animation = (TargetDisplay.target.isDead || (System.currentTimeMillis() - TargetDisplay.lastHit > 5000 && target1 != ProviderManager.mcProvider.getPlayer()))
                ? (float) AnimationUtils.base(animation, 0.0, 0.1)
                : (float) AnimationUtils.base(animation, 80.0, 0.1);

        // Health width
        float health = target1.getHealth();
        float maxHealth = target1.getMaxHealth();
        healthWidth = (float) AnimationUtils.base(healthWidth, (health / maxHealth), 0.1);

        // Set color based on health percentage
        if (health >= maxHealth * 0.8) {
            colorAnimation.base(new Color(50, 255, 55, (int) animation));
        } else if (health > maxHealth * 0.5) {
            colorAnimation.base(new Color(255, 255, 55, (int) animation));
        } else {
            colorAnimation.base(new Color(255, 55, 55, (int) animation));
        }

        // Draw elements if animation is greater than 1
        if (animation > 1) {
            Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, new Color(0, 0, 0, (int) animation));
            Render2DUtils.drawOptimizedRoundedRect(x, y, healthWidth * width, height, colorAnimation.getColor());
            FPSMaster.fontManager.s16.drawStringWithShadow(name, x + 27, y + 5, -1);
            Render2DUtils.drawPlayerHead(target1, x + 5, y + 5, 20, 20);
        }
    }
}
