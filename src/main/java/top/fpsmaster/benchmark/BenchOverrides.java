package top.fpsmaster.benchmark;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.modules.logger.ClientLogger;

import java.util.Map;

/**
 * Forces module and setting state at the start of a benchmark run.
 *
 * <p>This is what makes an interleaved A/B possible from a single build: the two sides of a
 * comparison are usually the same code with a feature toggled, so runs can alternate
 * A/B/A/B without rebuilding or switching branches between them. Alternating matters because
 * a laptop's thermal drift over a run series would otherwise be attributed entirely to
 * whichever variant ran second.
 *
 * <p>Keys are {@code "ModuleName"} to toggle a module, or {@code "ModuleName.SettingName"} for an
 * individual setting. Names match the strings passed to the {@code Module} and {@code Setting}
 * constructors, case-insensitively.
 */
public final class BenchOverrides {

    private BenchOverrides() {
    }

    /** Applies every override, failing the run if any key does not resolve. */
    public static void apply(JsonObject overrides) {
        if (overrides == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
            applyOne(entry.getKey(), entry.getValue());
        }
    }

    /** Sets one module or setting by the same key syntax {@link #apply} uses. */
    public static void set(String key, boolean value) {
        applyOne(key, new com.google.gson.JsonPrimitive(value));
    }

    private static void applyOne(String key, JsonElement value) {
        int dot = key.indexOf('.');
        String moduleName = dot < 0 ? key : key.substring(0, dot);
        Module module = findModule(moduleName);
        if (module == null) {
            // A silently ignored override would produce an A/B where both sides are identical, which
            // is far worse than an aborted run.
            throw new IllegalArgumentException("no module named '" + moduleName + "' for override '" + key + "'");
        }
        if (dot < 0) {
            module.set(value.getAsBoolean());
            ClientLogger.info("benchmark", "override " + key + " = " + value.getAsBoolean());
            return;
        }
        String settingName = key.substring(dot + 1);
        Setting<?> setting = findSetting(module, settingName);
        if (setting == null) {
            throw new IllegalArgumentException("module '" + moduleName + "' has no setting '" + settingName + "'");
        }
        applyValue(setting, value);
        ClientLogger.info("benchmark", "override " + key + " = " + value);
    }

    private static void applyValue(Setting<?> setting, JsonElement value) {
        if (setting instanceof BooleanSetting) {
            ((BooleanSetting) setting).setValue(value.getAsBoolean());
        } else if (setting instanceof NumberSetting) {
            ((NumberSetting) setting).setValue(value.getAsDouble());
        } else {
            throw new IllegalArgumentException("unsupported setting type for override: "
                    + setting.getClass().getSimpleName());
        }
    }

    private static Module findModule(String name) {
        for (Module module : FPSMaster.moduleManager.modules) {
            if (module.name.equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    private static Setting<?> findSetting(Module module, String name) {
        for (Setting<?> setting : module.settings) {
            if (setting.name.equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }
}
