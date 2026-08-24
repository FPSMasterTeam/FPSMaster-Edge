package top.fpsmaster.cosmetic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.features.impl.render.DragonWingsRenderer;
import top.fpsmaster.modules.client.api.AuthService;
import top.fpsmaster.modules.client.api.FPSMasterApiClient;
import top.fpsmaster.modules.client.api.model.CosmeticItem;
import top.fpsmaster.modules.client.api.model.OwnedItemView;
import top.fpsmaster.modules.logger.ClientLogger;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CosmeticManager {
    public static final String BUILTIN_WINGS_ID = "builtin:dragon-wings";
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final CosmeticOption BUILTIN_WINGS =
            new CosmeticOption(BUILTIN_WINGS_ID, "", "", "wings", null, "0", 1f, true, false);
    private static final CosmeticManager INSTANCE = new CosmeticManager();

    private final DragonWingsRenderer wingsRenderer = new DragonWingsRenderer();
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private volatile List<CosmeticOption> catalogOptions = Collections.emptyList();
    private volatile List<CosmeticOption> ownedOptions = Collections.emptyList();
    private volatile List<CosmeticOption> customOptions = Collections.emptyList();
    private volatile String previewCapeId;
    private volatile String previewWingsId;
    private volatile float previewWingScale = 1f;
    private volatile boolean previewing;

    private CosmeticManager() {
    }

    public static CosmeticManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        EventDispatcher.registerListener(wingsRenderer);
        reloadCustom();
        refreshOwned();
    }

    public void reloadCustom() {
        Path directory = customDirectory();
        try {
            Files.createDirectories(directory);
            installExamples(directory);
            List<CosmeticOption> options = new ArrayList<>();
            try (java.nio.file.DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
                for (Path file : files) {
                    try {
                        options.add(parseCustom(file));
                    } catch (Exception exception) {
                        ClientLogger.warn("Failed to load custom cosmetic " + file.getFileName() + ": " + exception.getMessage());
                    }
                }
            }
            options.sort(Comparator.comparing(CosmeticOption::getCategory).thenComparing(CosmeticOption::getName));
            synchronized (textures) {
                for (CosmeticOption option : customOptions) textures.remove(option.id);
            }
            customOptions = Collections.unmodifiableList(options);
            validateSelections();
        } catch (Exception exception) {
            ClientLogger.warn("Failed to load custom cosmetics: " + exception.getMessage());
        }
    }

    public void openCustomDirectory() {
        reloadCustom();
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new UnsupportedOperationException("Desktop folder opening is unavailable");
            }
            Desktop.getDesktop().open(customDirectory().toFile());
        } catch (Exception exception) {
            ClientLogger.warn("Failed to open custom cosmetics folder: " + exception.getMessage());
        }
    }

    public void refreshOwned() {
        FPSMasterApiClient.getInstance().getCatalogItems().whenComplete((result, error) -> {
            if (error != null || result == null || !result.isSuccess()) {
                ClientLogger.warn("Failed to load cosmetics catalog: " +
                        (error != null ? error.getMessage() : result == null ? "empty response" : result.getMessage()));
                return;
            }
            List<CosmeticOption> options = new ArrayList<>();
            CosmeticItem[] items = result.getData();
            if (items != null) {
                for (CosmeticItem item : items) {
                    if (item == null || !item.isAvailable()) continue;
                    String category = item.getCategory().toLowerCase(java.util.Locale.ROOT);
                    if (!("cape".equals(category) || "wings".equals(category)) || item.getAssetKey().isEmpty()) continue;
                    options.add(option(item, category));
                }
            }
            options.sort(Comparator.comparing(CosmeticOption::getCategory).thenComparing(CosmeticOption::getName));
            catalogOptions = Collections.unmodifiableList(options);
        });
        if (!AuthService.getInstance().isLoggedIn()) {
            ownedOptions = Collections.emptyList();
            validateSelections();
            return;
        }
        FPSMasterApiClient.getInstance().getOwnedItems().whenComplete((result, error) -> {
            if (error != null || result == null || !result.isSuccess()) {
                ClientLogger.warn("Failed to load owned cosmetics: " +
                        (error != null ? error.getMessage() : result == null ? "empty response" : result.getMessage()));
                return;
            }
            List<CosmeticOption> options = new ArrayList<>();
            OwnedItemView[] owned = result.getData();
            if (owned != null) {
                for (OwnedItemView view : owned) {
                    CosmeticItem item = view == null ? null : view.getItem();
                    if (item == null) continue;
                    String category = item.getCategory().toLowerCase(java.util.Locale.ROOT);
                    if (!("cape".equals(category) || "wings".equals(category)) || item.getAssetKey().isEmpty()) continue;
                    options.add(option(item, category));
                }
            }
            options.sort(Comparator.comparing(CosmeticOption::getCategory).thenComparing(CosmeticOption::getName));
            ownedOptions = Collections.unmodifiableList(options);
            validateSelections();
            CosmeticOption cape = selectedCape();
            if (cape != null) loadTexture(cape);
            loadTexture(selectedWings());
        });
    }

    public List<CosmeticOption> allOptions() {
        Map<String, CosmeticOption> options = new LinkedHashMap<>();
        options.put(BUILTIN_WINGS.id, BUILTIN_WINGS);
        for (CosmeticOption option : customOptions) options.put(option.id, option);
        for (CosmeticOption option : catalogOptions) options.put(option.id, option);
        for (CosmeticOption option : ownedOptions) options.put(option.id, option);
        return new ArrayList<>(options.values());
    }

    public boolean isOwned(String id) {
        if (BUILTIN_WINGS_ID.equals(id)) return true;
        for (CosmeticOption option : customOptions) if (option.id.equals(id)) return true;
        for (CosmeticOption option : ownedOptions) if (option.id.equals(id)) return true;
        return false;
    }

    public boolean isEquipped(String id) {
        CosmeticOption option = findAll(id);
        if (option == null) return false;
        if ("cape".equals(option.category)) {
            return id.equals(FPSMaster.configManager.configure.cosmeticCapeId);
        }
        return wingsEnabled() && id.equals(FPSMaster.configManager.configure.cosmeticWingsId);
    }

    public void preview(String id) {
        CosmeticOption option = findAll(id);
        if (option == null) return;
        if ("cape".equals(option.category)) {
            previewCapeId = id;
        } else {
            previewWingsId = id;
            previewWingScale = isEquipped(id) ? FPSMaster.configManager.configure.cosmeticWingScale : option.defaultScale;
        }
        loadTexture(option);
    }

    public void equip(String id) {
        if (!isOwned(id)) return;
        CosmeticOption option = findAll(id);
        if (option == null) return;
        if ("cape".equals(option.category)) {
            FPSMaster.configManager.configure.cosmeticCapeId = id;
            previewCapeId = id;
        } else {
            FPSMaster.configManager.configure.cosmeticWingsId = id;
            FPSMaster.configManager.configure.cosmeticWingsEnabled = true;
            previewWingsId = id;
            FPSMaster.configManager.configure.cosmeticWingScale = previewing ? previewWingScale : option.defaultScale;
        }
        loadTexture(option);
    }

    public void grantPurchasedAndEquip(String id) {
        CosmeticOption option = findAll(id);
        if (option == null) return;
        List<CosmeticOption> next = new ArrayList<>(ownedOptions);
        next.removeIf(candidate -> candidate.id.equals(id));
        next.add(option);
        ownedOptions = Collections.unmodifiableList(next);
        equip(id);
    }

    public void clearPreview() {
        previewCapeId = null;
        previewWingsId = null;
        previewWingScale = FPSMaster.configManager.configure.cosmeticWingScale;
    }

    public List<CosmeticOption> capeOptions() {
        List<CosmeticOption> result = new ArrayList<>();
        for (CosmeticOption option : customOptions) if ("cape".equals(option.category)) result.add(option);
        for (CosmeticOption option : ownedOptions) if ("cape".equals(option.category) && !result.contains(option)) result.add(option);
        return result;
    }

    public List<CosmeticOption> wingOptions() {
        List<CosmeticOption> result = new ArrayList<>();
        result.add(BUILTIN_WINGS);
        for (CosmeticOption option : customOptions) if ("wings".equals(option.category)) result.add(option);
        for (CosmeticOption option : ownedOptions) if ("wings".equals(option.category) && !result.contains(option)) result.add(option);
        return result;
    }

    public CosmeticOption selectedCape() {
        String selectedId = FPSMaster.configManager.configure.cosmeticCapeId;
        for (CosmeticOption option : capeOptions()) if (option.id.equals(selectedId)) return option;
        return null;
    }

    public CosmeticOption selectedWings() {
        String selectedId = FPSMaster.configManager.configure.cosmeticWingsId;
        for (CosmeticOption option : wingOptions()) if (option.id.equals(selectedId)) return option;
        return BUILTIN_WINGS;
    }

    public void nextCape() {
        List<CosmeticOption> options = capeOptions();
        CosmeticOption current = selectedCape();
        int index = current == null ? -1 : options.indexOf(current);
        CosmeticOption next = index + 1 < options.size() ? options.get(index + 1) : null;
        FPSMaster.configManager.configure.cosmeticCapeId = next == null ? null : next.id;
        if (next != null) loadTexture(next);
    }

    public void nextWings() {
        List<CosmeticOption> options = wingOptions();
        int index = options.indexOf(selectedWings());
        CosmeticOption next = options.get((Math.max(0, index) + 1) % options.size());
        FPSMaster.configManager.configure.cosmeticWingsId = next.id;
        loadTexture(next);
    }

    public boolean wingsEnabled() {
        return FPSMaster.configManager.configure.cosmeticWingsEnabled;
    }

    public void setWingsEnabled(boolean enabled) {
        FPSMaster.configManager.configure.cosmeticWingsEnabled = enabled;
        if (enabled) loadTexture(selectedWings());
    }

    public boolean capeAnimationEnabled() {
        return FPSMaster.configManager.configure.cosmeticCapeAnimationEnabled;
    }

    public void setCapeAnimationEnabled(boolean enabled) {
        FPSMaster.configManager.configure.cosmeticCapeAnimationEnabled = enabled;
    }

    public float wingScale() {
        CosmeticOption option = effectiveWings();
        if (!option.scaleAdjustable) return option.defaultScale;
        return previewing && previewWingsId != null
                ? previewWingScale : FPSMaster.configManager.configure.cosmeticWingScale;
    }

    public void setWingScale(float scale) {
        CosmeticOption option = effectiveWings();
        if (!option.scaleAdjustable) return;
        float value = Math.max(0f, Math.min(1f, scale));
        if (previewing && previewWingsId != null) {
            previewWingScale = value;
            if (isEquipped(option.id)) FPSMaster.configManager.configure.cosmeticWingScale = value;
        } else {
            FPSMaster.configManager.configure.cosmeticWingScale = value;
        }
    }

    public boolean wingScaleAdjustable() {
        return effectiveWings().scaleAdjustable;
    }

    public boolean rendersDragonWings() {
        return wingsEnabled() || previewing && previewWingsId != null;
    }

    public ResourceLocation wingTexture() {
        CosmeticOption selected = effectiveWings();
        return BUILTIN_WINGS_ID.equals(selected.id) ? null : selectedTexture(selected);
    }

    public ResourceLocation capeTexture() {
        CosmeticOption selected = effectiveCape();
        return selected == null ? null : selectedTexture(selected);
    }

    public ResourceLocation textureFor(String id) {
        CosmeticOption option = findAll(id);
        return option == null || BUILTIN_WINGS_ID.equals(id) ? null : selectedTexture(option);
    }

    public DragonWingsRenderer wingsRenderer() {
        return wingsRenderer;
    }

    public boolean isPreviewing() {
        return previewing;
    }

    public void setPreviewing(boolean previewing) {
        this.previewing = previewing;
    }

    private CosmeticOption effectiveCape() {
        CosmeticOption preview = previewing ? findAll(previewCapeId) : null;
        return preview != null ? preview : selectedCape();
    }

    private CosmeticOption effectiveWings() {
        CosmeticOption preview = previewing ? findAll(previewWingsId) : null;
        return preview != null ? preview : selectedWings();
    }

    private CosmeticOption findAll(String id) {
        if (id == null) return null;
        for (CosmeticOption option : allOptions()) if (id.equals(option.id)) return option;
        return null;
    }

    private ResourceLocation selectedTexture(CosmeticOption option) {
        synchronized (textures) {
            ResourceLocation texture = textures.get(option.id);
            if (texture != null) return texture;
        }
        loadTexture(option);
        return null;
    }

    private void validateSelections() {
        if (FPSMaster.configManager.configure.cosmeticCapeId != null && selectedCape() == null) {
            FPSMaster.configManager.configure.cosmeticCapeId = null;
        }
        FPSMaster.configManager.configure.cosmeticWingsId = selectedWings().id;
    }

    private void loadTexture(CosmeticOption option) {
        if (option == null || option.assetKey == null) return;
        synchronized (textures) {
            if (textures.containsKey(option.id) || !loading.add(option.id)) return;
        }
        FPSMaster.async.runnable(() -> {
            try {
                BufferedImage image = option.local
                        ? ImageIO.read(Paths.get(option.assetKey).toFile())
                        : download(resolveAssetUrl(option.assetKey));
                if (image == null) throw new IllegalArgumentException("Cosmetic asset is not an image");
                validateDimensions(image, option.category);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    try {
                        ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                                .getDynamicTextureLocation("fpsmaster_cosmetic_" + option.id, new DynamicTexture(image));
                        synchronized (textures) {
                            textures.put(option.id, location);
                            loading.remove(option.id);
                        }
                    } catch (Exception exception) {
                        fail(option, exception);
                    }
                });
            } catch (Exception exception) {
                fail(option, exception);
            }
        });
    }

    private String resolveAssetUrl(String assetKey) {
        return assetKey.startsWith("/") ? "https://api.fpsmaster.top" + assetKey : assetKey;
    }

    private BufferedImage download(String url) throws Exception {
        URI uri = URI.create(url);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Unsupported cosmetic URL");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(20_000);
        connection.setRequestProperty("User-Agent", "FPSMaster-Edge/" + FPSMaster.CLIENT_VERSION);
        try {
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new IllegalStateException("Cosmetic download returned " + connection.getResponseCode());
            }
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    total += read;
                    if (total > MAX_BYTES) throw new IllegalArgumentException("Cosmetic asset is too large");
                    output.write(buffer, 0, read);
                }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
                if (image == null) throw new IllegalArgumentException("Cosmetic asset is not an image");
                return image;
            }
        } finally {
            connection.disconnect();
        }
    }

    private void validateDimensions(BufferedImage image, String category) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean valid = "wings".equals(category)
                ? width == height && width % 30 == 0
                : width % 64 == 0 && height % 32 == 0 && width / 64 == height / 32;
        if (!valid) throw new IllegalArgumentException("Cosmetic atlas dimensions do not match " + category);
    }

    private void fail(CosmeticOption option, Exception exception) {
        synchronized (textures) {
            loading.remove(option.id);
        }
        ClientLogger.warn("Failed to load cosmetic " + option.id + ": " + exception.getMessage());
    }

    private CosmeticOption option(CosmeticItem item, String category) {
        return new CosmeticOption(
                String.valueOf(item.getId()), item.getName(), item.getDescription(), category,
                item.getAssetKey(), item.getPrice(), 1f, true, false
        );
    }

    private CosmeticOption parseCustom(Path file) throws Exception {
        JsonElement element;
        try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            element = new JsonParser().parse(reader);
        }
        if (element == null || !element.isJsonObject()) throw new IllegalArgumentException("root must be an object");
        JsonObject root = element.getAsJsonObject();
        if (integer(root, "schemaVersion", 0) != 1) throw new IllegalArgumentException("unsupported schemaVersion");
        String rawId = string(root, "id");
        if (!rawId.matches("[A-Za-z0-9._-]{1,64}")) throw new IllegalArgumentException("id contains unsupported characters");
        String name = string(root, "name");
        if (name.trim().isEmpty() || name.length() > 64) throw new IllegalArgumentException("name must contain 1-64 characters");
        String category = string(root, "type").toLowerCase(java.util.Locale.ROOT);
        if (!("cape".equals(category) || "wings".equals(category))) {
            throw new IllegalArgumentException("type must be cape or wings");
        }
        Path texture = Paths.get(string(root, "texture"));
        if (!texture.isAbsolute()) texture = file.getParent().resolve(texture);
        texture = texture.normalize().toAbsolutePath();
        if (!Files.isRegularFile(texture)) throw new IllegalArgumentException("texture file does not exist");
        if (Files.size(texture) <= 0L || Files.size(texture) > MAX_BYTES) {
            throw new IllegalArgumentException("cosmetic asset size is invalid");
        }
        JsonObject wing = root.has("wing") && root.get("wing").isJsonObject()
                ? root.getAsJsonObject("wing") : new JsonObject();
        return new CosmeticOption(
                "custom:" + rawId, name, optionalString(root, "description"), category,
                texture.toString(), "0", clamp(decimal(wing, "scale", 1f)),
                bool(wing, "allowResize", true), true
        );
    }

    private Path customDirectory() {
        return Minecraft.getMinecraft().mcDataDir.toPath()
                .resolve("config").resolve("fpsmaster").resolve("cosmetics");
    }

    private void installExamples(Path directory) throws Exception {
        Path examples = directory.resolve("examples");
        Files.createDirectories(examples);
        copyIfMissing(directory.resolve("example-cape.json.disabled"),
                "/assets/fpsmaster/cosmetics/examples/example-cape.json.disabled");
        copyIfMissing(directory.resolve("example-wings.json.disabled"),
                "/assets/fpsmaster/cosmetics/examples/example-wings.json.disabled");
        copyIfMissing(examples.resolve("example-cape.png"),
                "/assets/fpsmaster/cosmetics/examples/example-cape.png");
        copyIfMissing(examples.resolve("example-wings.png"),
                "/assets/fpsmaster/cosmetics/examples/example-wings.png");
    }

    private void copyIfMissing(Path target, String resource) throws Exception {
        if (Files.exists(target)) return;
        try (InputStream input = CosmeticManager.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing bundled cosmetic example " + resource);
            Files.copy(input, target);
        }
    }

    private String string(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        return object.get(key).getAsString();
    }

    private String optionalString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
    }

    private float decimal(JsonObject object, String key, float fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsFloat() : fallback;
    }

    private boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static final class CosmeticOption {
        private final String id;
        private final String name;
        private final String description;
        private final String category;
        private final String assetKey;
        private final String price;
        private final float defaultScale;
        private final boolean scaleAdjustable;
        private final boolean local;

        private CosmeticOption(String id, String name, String description, String category,
                               String assetKey, String price, float defaultScale,
                               boolean scaleAdjustable, boolean local) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.assetKey = assetKey;
            this.price = price;
            this.defaultScale = defaultScale;
            this.scaleAdjustable = scaleAdjustable;
            this.local = local;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public String getPrice() {
            return price;
        }
    }

}
