package top.fpsmaster.forge.mixin;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.utility.SoundModifier;

import static top.fpsmaster.utils.Utility.mc;

@Mixin(WorldClient.class)
public class WorldClientMixin {
    @ModifyConstant(method = "doVoidFogParticles", constant = @Constant(intValue = 1000))
    private int patcher$lowerTickCount(int original) {
        return Performance.lowAnimationTick.getValue() ? 100 : original;
    }
//
//    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
//    public void playSound(double x, double y, double z, String soundName, float volume, float pitch, boolean distanceDelay, CallbackInfo ci) {
//        if (soundName.startsWith("block.") || soundName.startsWith("liquid.")) {
//            play(x, y, z, soundName, volume * SoundModifier.block.getValue().floatValue(), pitch, distanceDelay);
//            ci.cancel();
//        }
//        if (soundName.startsWith("game.")) {
//            play(x, y, z, soundName, volume * SoundModifier.game.getValue().floatValue(), pitch, distanceDelay);
//            ci.cancel();
//        }
//    }
//
//    @Unique
//    public void play(double x, double y, double z, String soundName, float volume, float pitch, boolean distanceDelay) {
//        double d0 = mc.getRenderViewEntity().getDistanceSq(x, y, z);
//        PositionedSoundRecord positionedsoundrecord = new PositionedSoundRecord(new ResourceLocation(soundName), volume, pitch, (float) x, (float) y, (float) z);
//        if (distanceDelay && d0 > (double) 100.0F) {
//            double d1 = Math.sqrt(d0) / (double) 40.0F;
//            mc.getSoundHandler().playDelayedSound(positionedsoundrecord, (int) (d1 * (double) 20.0F));
//        } else {
//            mc.getSoundHandler().playSound(positionedsoundrecord);
//        }
//    }
}