package top.fpsmaster.modules.config;

import java.util.HashMap;

public class Configure {

    public HashMap<String, String> configures = new HashMap<>();

    public String getOrCreate(String name, String defaultValue) {
        if (configures.containsKey(name)) {
            return configures.getOrDefault(name, defaultValue);
        } else {
            configures.put(name, defaultValue);
            return defaultValue;
        }
    }

    public void set(String name, String value) {
        configures.put(name, value);
    }
}



