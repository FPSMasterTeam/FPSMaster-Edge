package top.fpsmaster.forge.mixin;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.utility.SoundModifier;

@Mixin(SoundHandler.class)
public class MixinSoundHandler {
    @Final
    @Shadow
    private SoundManager sndManager;

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    public void playSound(ISound sound, CallbackInfo ci) {
        if (!SoundModifier.using)
            return;
        if (sound instanceof PositionedSoundRecord) {
            PositionedSoundRecord record = (PositionedSoundRecord) sound;
            if (record.getSoundLocation().getResourcePath().startsWith("dig")) {
                this.sndManager.playSound(new PositionedSoundRecord(record.getSoundLocation(), record.getVolume() * SoundModifier.dig.getValue().floatValue(), record.getPitch(), record.getXPosF(), record.getYPosF(), record.getZPosF()));
                ci.cancel();
            }
            if (record.getSoundLocation().getResourcePath().startsWith("liquid")) {
                this.sndManager.playSound(new PositionedSoundRecord(record.getSoundLocation(), record.getVolume() * SoundModifier.liquid.getValue().floatValue(), record.getPitch(), record.getXPosF(), record.getYPosF(), record.getZPosF()));
                ci.cancel();
            }
            if (record.getSoundLocation().getResourcePath().startsWith("game")) {
                this.sndManager.playSound(new PositionedSoundRecord(record.getSoundLocation(), record.getVolume() * SoundModifier.game.getValue().floatValue(), record.getPitch(), record.getXPosF(), record.getYPosF(), record.getZPosF()));
                ci.cancel();
            }
            if (record.getSoundLocation().getResourcePath().startsWith("mob")) {
                this.sndManager.playSound(new PositionedSoundRecord(record.getSoundLocation(), record.getVolume() * SoundModifier.mob.getValue().floatValue(), record.getPitch(), record.getXPosF(), record.getYPosF(), record.getZPosF()));
                ci.cancel();
            }
        }
    }

}



