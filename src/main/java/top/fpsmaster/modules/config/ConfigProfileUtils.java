package top.fpsmaster.modules.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.shortcut.Shortcut;
import top.fpsmaster.utils.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ConfigProfileUtils {
    public static final String CURRENT_CONFIG = "default";
    public static final String ALL_OFF_PRESET = "all_off";
    private static final String PROFILE_DIR = "config";
    private static final String JSON_SUFFIX = ".json";
    private static final String ACTIVE_PROFILE_STATE = "active_profile.txt";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String activeProfileName = CURRENT_CONFIG;

    private ConfigProfileUtils() {
    }

    public static File getCurrentConfigFile() {
        return new File(getProfileDir(), CURRENT_CONFIG + JSON_SUFFIX);
    }

    public static File getProfileDir() {
        File dir = new File(FileUtils.dir, PROFILE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            ClientLogger.error("Failed to create config profile directory: " + dir.getAbsolutePath());
        }
        return dir;
    }

    public static File getProfileFile(String name) throws FileException {
        return new File(getProfileDir(), sanitizeProfileName(name) + JSON_SUFFIX);
    }

    public static String getCurrentConfigPath() {
        return getCurrentConfigFile().getAbsolutePath();
    }

    public static String getActiveProfileName() {
        return activeProfileName;
    }

    public static String loadActiveProfileName() {
        File stateFile = getActiveProfileStateFile();
        if (!stateFile.exists()) {
            activeProfileName = CURRENT_CONFIG;
            return activeProfileName;
        }
        try {
            String profileName = sanitizeProfileName(readString(stateFile).trim());
            if (!getProfileFile(profileName).exists()) {
                activeProfileName = CURRENT_CONFIG;
                saveActiveProfileStateQuietly();
                return activeProfileName;
            }
            activeProfileName = profileName;
        } catch (FileException exception) {
            activeProfileName = CURRENT_CONFIG;
            saveActiveProfileStateQuietly();
        }
        return activeProfileName;
    }

    public static void saveActiveProfileStateQuietly() {
        try {
            writeString(getActiveProfileStateFile(), activeProfileName);
        } catch (FileException exception) {
            ClientLogger.warn("Failed to save active config profile state: " + activeProfileName);
        }
    }

    public static File getActiveProfileStateFile() {
        return new File(getProfileDir(), ACTIVE_PROFILE_STATE);
    }

    public static void setActiveProfileName(String profileName) {
        try {
            activeProfileName = sanitizeProfileName(profileName);
        } catch (FileException exception) {
            activeProfileName = CURRENT_CONFIG;
        }
        saveActiveProfileStateQuietly();
    }

    public static void saveActiveProfileQuietly() {
        try {
            FPSMaster.configManager.saveConfig(activeProfileName);
        } catch (FileException exception) {
            ClientLogger.warn("Failed to save active config profile: " + activeProfileName);
        }
    }

    public static void migrateLegacyDefaultIfNeeded() throws FileException {
        File legacy = new File(FileUtils.dir, CURRENT_CONFIG + JSON_SUFFIX);
        File target = getCurrentConfigFile();
        if (!legacy.exists() || target.exists()) {
            return;
        }

        ensureParent(target);
        try {
            if (!legacy.renameTo(target)) {
                Files.copy(legacy.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (!legacy.delete()) {
                    ClientLogger.warn("Legacy default config copied but not deleted: " + legacy.getAbsolutePath());
                }
            }
            ClientLogger.info("Migrated legacy config: " + legacy.getAbsolutePath() + " -> " + target.getAbsolutePath());
        } catch (IOException exception) {
            throw new FileException("Failed to migrate legacy default config", exception);
        }
    }

    public static String readConfigFile(String name) throws FileException {
        if (CURRENT_CONFIG.equals(sanitizeProfileName(name))) {
            migrateLegacyDefaultIfNeeded();
        }
        File file = getProfileFile(name);
        ensureFile(file);
        return readString(file);
    }

    public static void saveConfigFile(String name, String content) throws FileException {
        File file = getProfileFile(name);
        writeString(file, content);
    }

    public static String importProfile(File source) throws FileException {
        if (source == null || !source.exists() || !source.isFile()) {
            throw new FileException("Config profile source not found");
        }

        String fileName = source.getName();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(JSON_SUFFIX)) {
            fileName = fileName.substring(0, fileName.length() - JSON_SUFFIX.length());
        }
        String profileName = sanitizeProfileName(fileName);
        File target = getProfileFile(profileName);
        ensureParent(target);
        try {
            if (!source.getCanonicalFile().equals(target.getCanonicalFile())) {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            writeProfileMeta(profileName, "");
        } catch (IOException exception) {
            throw new FileException("Failed to import config profile: " + source.getAbsolutePath(), exception);
        }
        ClientLogger.info("Imported config profile: " + source.getAbsolutePath() + " -> " + target.getAbsolutePath());
        return profileName;
    }

    public static void exportActiveProfile(File target) throws FileException {
        if (target == null) {
            throw new FileException("Config profile export target is empty");
        }

        String profileName = getActiveProfileName();
        FPSMaster.configManager.saveConfig(profileName);
        File source = getProfileFile(profileName);
        ensureParent(target);
        try {
            if (!source.getCanonicalFile().equals(target.getCanonicalFile())) {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new FileException("Failed to export config profile: " + target.getAbsolutePath(), exception);
        }
        ClientLogger.info("Exported config profile: " + source.getAbsolutePath() + " -> " + target.getAbsolutePath());
    }

    public static void deleteConfigFile(String name) throws FileException {
        File file = getProfileFile(name);
        if (file.exists() && !file.delete()) {
            throw new FileException("Failed to delete file: " + file.getAbsolutePath());
        }
    }

    public static List<ConfigProfile> listConfigs() {
        List<ConfigProfile> profiles = new ArrayList<>();
        if (FileUtils.dir == null || !FileUtils.dir.exists()) {
            return profiles;
        }

        try {
            migrateLegacyDefaultIfNeeded();
        } catch (FileException exception) {
            ClientLogger.warn("Failed to migrate legacy default config before listing profiles");
        }

        File[] files = getProfileDir().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(JSON_SUFFIX));
        if (files == null) {
            return profiles;
        }

        for (File file : files) {
            String fileName = file.getName();
            String profileName = fileName.substring(0, fileName.length() - JSON_SUFFIX.length());
            profiles.add(new ConfigProfile(profileName, "", file, activeProfileName.equals(profileName)));
        }

        Collections.sort(profiles, Comparator
                .comparing(ConfigProfile::isCurrent).reversed()
                .thenComparing(ConfigProfile::getName, String.CASE_INSENSITIVE_ORDER));
        return profiles;
    }

    public static String saveCurrentAs(String name) throws FileException {
        return saveCurrentAs(name, "");
    }

    public static String saveCurrentAs(String name, String author) throws FileException {
        String profileName = sanitizeProfileName(name);
        FPSMaster.configManager.saveConfig(profileName);
        writeProfileMeta(profileName, author);
        ClientLogger.info("Saved config profile: " + profileName);
        return profileName;
    }

    public static void loadProfile(String name) throws Exception {
        loadProfile(name, true);
    }

    public static void loadProfileWithoutSavingCurrent(String name) throws Exception {
        loadProfile(name, false);
    }

    private static void loadProfile(String name, boolean saveCurrent) throws Exception {
        String profileName = sanitizeProfileName(name);
        String currentProfileName = activeProfileName;
        if (saveCurrent && !currentProfileName.equals(profileName) && getProfileFile(currentProfileName).exists()) {
            FPSMaster.configManager.saveConfig(currentProfileName);
        }
        FPSMaster.configManager.loadConfig(profileName);
        activeProfileName = profileName;
        saveActiveProfileStateQuietly();
        ClientLogger.info("Loaded config profile: " + profileName);
    }

    public static void resetActiveProfileToAllOff() throws FileException {
        String profileName = activeProfileName;
        FPSMaster.configManager.resetProfileToAllOff(profileName);
        ClientLogger.info("Reset config profile: " + profileName);
    }

    public static String renameProfile(String oldName, String newName, String author) throws FileException {
        String sourceName = sanitizeProfileName(oldName);
        String targetName = sanitizeProfileName(newName);
        if (CURRENT_CONFIG.equals(sourceName)) {
            throw new FileException("Default config cannot be renamed");
        }

        File source = getProfileFile(sourceName);
        File target = getProfileFile(targetName);
        if (!source.exists()) {
            throw new FileException("Config profile not found: " + sourceName);
        }
        if (!sourceName.equals(targetName) && target.exists()) {
            throw new FileException("Config profile already exists: " + targetName);
        }

        if (!sourceName.equals(targetName)) {
            ensureParent(target);
            if (!source.renameTo(target)) {
                try {
                    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    if (!source.delete()) {
                        ClientLogger.warn("Renamed config copied but old file was not deleted: " + source.getAbsolutePath());
                    }
                } catch (IOException exception) {
                    throw new FileException("Failed to rename config profile: " + sourceName + " -> " + targetName, exception);
                }
            }
        }

        writeProfileMeta(targetName, author);
        ClientLogger.info("Renamed config profile: " + sourceName + " -> " + targetName);
        return targetName;
    }

    public static void deleteProfile(String name) throws FileException {
        String profileName = sanitizeProfileName(name);
        if (CURRENT_CONFIG.equals(profileName)) {
            throw new FileException("Default config cannot be deleted");
        }
        if (countProfiles() <= 1) {
            throw new FileException("At least one config profile must remain");
        }
        deleteConfigFile(profileName);
        ClientLogger.info("Deleted config profile: " + profileName);
    }

    private static int countProfiles() {
        File[] files = getProfileDir().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(JSON_SUFFIX));
        return files == null ? 0 : files.length;
    }

    public static String sanitizeProfileName(String name) throws FileException {
        String rawName = name == null ? "" : name.trim();
        if (rawName.toLowerCase(Locale.ROOT).endsWith(JSON_SUFFIX)) {
            rawName = rawName.substring(0, rawName.length() - JSON_SUFFIX.length());
        }

        String safeName = FileUtils.fixName(rawName).trim();
        if (safeName.isEmpty()) {
            throw new FileException("Config profile name is empty");
        }
        return safeName;
    }

    public static String sanitizeAuthor(String author) {
        if (author == null) {
            return "";
        }
        return author.trim();
    }

    private static String readProfileAuthor(File file) {
        try {
            JsonObject json = GSON.fromJson(readString(file), JsonObject.class);
            if (json == null || !json.has("profile") || !json.get("profile").isJsonObject()) {
                return "";
            }
            JsonObject profile = json.getAsJsonObject("profile");
            if (!profile.has("author") || profile.get("author").isJsonNull()) {
                return "";
            }
            return sanitizeAuthor(profile.get("author").getAsString());
        } catch (Exception exception) {
            ClientLogger.warn("Failed to read config profile author: " + file.getName());
            return "";
        }
    }

    private static void writeProfileMeta(String profileName, String author) throws FileException {
        String safeAuthor = sanitizeAuthor(author);
        File file = getProfileFile(profileName);
        JsonObject json = GSON.fromJson(readString(file), JsonObject.class);
        if (json == null) {
            throw new FileException("Failed to write config metadata: " + file.getAbsolutePath());
        }

        JsonObject profile = json.has("profile") && json.get("profile").isJsonObject()
                ? json.getAsJsonObject("profile")
                : new JsonObject();
        profile.addProperty("name", profileName);
        if (safeAuthor.isEmpty()) {
            profile.remove("author");
        } else {
            profile.addProperty("author", safeAuthor);
        }
        json.add("profile", profile);
        writeString(file, GSON.toJson(json));
    }

    private static void ensureFile(File file) throws FileException {
        ensureParent(file);
        if (file.exists()) {
            return;
        }
        try {
            if (!file.createNewFile()) {
                throw new FileException("Failed to create file: " + file.getAbsolutePath());
            }
        } catch (IOException exception) {
            throw new FileException("Failed to create file: " + file.getAbsolutePath(), exception);
        }
    }

    private static void ensureParent(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            ClientLogger.error("Failed to create config directory: " + parent.getAbsolutePath());
        }
    }

    private static String readString(File file) throws FileException {
        StringBuilder result = new StringBuilder();
        try (FileInputStream inputStream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        } catch (IOException exception) {
            throw new FileException("Failed to read config file: " + file.getAbsolutePath(), exception);
        }
        return result.toString();
    }

    private static void writeString(File file, String content) throws FileException {
        ensureParent(file);
        try (FileOutputStream outputStream = new FileOutputStream(file);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.write(content);
            writer.flush();
        } catch (IOException exception) {
            throw new FileException("Failed to save config file: " + file.getAbsolutePath(), exception);
        }
    }

    public static final class ConfigProfile {
        private final String name;
        private final String author;
        private final File file;
        private final boolean current;

        private ConfigProfile(String name, String author, File file, boolean current) {
            this.name = name;
            this.author = author;
            this.file = file;
            this.current = current;
        }

        public String getName() {
            return name;
        }

        public String getAuthor() {
            return author;
        }

        public File getFile() {
            return file;
        }

        public boolean isCurrent() {
            return current;
        }
    }
}
