package top.fpsmaster.modules.config;

import com.google.gson.*;
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
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.world.ItemsUtil;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA_VERSION = 1;

    public Configure configure = new Configure();

    public void saveConfig(String name) throws FileException {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonObject client = new JsonObject();
        client.addProperty("volume", configure.volume);
        client.addProperty("background", configure.background);
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

        FileUtils.saveFile(name + ".json", gson.toJson(json));
    }

    public void loadConfig(String name) throws Exception {
        String jsonStr = FileUtils.readFile(name + ".json");
        if (jsonStr.isEmpty()) {
            openDefaultModules();
            saveConfig("default");
            loadConfig(name);
            return;
        }
        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
        if (json == null || !json.has("schemaVersion") || json.get("schemaVersion").getAsInt() != SCHEMA_VERSION) {
            openDefaultModules();
            saveConfig("default");
            loadConfig(name);
            return;
        }

        JsonObject client = json.getAsJsonObject("client");
        if (client != null) {
            if (client.has("volume")) {
                configure.volume = client.get("volume").getAsDouble();
            }
            if (client.has("background")) {
                configure.background = client.get("background").getAsString();
            }
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
        }

        JsonArray components = json.getAsJsonArray("components");
        if (components != null) {
            for (JsonElement element : components) {
                JsonObject component = element.getAsJsonObject();
                if (component == null || !component.has("module")) continue;
                String moduleName = component.get("module").getAsString();
                FPSMaster.componentsManager.components.stream()
                        .filter(c -> c.mod.name.equals(moduleName))
                        .findFirst()
                        .ifPresent(c -> {
                            c.x = component.get("x").getAsFloat();
                            c.y = component.get("y").getAsFloat();
                            c.scale = component.has("scale") && !component.get("scale").isJsonNull()
                                    ? component.get("scale").getAsFloat()
                                    : 1f;
                            c.position = Position.valueOf(component.get("position").getAsString());
                        });
            }
        }

        JsonObject modulesJson = json.getAsJsonObject("modules");
        for (Module module : FPSMaster.moduleManager.modules) {
            JsonObject moduleJson = modulesJson != null ? modulesJson.getAsJsonObject(module.name) : null;
            if (moduleJson != null && moduleJson.has("settings")) {
                module.set(moduleJson.get("enabled").getAsBoolean());
                module.key = moduleJson.get("key").getAsInt();
                JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                for (Setting<?> setting : module.settings) {
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
                            for (JsonElement itemElement : value.getAsJsonArray()) {
                                JsonObject item = itemElement.getAsJsonObject();
                                int id = item.get("id").getAsInt();
                                int metadata = item.get("meta").getAsInt();
                                multipleItemSetting.addItem(ItemsUtil.getItemStackWithMetadata(Item.getItemById(id), metadata));
                            }
                        }
                    }
                }
            }
        }
    }

    private void openDefaultModules() {
        FPSMaster.moduleManager.getModule(Performance.class).set(true);
        FPSMaster.moduleManager.getModule(OldAnimations.class).set(true);
        FPSMaster.moduleManager.getModule(ItemPhysics.class).set(true);
    }
}
