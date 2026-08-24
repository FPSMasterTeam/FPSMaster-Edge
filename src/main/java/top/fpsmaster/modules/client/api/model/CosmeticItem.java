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
}
