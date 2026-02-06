package top.fpsmaster.forge.mixin;

import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fpsmaster.forge.api.IServerSelectionList;

@Mixin(ServerSelectionList.class)
public abstract class MixinServerSelectionList implements IServerSelectionList {


    @Shadow
    abstract int getSize();

    @Override
    public int getListSize() {

        return 0;
    }
}
