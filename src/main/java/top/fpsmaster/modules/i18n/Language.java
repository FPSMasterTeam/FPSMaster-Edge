package top.fpsmaster.modules.i18n;

import top.fpsmaster.exception.FileException;
import top.fpsmaster.utils.io.FileUtils;

import java.util.HashMap;
import java.util.Map;

public class Language {
    private final Map<String, String> prompts = new HashMap<>();

    public Language() {
        FileUtils.release("en_us");
        FileUtils.release("zh_cn");
    }

    public void save(String language) throws FileException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : prompts.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(System.lineSeparator());
        }
        FileUtils.saveFile(language + ".lang", sb.toString());
    }

    public void read(String language) throws FileException {
        String content = FileUtils.readFile(language + ".lang");
        String[] lines = content.split(System.lineSeparator());
        prompts.clear();
        for (String line : lines) {
            String[] split = line.split("=");
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < split.length; i++) {
            sb.append(split[i]);
        }
            if (split.length == 2) {
                prompts.put(split[0], sb.toString());
            }
        }
    }

    public String get(String key) {
        return prompts.getOrDefault(key, key);
    }

    public void put(String name, String content) {
        prompts.put(name, content);
    }
}



