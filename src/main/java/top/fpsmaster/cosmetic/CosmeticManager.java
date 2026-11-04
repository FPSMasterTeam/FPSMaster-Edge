package top.fpsmaster.cosmetic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.render.DragonWingsRenderer;
import top.fpsmaster.features.impl.render.ElytraRenderer;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.system.FolderOpen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
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
import java.util.UUID;

/**
 * Every cosmetic is local: the built-in dragon wings, the items bundled in the jar
 * ({@value #BUNDLED_INDEX}) and the player's own files in {@code config/fpsmaster/cosmetics}.
 * There is no shop and no account, so everything listed is owned. Cosmetics are only drawn on the
 * local player: showing other players' items needed the FPSMaster servers, which are gone.
 */
public final class CosmeticManager {
    public static final String BUILTIN_WINGS_ID = "builtin:dragon-wings";
    private static final String BUNDLED_ROOT = "/assets/fpsmaster/cosmetics/bundled/";
    private static final String BUNDLED_INDEX = BUNDLED_ROOT + "index.json";
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    /** Contract bounds. An individual item narrows or locks its own range inside these. */
    public static final float SCALE_FLOOR = 0.10f;
    public static final float SCALE_CEILING = 3.00f;
    private static final CosmeticOption BUILTIN_WINGS =
            new CosmeticOption(BUILTIN_WINGS_ID, "", "", "wings", null, 1f, true, 0.5f, 1.5f, Source.BUILTIN);
    private static final CosmeticManager INSTANCE = new CosmeticManager();

    /** Back cosmetics this client can actually draw. Anything else is neither offered nor equipped. */
    private static boolean isBackCategory(String category) {
        return "wings".equals(category) || "elytra".equals(category);
    }

    private static boolean isRenderableCategory(String category) {
        return "cape".equals(category) || isBackCategory(category);
    }

