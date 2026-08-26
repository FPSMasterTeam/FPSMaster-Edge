package top.fpsmaster.modules.client.api.model;

/**
 * Account-level cosmetic loadout, as served by {@code GET/PUT /me/cosmetics/loadout} and nested
 * inside each entry of {@code /cosmetics/loadouts/resolve}.
 *
 * <p>The full {@link CosmeticItem} views are embedded, so a client rendering someone else's loadout
 * gets the asset key and scale policy without a second catalog fetch.
 */
public class CosmeticLoadoutView {
    private Long capeItemId;
    private Long backItemId;
    private boolean builtinWingsEnabled;
    private String wingScale;
    private Boolean capeAnimationEnabled;
    private CosmeticItem capeItem;
    private CosmeticItem backItem;

    /** Cosmetic ids are keyed as strings client-side, where they share a namespace with local ones. */
    public String getCapeItemId() {
        return capeItemId == null ? null : String.valueOf(capeItemId.longValue());
    }

    public String getBackItemId() {
        return backItemId == null ? null : String.valueOf(backItemId.longValue());
    }

    public CosmeticItem getCapeItem() {
        return capeItem;
    }

    public CosmeticItem getBackItem() {
        return backItem;
    }

    public boolean isBuiltinWingsEnabled() {
        return builtinWingsEnabled;
    }

    /** A real scale such as 1.20, never a 0..1 slider position. */
    public float getWingScale() {
        return ApiDecimals.parse(wingScale, 1f);
    }

    /** Absent means enabled, matching how the backend defaults an omitted field on write. */
    public boolean isCapeAnimationEnabled() {
        return capeAnimationEnabled == null || capeAnimationEnabled;
    }
}
