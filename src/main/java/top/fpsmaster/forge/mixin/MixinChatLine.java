package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.ChatLine;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import top.fpsmaster.forge.api.IChatLine;

@Mixin(ChatLine.class)
@Implements(@Interface(iface = IChatLine.class, prefix = "chatline$"))
public class MixinChatLine implements IChatLine{

    float animation;
    @Override
    public void setAnimation(float a) {
        animation = a;
    }

    @Override
    public float getAnimation() {
        return animation;
    }
}
