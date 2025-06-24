package top.fpsmaster.forge.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import top.fpsmaster.utils.GitInfo;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {
    /**
     * @author vlouboos
     * @reason Overwrite Tag
     */
    @Overwrite
    public static String getClientModName() {
        return "fpsmaster:" + GitInfo.getBranch() + ":" + GitInfo.getCommitIdAbbrev();
    }
}
