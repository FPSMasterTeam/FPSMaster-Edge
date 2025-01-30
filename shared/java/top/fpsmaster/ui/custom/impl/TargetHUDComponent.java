package top.fpsmaster.ui.custom.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.TargetDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.interfaces.ProviderManager;

import java.awt.Color;

public class TargetHUDComponent extends Component {

    private float animation = 0f;
    private float healthWidth = 0f;
    private ColorAnimation colorAnimation = new ColorAnimation();

    public TargetHUDComponent() {
        super(TargetDisplay.class);
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);

        if (TargetDisplay.targetHUD.getMode() != 0) return;

        // Get the target or player if chat is open
        EntityPlayer target1 = TargetDisplay.target;
        if (Utility.mc.ingameGUI.getChatGUI().getChatOpen()) {
            target1 = ProviderManager.mcProvider.getPlayer();
        }

        if (target1 == null) return;

        // Set width and height
        width = (30 + FPSMaster.fontManager.s16.getStringWidth(((Entity) target1).getDisplayName().getFormattedText()));
        height = 30f;

        // Update animation based on target's health and last hit time
        animation = (TargetDisplay.target.isDead || (System.currentTimeMillis() - TargetDisplay.lastHit > 5000 && target1 != ProviderManager.mcProvider.getPlayer()))
            ? (float) AnimationUtils.base(animation, 0.0, 0.1) 
            : (float) AnimationUtils.base(animation, 80.0, 0.1);

        // Health width
        float health = ((EntityPlayer) target1).getHealth();
        float maxHealth = ((EntityPlayer) target1).getMaxHealth();
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
            FPSMaster.fontManager.s16.drawStringWithShadow(((Entity) target1).getDisplayName().getFormattedText(), x + 27, y + 5, -1);
            Render2DUtils.drawPlayerHead(target1, x + 5, y + 5, 20, 20);
        }
    }
}
