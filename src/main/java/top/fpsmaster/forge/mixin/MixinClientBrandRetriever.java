package top.fpsmaster.forge.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import top.fpsmaster.utils.git.GitInfo;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {

    //Some servers abandon any client they don't know, so we need to remove it temporarily until we can detect these servers and switch to vanilla brand automatically.
    /**
     * @author vlouboos
     * @reason Overwrite Tag
     */
//    @Overwrite
//    public static String getClientModName() {
//        return "fpsmaster:" + GitInfo.getBranch() + ":" + GitInfo.getCommitIdAbbrev();
//    }
}



