package top.fpsmaster.features.impl.optimizes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.world.WorldServer;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.interfaces.ProviderManager;

import static top.fpsmaster.utils.Utility.mc;

public class OldAnimations extends Module {

    public static BooleanSetting noShield = new BooleanSetting("NoShield", true);
    public static BooleanSetting animationSneak = new BooleanSetting("AnimationSneak", true);
    public static BooleanSetting oldBlock = new BooleanSetting("OldBlock", true);
    public static ModeSetting animationMode = new ModeSetting("AnimationMode", 0, () -> oldBlock.getValue(), "Lunar", "1.7", "Swang", "Sigma", "Swank", "Swong", "Debug", "Luna", "Jigsaw", "Jello", "Push");
    public static BooleanSetting oldRod = new BooleanSetting("OldRod", true);
    public static BooleanSetting oldBow = new BooleanSetting("OldBow", true);
    public static BooleanSetting oldSwing = new BooleanSetting("OldSwing", true);
    public static BooleanSetting oldUsing = new BooleanSetting("OldUsing", true);
    public static BooleanSetting blockSwing = new BooleanSetting("BlockSwing", true);
    public static BooleanSetting oldDamage = new BooleanSetting("OldDamage", true);
    public static NumberSetting x = new NumberSetting("X", 0, -1, 1, 0.01);
    public static NumberSetting y = new NumberSetting("Y", 0, -1, 1, 0.01);
    public static NumberSetting z = new NumberSetting("Z", 0, -1, 1, 0.01);
    public static NumberSetting scale = new NumberSetting("Scale", 1, 0, 3, 0.01);

    public static boolean using = false;

    private static float eyeHeight = 0f;
    private static float lastEyeHeight = 0f;
    private static final float START_HEIGHT = 1.62f;
    private static final float END_HEIGHT = 1.54f;

    public OldAnimations() {
        super("OldAnimations", Category.OPTIMIZE);
        addSettings(noShield, animationSneak, oldRod, oldBow, oldSwing, blockSwing, oldDamage, oldUsing, oldBlock, animationMode, x, y, z);
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
        if (Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown() && thePlayer.isUsingItem() && blockSwing.getValue()) {
            swingItem();
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

    public void swingItem() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || stack.getItem() == null || !stack.getItem().onEntitySwing(mc.thePlayer, stack)) {
            if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt >= getArmSwingAnimationEnd() / 2 || mc.thePlayer.swingProgressInt < 0) {
                mc.thePlayer.swingProgressInt = -1;
                mc.thePlayer.isSwingInProgress = true;
                if (mc.thePlayer.worldObj instanceof WorldServer) {
                    ((WorldServer)mc.thePlayer.worldObj).getEntityTracker().sendToAllTrackingEntity(mc.thePlayer, new S0BPacketAnimation(mc.thePlayer, 0));
                }
            }
        }
    }

    private int getArmSwingAnimationEnd() {
        return mc.thePlayer.isPotionActive(Potion.digSpeed) ? 6 - (1 + mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier()) : (mc.thePlayer.isPotionActive(Potion.digSlowdown) ? 6 + (1 + mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);
    }
}
