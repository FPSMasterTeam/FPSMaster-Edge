package top.fpsmaster.modules.config;

import com.google.gson.*;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.render.ItemPhysics;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.MultipleItemSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.modules.config.migration.ConfigMigration;
import top.fpsmaster.modules.config.migration.ConfigMigrationRegistry;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.shortcut.Shortcut;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.world.ItemsUtil;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA_VERSION = 1;
    @Getter
    private boolean loadingConfig;
    @Getter
    private boolean configLoaded;

    public Configure configure = new Configure();

    public void saveConfigQuietly(String name) {
        if (loadingConfig) {
            return;
        }
        try {
            saveConfig(name);
        } catch (FileException exception) {
            ClientLogger.error("Failed to save config quietly: " + name);
        }
    }

    public void saveConfig(String name) throws FileException {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonObject client = new JsonObject();
        client.addProperty("volume", configure.volume);
        client.addProperty("background", configure.background);
        client.addProperty("oobeCompleted", configure.oobeCompleted);
        client.addProperty("antiCheatEnabled", configure.antiCheatEnabled);
        client.addProperty("anonymousDataEnabled", configure.anonymousDataEnabled);
        client.addProperty("telemetryInstanceId", configure.telemetryInstanceId);
        client.addProperty("classicBackgroundColor", configure.classicBackgroundColor);
        client.addProperty("classicBackgroundHue", configure.classicBackgroundHue);
        client.addProperty("classicBackgroundSaturation", configure.classicBackgroundSaturation);
        client.addProperty("classicBackgroundBrightness", configure.classicBackgroundBrightness);
        client.addProperty("classicBackgroundAlpha", configure.classicBackgroundAlpha);
        client.addProperty("classicBackgroundMode", configure.classicBackgroundMode);
        json.add("client", client);

        JsonArray components = new JsonArray();
        for (Component moduleComponent : FPSMaster.componentsManager.components) {
            JsonObject component = new JsonObject();
            component.addProperty("module", moduleComponent.mod.name);
            component.addProperty("x", moduleComponent.x);
            component.addProperty("y", moduleComponent.y);
            component.addProperty("position", moduleComponent.position.name());
            component.addProperty("scale", moduleComponent.scale);
            components.add(component);
        }
        json.add("components", components);

        JsonObject modulesJson = new JsonObject();
        for (Module module : FPSMaster.moduleManager.modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            moduleJson.addProperty("key", module.key);
            JsonObject settingsJson = new JsonObject();
            for (Setting<?> setting : module.settings) {
                if (setting instanceof ColorSetting) {
                    ColorSetting colorSetting = (ColorSetting) setting;
                    CustomColor value = colorSetting.getValue();
                    JsonObject color = new JsonObject();
                    color.addProperty("h", value.hue);
                    color.addProperty("s", value.saturation);
                    color.addProperty("b", value.brightness);
                    color.addProperty("a", value.alpha);
                    color.addProperty("mode", colorSetting.getColorType().name());
                    JsonObject settingJson = new JsonObject();
                    settingJson.addProperty("type", "color");
                    settingJson.add("value", color);
                    settingsJson.add(setting.name, settingJson);
                } else if (setting instanceof MultipleItemSetting) {
                    MultipleItemSetting multipleItemSetting = (MultipleItemSetting) setting;
                    ArrayList<ItemStack> value = multipleItemSetting.getValue();
                    JsonArray items = new JsonArray();
                    for (ItemStack itemStack : value) {
                        JsonObject item = new JsonObject();
                        item.addProperty("id", Item.getIdFromItem(itemStack.getItem()));
                        item.addProperty("meta", itemStack.getMetadata());
                        items.add(item);
                    }
                    JsonObject settingJson = new JsonObject();
                    settingJson.addProperty("type", "multiItem");
                    settingJson.add("value", items);
                    settingsJson.add(setting.name, settingJson);
                } else {
                    JsonObject settingJson = new JsonObject();
                    if (setting instanceof BooleanSetting) {
                        settingJson.addProperty("type", "boolean");
                        settingJson.addProperty("value", ((BooleanSetting) setting).getValue());
                    } else if (setting instanceof NumberSetting) {
                        settingJson.addProperty("type", "number");
                        settingJson.addProperty("value", ((NumberSetting) setting).getValue());
                    } else if (setting instanceof ModeSetting) {
                        settingJson.addProperty("type", "mode");
                        settingJson.addProperty("value", ((ModeSetting) setting).getValue());
                    } else if (setting instanceof TextSetting) {
                        settingJson.addProperty("type", "text");
                        settingJson.addProperty("value", ((TextSetting) setting).getValue());
                    } else if (setting instanceof BindSetting) {
                        settingJson.addProperty("type", "bind");
                        settingJson.addProperty("value", ((BindSetting) setting).getValue());
                    } else {
                        settingJson.addProperty("type", "unknown");
                        settingJson.addProperty("value", String.valueOf(setting.getValue()));
                    }
                    settingsJson.add(setting.name, settingJson);
                }
            }
            moduleJson.add("settings", settingsJson);
            modulesJson.add(module.name, moduleJson);
        }
        json.add("modules", modulesJson);

        JsonArray shortcutsJson = new JsonArray();
        for (Shortcut shortcut : Shortcut.shortcuts) {
            JsonObject shortcutJson = new JsonObject();
            shortcutJson.addProperty("name", shortcut.name);
            shortcutJson.addProperty("key", shortcut.key);
            JsonArray actions = new JsonArray();
            for (Shortcut.Action action : shortcut.actions) {
                JsonObject actionJson = new JsonObject();
                actionJson.addProperty("type", action.type.name());
                actionJson.addProperty("Context", action.context);
                actions.add(actionJson);
            }
            shortcutJson.add("actions", actions);
            shortcutsJson.add(shortcutJson);
        }

        json.add("shortcuts", shortcutsJson);

        FileUtils.saveFile(name + ".json", gson.toJson(json));
    }

    public void loadConfig(String name) throws Exception {
        loadingConfig = true;
        try {
            File configFile = new File(FileUtils.dir, name + ".json");
            ClientLogger.info("Loading config: " + configFile.getAbsolutePath());
            String jsonStr = FileUtils.readFile(name + ".json");
            if (jsonStr.trim().isEmpty()) {
                ClientLogger.warn("Config file is empty, creating default config: " + configFile.getAbsolutePath());
                resetConfigToDefaults(name);
                return;
            }
            JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
            if (json == null) {
                ClientLogger.warn("Config file is empty or invalid JSON, resetting: " + name + ".json");
                resetConfigToDefaults(name);
                return;
            }

            json = migrateConfigIfNeeded(name, json);
            if (json == null) {
                resetConfigToDefaults(name);
                return;
            }

            JsonObject client = json.getAsJsonObject("client");
            if (client != null) {
                if (client.has("volume")) {
                    configure.volume = client.get("volume").getAsDouble();
                }
                if (client.has("background")) {
                    configure.background = client.get("background").getAsString();
                    if ("new".equals(configure.background)) {
                        configure.background = "panorama_1";
                    }
                }
                configure.oobeCompleted = client.has("oobeCompleted")
                        ? client.get("oobeCompleted").getAsBoolean()
                        : FPSMaster.defaultConfigExistedBeforeLoad;
                configure.antiCheatEnabled = !client.has("antiCheatEnabled") || client.get("antiCheatEnabled").getAsBoolean();
                // Telemetry defaults to false (opt-in for privacy compliance)
                configure.anonymousDataEnabled = client.has("anonymousDataEnabled") && client.get("anonymousDataEnabled").getAsBoolean();
                configure.telemetryInstanceId = client.has("telemetryInstanceId")
                        ? client.get("telemetryInstanceId").getAsString()
                        : UUID.randomUUID().toString();
                if (client.has("classicBackgroundColor")) {
                    configure.classicBackgroundColor = client.get("classicBackgroundColor").getAsInt();
                }
                if (client.has("classicBackgroundHue")) {
                    configure.classicBackgroundHue = client.get("classicBackgroundHue").getAsFloat();
                }
                if (client.has("classicBackgroundSaturation")) {
                    configure.classicBackgroundSaturation = client.get("classicBackgroundSaturation").getAsFloat();
                }
                if (client.has("classicBackgroundBrightness")) {
                    configure.classicBackgroundBrightness = client.get("classicBackgroundBrightness").getAsFloat();
                }
                if (client.has("classicBackgroundAlpha")) {
                    configure.classicBackgroundAlpha = client.get("classicBackgroundAlpha").getAsFloat();
                }
                if (client.has("classicBackgroundMode")) {
                    configure.classicBackgroundMode = client.get("classicBackgroundMode").getAsString();
                }

                if (!client.has("classicBackgroundHue") || !client.has("classicBackgroundSaturation") || !client.has("classicBackgroundBrightness") || !client.has("classicBackgroundAlpha")) {
                    CustomColor converted = new CustomColor(new java.awt.Color(configure.classicBackgroundColor, true));
                    configure.classicBackgroundHue = converted.hue;
                    configure.classicBackgroundSaturation = converted.saturation;
                    configure.classicBackgroundBrightness = converted.brightness;
                    configure.classicBackgroundAlpha = converted.alpha;
                }
                if (configure.telemetryInstanceId == null || configure.telemetryInstanceId.trim().isEmpty()) {
                    configure.telemetryInstanceId = UUID.randomUUID().toString();
                }
            }

            JsonArray components = json.getAsJsonArray("components");
            if (components != null) {
                for (JsonElement element : components) {
                    try {
                        JsonObject component = element.getAsJsonObject();
                        if (component == null || !component.has("module")) {
                            continue;
                        }
                        String moduleName = component.get("module").getAsString();
                        Component targetComponent = FPSMaster.componentsManager.components.stream()
                                .filter(c -> c.mod.name.equals(moduleName))
                                .findFirst()
                                .orElse(null);
                        if (targetComponent == null) {
                            ClientLogger.warn("Skipping missing component config: " + moduleName);
                            continue;
                        }
                        targetComponent.x = component.get("x").getAsFloat();
                        targetComponent.y = component.get("y").getAsFloat();
                        targetComponent.scale = component.has("scale") && !component.get("scale").isJsonNull()
                                ? component.get("scale").getAsFloat()
                                : 1f;
                        targetComponent.position = Position.valueOf(component.get("position").getAsString());
                    } catch (Throwable throwable) {
                        ClientLogger.error("Failed to load one component entry from config");
                    }
                }
            }

            JsonObject modulesJson = json.getAsJsonObject("modules");
            for (Module module : FPSMaster.moduleManager.modules) {
                try {
                    JsonObject moduleJson = modulesJson != null ? modulesJson.getAsJsonObject(module.name) : null;
                    if (moduleJson != null && moduleJson.has("settings")) {
                        module.set(moduleJson.get("enabled").getAsBoolean());
                        module.key = moduleJson.get("key").getAsInt();
                        JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                        for (Setting<?> setting : module.settings) {
                            try {
                                JsonObject settingJson = settingsJson.getAsJsonObject(setting.name);
                                if (settingJson != null && settingJson.has("type") && settingJson.has("value")) {
                                    String type = settingJson.get("type").getAsString();
                                    JsonElement value = settingJson.get("value");
                                    if (setting instanceof BooleanSetting && "boolean".equals(type)) {
                                        ((BooleanSetting) setting).setValue(value.getAsBoolean());
                                    } else if (setting instanceof NumberSetting && "number".equals(type)) {
                                        ((NumberSetting) setting).setValue(value.getAsDouble());
                                    } else if (setting instanceof ModeSetting && "mode".equals(type)) {
                                        ((ModeSetting) setting).setValue(value.getAsInt());
                                    } else if (setting instanceof TextSetting && "text".equals(type)) {
                                        ((TextSetting) setting).setValue(value.getAsString());
                                    } else if (setting instanceof ColorSetting && "color".equals(type)) {
                                        JsonObject color = value.getAsJsonObject();
                                        ColorSetting colorSetting = (ColorSetting) setting;
                                        colorSetting.getValue().setColor(
                                                color.get("h").getAsFloat(),
                                                color.get("s").getAsFloat(),
                                                color.get("b").getAsFloat(),
                                                color.get("a").getAsFloat()
                                        );
                                        if (color.has("mode")) {
                                            try {
                                                colorSetting.setColorType(ColorSetting.ColorType.valueOf(color.get("mode").getAsString()));
                                            } catch (IllegalArgumentException ignored) {
                                            }
                                        }
                                    } else if (setting instanceof BindSetting && "bind".equals(type)) {
                                        ((BindSetting) setting).setValue(value.getAsInt());
                                    } else if (setting instanceof MultipleItemSetting && "multiItem".equals(type)) {
                                        MultipleItemSetting multipleItemSetting = (MultipleItemSetting) setting;
                                        multipleItemSetting.getValue().clear();
                                        for (JsonElement itemElement : value.getAsJsonArray()) {
                                            JsonObject item = itemElement.getAsJsonObject();
                                            int id = item.get("id").getAsInt();
                                            int metadata = item.get("meta").getAsInt();
                                            multipleItemSetting.addItem(ItemsUtil.getItemStackWithMetadata(Item.getItemById(id), metadata));
                                        }
                                    }
                                }
                            } catch (Throwable throwable) {
                                ClientLogger.error("Failed to load setting from config: " + module.name + "/" + setting.name);
                            }
                        }
                    }
                } catch (Throwable throwable) {
                    ClientLogger.error("Failed to load module from config: " + module.name);
                }
            }

            JsonArray shortcutsJson = Optional.ofNullable(json.getAsJsonArray("shortcuts")).orElse(new JsonArray());
            for (JsonElement element : shortcutsJson) {
                if (element.isJsonObject()) {
                    JsonObject shortcutJson = element.getAsJsonObject();
                    ArrayList<Shortcut.Action> actions = new ArrayList<>();
                    JsonArray actionsJson = shortcutJson.getAsJsonArray("actions");
                    for (JsonElement actionElement : actionsJson) {
                        if (actionElement.isJsonObject()) {
                            JsonObject actionJson = actionElement.getAsJsonObject();
                            actions.add(new Shortcut.Action(Shortcut.Action.Type.valueOf(actionJson.get("type").getAsString()), actionJson.get("context").getAsString()));
                        }
                    }
                    Shortcut.shortcuts.add(new Shortcut(shortcutJson.get("name").getAsString(),
                            shortcutJson.get("key").getAsInt(),
                            actions
                    ));
                }
            }
        } finally {
            loadingConfig = false;
            configLoaded = true;
        }
    }

    private JsonObject migrateConfigIfNeeded(String name, JsonObject json) throws FileException {
        int currentVersion = readSchemaVersion(json);
        ClientLogger.info("Detected config schemaVersion=" + currentVersion + " for " + name + ".json");
        if (currentVersion == SCHEMA_VERSION) {
            return json;
        }

        List<ConfigMigration> migrationPath = resolveMigrationPath(currentVersion, SCHEMA_VERSION);
        if (migrationPath.isEmpty()) {
            ClientLogger.warn("No config migration path from schema " + currentVersion + " to " + SCHEMA_VERSION + ", deleting " + name + ".json");
            deleteConfigFile(name);
            return null;
        }

        JsonObject migrated = gson.fromJson(gson.toJson(json), JsonObject.class);
        for (ConfigMigration migration : migrationPath) {
            migrated = migration.migrate(migrated);
        }
        migrated.addProperty("schemaVersion", SCHEMA_VERSION);
        FileUtils.saveFile(name + ".json", gson.toJson(migrated));
        ClientLogger.info("Migrated config " + name + ".json from schema " + currentVersion + " to " + SCHEMA_VERSION);
        return migrated;
    }

    private int readSchemaVersion(JsonObject json) {
        if (json == null || !json.has("schemaVersion") || json.get("schemaVersion").isJsonNull()) {
            return 0;
        }
        try {
            return json.get("schemaVersion").getAsInt();
        } catch (RuntimeException exception) {
            ClientLogger.warn("Invalid config schemaVersion, treating as legacy version 0");
            return 0;
        }
    }

    private List<ConfigMigration> resolveMigrationPath(int fromVersion, int targetVersion) {
        if (fromVersion == targetVersion) {
            return Collections.emptyList();
        }
        if (fromVersion > targetVersion) {
            return Collections.emptyList();
        }

        List<ConfigMigration> path = new ArrayList<>();
        int version = fromVersion;
        while (version < targetVersion) {
            ConfigMigration migration = ConfigMigrationRegistry.findMigration(version);
            if (migration == null || migration.getToVersion() <= version || migration.getToVersion() > targetVersion) {
                return Collections.emptyList();
            }
            path.add(migration);
            version = migration.getToVersion();
        }
        return version == targetVersion ? path : Collections.emptyList();
    }

    private void resetConfigToDefaults(String name) throws FileException, Exception {
        File configFile = new File(FileUtils.dir, name + ".json");
        ClientLogger.warn("Resetting config to defaults: " + configFile.getAbsolutePath());
        openDefaultModules();
        saveConfig(name);
        ClientLogger.info("Wrote default config: " + configFile.getAbsolutePath());
        loadConfig(name);
    }

    private void deleteConfigFile(String name) throws FileException {
        File configFile = new File(FileUtils.dir, name + ".json");
        ClientLogger.warn("Deleting config file: " + configFile.getAbsolutePath());
        if (configFile.exists() && !configFile.delete()) {
            throw new FileException("Failed to delete file: " + configFile.getAbsolutePath());
        }
    }

    private void openDefaultModules() {
        FPSMaster.moduleManager.requireModule(Performance.class).set(true);
        FPSMaster.moduleManager.requireModule(OldAnimations.class).set(true);
        FPSMaster.moduleManager.requireModule(ItemPhysics.class).set(true);
    }
}
