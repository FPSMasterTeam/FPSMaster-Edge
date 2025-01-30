package top.fpsmaster.features.impl.optimizes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.interfaces.ProviderManager;

public class OldAnimations extends Module {

    public static BooleanSetting noShield = new BooleanSetting("NoShield", true);
    public static BooleanSetting animationSneak = new BooleanSetting("AnimationSneak", true);
    public static BooleanSetting oldBlock = new BooleanSetting("OldBlock", true);
    public static BooleanSetting oldRod = new BooleanSetting("OldRod", true);
    public static BooleanSetting oldBow = new BooleanSetting("OldBow", true);
    public static BooleanSetting oldSwing = new BooleanSetting("OldSwing", true);
    public static BooleanSetting oldUsing = new BooleanSetting("OldUsing", true);
    public static BooleanSetting oldDamage = new BooleanSetting("OldDamage", true);
    public static BooleanSetting blockHit = new BooleanSetting("BlockHit", true);
    public static NumberSetting x = new NumberSetting("X", 0, -1, 1, 0.01);
    public static NumberSetting y = new NumberSetting("Y", 0, -1, 1, 0.01);
    public static NumberSetting z = new NumberSetting("Z", 0, -1, 1, 0.01);

    public static boolean using = false;

    private static float eyeHeight = 0f;
    private static float lastEyeHeight = 0f;
    private static final float START_HEIGHT = 1.62f;
    private static final float END_HEIGHT = 1.54f;

    public OldAnimations() {
        super("OldAnimations", Category.OPTIMIZE);
        addSettings(noShield, animationSneak, oldRod, oldBow, oldSwing, oldDamage, oldUsing, blockHit, oldBlock, x, y, z);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    @Subscribe
    public void onTick(EventTick event) {
        lastEyeHeight = eyeHeight;

        EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
        if (thePlayer == null) {
            return;
        }
        if (thePlayer.isSneaking()) {
            eyeHeight = END_HEIGHT;
        } else if (!animationSneak.getValue()) {
            eyeHeight = START_HEIGHT;
        } else if (eyeHeight < START_HEIGHT) {
            float delta = START_HEIGHT - eyeHeight;
            delta *= 0.4f;
            eyeHeight = START_HEIGHT - delta;
        }
    }

    public static float getClientEyeHeight(float partialTicks) {
        if (!animationSneak.getValue()) {
            return eyeHeight;
        }

        return lastEyeHeight + (eyeHeight - lastEyeHeight) * partialTicks;
    }

    // Getter and Setter for 'using' and other fields if needed
    public static boolean isUsing() {
        return using;
    }

    public static void setUsing(boolean using) {
        OldAnimations.using = using;
    }
}