    private final DragonWingsRenderer wingsRenderer = new DragonWingsRenderer();
    private final ElytraRenderer elytraRenderer = new ElytraRenderer();
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    /**
     * Option id -> when its texture last failed to load. The renderers ask for a texture every frame,
     * so without this a missing or malformed atlas was read again on the next frame, forever.
     * Guarded by {@link #textures}.
     */
    private final Map<String, Long> failedAt = new HashMap<>();
    private static final long TEXTURE_RETRY_MS = 5L * 60_000L;
    private volatile List<CosmeticOption> bundledOptions = Collections.emptyList();
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
        bundledOptions = loadBundled();
        reloadCustom();
    }

    /** Items shipped inside the jar. A missing index just means this build bundles none. */
    private static List<CosmeticOption> loadBundled() {
        List<CosmeticOption> options = new ArrayList<>();
        try (InputStream input = CosmeticManager.class.getResourceAsStream(BUNDLED_INDEX)) {
            if (input == null) return Collections.emptyList();
            JsonElement element = new JsonParser().parse(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
            JsonElement items = element != null && element.isJsonObject() ? element.getAsJsonObject().get("items") : null;
            if (items == null || !items.isJsonArray()) throw new IllegalArgumentException("index has no items array");
            for (JsonElement item : items.getAsJsonArray()) {
                try {
                    if (!item.isJsonObject()) throw new IllegalArgumentException("item must be an object");
                    options.add(parseBundled(item.getAsJsonObject()));
                } catch (Exception exception) {
                    ClientLogger.warn("Skipping bundled cosmetic: " + exception.getMessage());
                }
            }
        } catch (Exception exception) {
            ClientLogger.warn("Failed to load bundled cosmetics: " + exception.getMessage());
            return Collections.emptyList();
        }
        options.sort(Comparator.comparing(CosmeticOption::getCategory).thenComparing(CosmeticOption::getName));
        return Collections.unmodifiableList(options);
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
            // The reloaded file may hold a different image under the same id, so the entry has to go
            // — and with it the GL texture, which dropping the map entry alone would strand.
            synchronized (textures) {
                for (CosmeticOption option : customOptions) {
                    deleteTexture(textures.remove(option.id));
                    // The user may just have fixed the file: give it a fresh attempt.
                    failedAt.remove(option.id);
                }
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
            if (!FolderOpen.open(customDirectory().toFile())) {
                throw new UnsupportedOperationException("Desktop folder opening is unavailable");
            }
        } catch (Exception exception) {
            ClientLogger.warn("Failed to open custom cosmetics folder: " + exception.getMessage());
        }
    }

    public List<CosmeticOption> allOptions() {
        Map<String, CosmeticOption> options = new LinkedHashMap<>();
        options.put(BUILTIN_WINGS.id, BUILTIN_WINGS);
        for (CosmeticOption option : bundledOptions) options.put(option.id, option);
        for (CosmeticOption option : customOptions) options.put(option.id, option);
        return new ArrayList<>(options.values());
    }

    /** Everything that is listed can be worn: there is nothing left to buy. */
    public boolean isOwned(String id) {
        return findAll(id) != null;
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
            previewWingScale = option.clampScale(
                    isEquipped(id) ? FPSMaster.configManager.configure.cosmeticWingScale : option.defaultScale);
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
            FPSMaster.configManager.configure.cosmeticWingScale =
                    option.clampScale(previewing ? previewWingScale : option.defaultScale);
        }
        loadTexture(option);
    }

    public void clearPreview() {
        previewCapeId = null;
        previewWingsId = null;
        previewWingScale = FPSMaster.configManager.configure.cosmeticWingScale;
    }

    public List<CosmeticOption> capeOptions() {
        List<CosmeticOption> result = new ArrayList<>();
        for (CosmeticOption option : allOptions()) if ("cape".equals(option.category)) result.add(option);
        return result;
    }

    /** Built-in wings first: {@link #allOptions()} always starts with them. */
    public List<CosmeticOption> wingOptions() {
        List<CosmeticOption> result = new ArrayList<>();
        for (CosmeticOption option : allOptions()) if (isBackCategory(option.category)) result.add(option);
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
        float value = option.clampScale(scale);
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

    /** Drops every uploaded cosmetic. The next render re-uploads whatever is still selected. */
    public void releaseTextures() {
        synchronized (textures) {
            for (ResourceLocation location : textures.values()) deleteTexture(location);
            textures.clear();
            loading.clear();
            failedAt.clear();
        }
    }

    private static void deleteTexture(ResourceLocation location) {
        if (location == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.getTextureManager() != null) {
            minecraft.getTextureManager().deleteTexture(location);
        }
    }

    private void loadTexture(CosmeticOption option) {
        if (option == null || option.assetKey == null) return;
        synchronized (textures) {
            if (textures.containsKey(option.id)) return;
            Long failed = failedAt.get(option.id);
            if (failed != null && System.currentTimeMillis() - failed < TEXTURE_RETRY_MS) return;
            if (!loading.add(option.id)) return;
        }
        FPSMaster.async.runnable(() -> {
            try {
                BufferedImage image = readImage(option);
                if (image == null) throw new IllegalArgumentException("Cosmetic asset is not an image");
                validateDimensions(image, option.category);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    try {
                        ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                                .getDynamicTextureLocation("fpsmaster_cosmetic_" + option.id, new DynamicTexture(image));
                        synchronized (textures) {
                            textures.put(option.id, location);
                            loading.remove(option.id);
                            failedAt.remove(option.id);
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

    private static BufferedImage readImage(CosmeticOption option) throws Exception {
        if (option.source != Source.BUNDLED) return ImageIO.read(Paths.get(option.assetKey).toFile());
        try (InputStream input = CosmeticManager.class.getResourceAsStream(option.assetKey)) {
            if (input == null) throw new IllegalStateException("Missing bundled texture " + option.assetKey);
            return ImageIO.read(input);
        }
    }

    private void validateDimensions(BufferedImage image, String category) {
        int width = image.getWidth();
        int height = image.getHeight();
        // Wings use the 30n studio atlas; capes and elytra both use a 64x32-proportioned sheet.
        boolean valid = "wings".equals(category)
                ? width == height && width % 30 == 0
                : width % 64 == 0 && height % 32 == 0 && width / 64 == height / 32;
        if (!valid) throw new IllegalArgumentException("Cosmetic atlas dimensions do not match " + category);
    }

    private void fail(CosmeticOption option, Exception exception) {
        synchronized (textures) {
            loading.remove(option.id);
            failedAt.put(option.id, System.currentTimeMillis());
        }
        ClientLogger.warn("Failed to load cosmetic " + option.id + " (next attempt in "
                + (TEXTURE_RETRY_MS / 60_000L) + " min): " + exception.getMessage());
    }

    // ================== Per-player resolution ================== //

    /** What to draw on a player's back this frame. */
    public static final class BackPiece {
        /** Null means the built-in dragon wings atlas. */
        public final ResourceLocation texture;
        public final boolean elytra;
        public final float scale;

        BackPiece(ResourceLocation texture, boolean elytra, float scale) {
            this.texture = texture;
            this.elytra = elytra;
            this.scale = scale;
        }
    }

    /** Other players never get a back piece: their loadouts used to come from the servers. */
    public BackPiece backPieceFor(UUID uuid, boolean local) {
        if (!local || !rendersDragonWings()) return null;
        CosmeticOption option = effectiveWings();
        ResourceLocation texture = BUILTIN_WINGS_ID.equals(option.id) ? null : selectedTexture(option);
        // A wing whose texture has not been uploaded yet must not fall back to the built-in
        // atlas, which would briefly show the wrong cosmetic.
        if (texture == null && !BUILTIN_WINGS_ID.equals(option.id)) return null;
        return new BackPiece(texture, option.isElytra(), Math.max(0.01f, wingScale()));
    }

    public ResourceLocation capeTextureFor(UUID uuid, boolean local) {
        return local ? capeTexture() : null;
    }

    public boolean capeAnimationFor(UUID uuid, boolean local) {
        return local && capeAnimationEnabled();
    }

    public ElytraRenderer elytraRenderer() {
        return elytraRenderer;
    }

    private CosmeticOption parseCustom(Path file) throws Exception {
        JsonElement element;
        try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            element = new JsonParser().parse(reader);
        }
        if (element == null || !element.isJsonObject()) throw new IllegalArgumentException("root must be an object");
        JsonObject root = element.getAsJsonObject();
        if (integer(root, "schemaVersion", 0) != 1) throw new IllegalArgumentException("unsupported schemaVersion");
        Path texture = Paths.get(string(root, "texture"));
        if (!texture.isAbsolute()) texture = file.getParent().resolve(texture);
        texture = texture.normalize().toAbsolutePath();
        if (!Files.isRegularFile(texture)) throw new IllegalArgumentException("texture file does not exist");
        if (Files.size(texture) <= 0L || Files.size(texture) > MAX_BYTES) {
            throw new IllegalArgumentException("cosmetic asset size is invalid");
        }
        return parseItem(root, "custom:", texture.toString(), Source.CUSTOM);
    }

    /**
     * A bundled item uses the same fields as a custom file; its {@code texture} is relative to
     * {@value #BUNDLED_ROOT}. Ids are kept as-is (no prefix) so a loadout saved by an older build,
     * which stored shop item ids, still finds its item once that item is bundled under the same id.
     */
    private static CosmeticOption parseBundled(JsonObject root) {
        String texture = string(root, "texture");
        if (!texture.matches("[A-Za-z0-9._/-]{1,128}") || texture.contains("..") || texture.startsWith("/")) {
            throw new IllegalArgumentException("texture path is not a plain relative path");
        }
        return parseItem(root, "", BUNDLED_ROOT + texture, Source.BUNDLED);
    }

    private static CosmeticOption parseItem(JsonObject root, String idPrefix, String assetKey, Source source) {
        String rawId = string(root, "id");
        if (!rawId.matches("[A-Za-z0-9._-]{1,64}")) throw new IllegalArgumentException("id contains unsupported characters");
        String name = string(root, "name");
        if (name.trim().isEmpty() || name.length() > 64) throw new IllegalArgumentException("name must contain 1-64 characters");
        String category = string(root, "type").toLowerCase(java.util.Locale.ROOT);
        if (!isRenderableCategory(category)) {
            throw new IllegalArgumentException("type must be cape, wings or elytra");
        }
        JsonObject wing = root.has("wing") && root.get("wing").isJsonObject()
                ? root.getAsJsonObject("wing") : new JsonObject();
        float scale = clamp(decimal(wing, "scale", 1f));
        boolean resizable = bool(wing, "allowResize", true);
        return new CosmeticOption(
                idPrefix + rawId, name, optionalString(root, "description"), category,
                assetKey, scale, resizable,
                resizable ? clamp(decimal(wing, "minScale", 0.5f)) : scale,
                resizable ? clamp(decimal(wing, "maxScale", 1.5f)) : scale,
                source
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

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        return object.get(key).getAsString();
    }

    private static String optionalString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
    }

    private static float decimal(JsonObject object, String key, float fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsFloat() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
    }

    private static float clamp(float value) {
        return Math.max(SCALE_FLOOR, Math.min(SCALE_CEILING, value));
    }

    private enum Source { BUILTIN, BUNDLED, CUSTOM }

    public static final class CosmeticOption {
        private final String id;
        private final String name;
        private final String description;
        private final String category;
        /** File path for custom items, classpath resource for bundled ones, null for built-in. */
        private final String assetKey;
        private final float defaultScale;
        private final boolean scaleAdjustable;
        private final float minScale;
        private final float maxScale;
        private final Source source;

        private CosmeticOption(String id, String name, String description, String category,
                               String assetKey, float defaultScale,
                               boolean scaleAdjustable, float minScale, float maxScale, Source source) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.assetKey = assetKey;
            this.defaultScale = defaultScale;
            this.scaleAdjustable = scaleAdjustable;
            // A locked item collapses its range onto its authored size, so clamping alone enforces the
            // policy and no caller has to remember to check the flag first.
            this.minScale = scaleAdjustable ? Math.min(minScale, maxScale) : defaultScale;
            this.maxScale = scaleAdjustable ? Math.max(minScale, maxScale) : defaultScale;
            this.source = source;
        }

        public float clampScale(float scale) {
            return Math.max(minScale, Math.min(maxScale, scale));
        }

        public boolean isElytra() {
            return "elytra".equals(category);
        }

        public float getDefaultScale() {
            return defaultScale;
        }

        public boolean isScaleAdjustable() {
            return scaleAdjustable;
        }

        public float getMinScale() {
            return minScale;
        }

        public float getMaxScale() {
            return maxScale;
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
    }

}
