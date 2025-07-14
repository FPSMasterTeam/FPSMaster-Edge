package top.fpsmaster.modules.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.optimizes.OldAnimations;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.render.ItemPhysics;
import top.fpsmaster.features.impl.utility.IRC;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.*;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.os.FileUtils;

import java.util.Map;

public class ConfigManager {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Configure configure = new Configure();

    public ConfigManager() {
        if (!FileUtils.dir.exists()) {
            FileUtils.dir.mkdirs();
        }
    }

    private void saveComponents() throws FileException {
        JsonObject json = new JsonObject();
        for (Component moduleComponent : FPSMaster.componentsManager.components) {
            JsonObject component = new JsonObject();
            component.addProperty("name", moduleComponent.mod.name);
            component.addProperty("x", moduleComponent.x);
            component.addProperty("y", moduleComponent.y);
            component.addProperty("position", moduleComponent.position.name());
            component.addProperty("scale", moduleComponent.scale);
            json.add(moduleComponent.mod.name, component);
        }
        FileUtils.saveFile("components.json", gson.toJson(json));
    }

    private void readComponents() throws FileException {
        String jsonStr = FileUtils.readFile("components.json");
        if (jsonStr.isEmpty()) return;
        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
        for (Module mod : FPSMaster.moduleManager.modules) {
            JsonObject module = json.getAsJsonObject(mod.name);
            if (module != null) {
                FPSMaster.componentsManager.components.stream()
                        .filter(component -> component.mod.name.equals(module.get("name").getAsString()))
                        .findFirst()
                        .ifPresent(component -> {
                            component.x = module.get("x").getAsFloat();
                            component.y = module.get("y").getAsFloat();
                            component.scale = module.has("scale") && !module.get("scale").isJsonNull()
                                    ? module.get("scale").getAsFloat()
                                    : 1f;
                            component.position = Position.valueOf(module.get("position").getAsString());
                        });
            }
        }
    }

    public void saveConfig(String name) throws FileException {
        saveComponents();
        JsonObject json = new JsonObject();
        JsonObject configures = new JsonObject();
        configure.configures.forEach(configures::addProperty);
        json.add("clientConfigure", configures);

        for (Module module : FPSMaster.moduleManager.modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            moduleJson.addProperty("key", module.key);
            for (Setting<?> setting : module.settings) {
                String settingValue = setting.getValue().toString();
                if (setting instanceof ColorSetting) {
                    ColorSetting colorSetting = (ColorSetting) setting;
                    CustomColor value = colorSetting.getValue();
                    settingValue = value.hue + "|" + value.saturation +
                            "|" + value.brightness + "|" + value.alpha;
                }
                moduleJson.addProperty(setting.name, settingValue);
            }
            json.add(module.name, moduleJson);
        }

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

        readComponents();
        jsonStr = FileUtils.readFile(name + ".json");
        JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

        for (Module module : FPSMaster.moduleManager.modules) {
            JsonObject moduleJson = json.getAsJsonObject(module.name);
            if (moduleJson != null) {
                module.set(moduleJson.get("enabled").getAsBoolean());
                module.key = moduleJson.get("key").getAsInt();
                for (Setting<?> setting : module.settings) {
                    JsonElement settingValue = moduleJson.get(setting.name);
                    if (settingValue != null) {
                        if (setting instanceof BooleanSetting) {
                            BooleanSetting booleanSetting = (BooleanSetting) setting;
                            booleanSetting.setValue(settingValue.getAsBoolean());
                        } else if (setting instanceof NumberSetting) {
                            NumberSetting numberSetting = (NumberSetting) setting;
                            numberSetting.setValue(settingValue.getAsDouble());
                        } else if (setting instanceof ModeSetting) {
                            ModeSetting modeSetting = (ModeSetting) setting;
                            modeSetting.setValue(settingValue.getAsInt());
                        } else if (setting instanceof TextSetting) {
                            TextSetting textSetting = (TextSetting) setting;
                            textSetting.setValue(settingValue.getAsString());
                        } else if (setting instanceof ColorSetting) {
                            ColorSetting colorSetting = (ColorSetting) setting;
                            String[] colorParts = settingValue.getAsString().split("\\|");
                            colorSetting.getValue().setColor(
                                    Float.parseFloat(colorParts[0]),
                                    Float.parseFloat(colorParts[1]),
                                    Float.parseFloat(colorParts[2]),
                                    Float.parseFloat(colorParts[3])
                            );
                        } else if (setting instanceof BindSetting) {
                            BindSetting bindSetting = (BindSetting) setting;
                            bindSetting.setValue(settingValue.getAsInt());
                        }
                    }
                }
            }
        }

        JsonObject clientConfigure = json.get("clientConfigure").getAsJsonObject();
        for (Map.Entry<String, JsonElement> element : clientConfigure.entrySet()) {
            configure.configures.put(element.getKey(), element.getValue().getAsString());
        }
    }

    private void openDefaultModules() {
        FPSMaster.moduleManager.getModule(Performance.class).set(true);
        FPSMaster.moduleManager.getModule(OldAnimations.class).set(true);
        FPSMaster.moduleManager.getModule(ItemPhysics.class).set(true);
        FPSMaster.moduleManager.getModule(IRC.class).set(true);
    }
}
