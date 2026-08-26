package top.fpsmaster.modules.client.api.model;

public class CosmeticItem {
    private long id;
    private String category;
    private String name;
    private String description;
    private String imageUrl;
    private String assetKey;
    private String price;
    private boolean available;
    private String scale;
    private boolean allowResize;
    private String minScale;
    private String maxScale;

    public long getId() {
        return id;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public String getAssetKey() {
        return assetKey == null ? "" : assetKey;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public String getImageUrl() {
        return imageUrl == null ? "" : imageUrl;
    }

    public String getPrice() {
        return price == null ? "0" : price;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Scale policy. The backend only ever enforces one for {@code wings}; every other category is
     * stored as fixed at its authored size, which is what locks the elytra and cape sliders.
     *
     * <p>The fallbacks below describe an item fixed at 1.0, so a response predating these fields
     * yields something un-resizable rather than something silently resizable.
     */
    public float getScale() {
        return ApiDecimals.parse(scale, 1f);
    }

    public boolean isAllowResize() {
        return allowResize;
    }

    public float getMinScale() {
        return ApiDecimals.parse(minScale, getScale());
    }

    public float getMaxScale() {
        return ApiDecimals.parse(maxScale, getScale());
    }
}
