package top.fpsmaster.modules.client.api.model;

/**
 * One entry of the {@code /cosmetics/loadouts/resolve} array. Players who are unknown or have not
 * linked a Minecraft account are simply absent from the response rather than present with a null
 * loadout, so an entry always describes somebody who has cosmetics to draw.
 */
public class ResolvedLoadoutView {
    private String minecraftUuid;
    private CosmeticLoadoutView loadout;

    /** Canonical dashed lowercase, the only form this endpoint accepts or emits. */
    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public CosmeticLoadoutView getLoadout() {
        return loadout;
    }
}
