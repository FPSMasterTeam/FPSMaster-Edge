package top.fpsmaster.utils.io;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import net.minecraft.client.Minecraft;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {

    public static File fpsmasterCache;
    public static File netease;
    public static File dir;
    public static File cache;
    public static File music;
    public static File artists;
    public static File omaments;
    public static File background;
    public static boolean hasBackground = false;
    public static File fonts;

    static {
        if (net.minecraft.client.Minecraft.getMinecraft() == null) {
            ClientLogger.error("FileUtils", "Minecraft provider is not initialized");
            cache = file(new File("D:\\Code\\FPSMaster"), ".cache");
            dir = file(new File("D:\\Code\\FPSMaster"), "FPSMaster " + FPSMaster.EDITION);
        } else {
            cache = file(Minecraft.getMinecraft().mcDataDir, ".cache");
            dir = file(Minecraft.getMinecraft().mcDataDir, "FPSMaster " + FPSMaster.EDITION);
        }
        fonts = file(dir, "fonts");

        fpsmasterCache = file(cache, "FPSMasterClient");
        netease = file(cache, "netease");
        music = file(netease, "songs");
        artists = file(netease, "artists");
        omaments = file(cache, "omaments");
        background = new File(dir, "background.png");
        if (background.exists()) {
            hasBackground = true;
        }
    }

    public static File file(File parent, String child) {
        File file = new File(parent, child);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static void saveFileBytes(String s, byte[] bytes) throws FileException {
        File file = new File(dir, s);
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new FileException("Failed to create file: " + s);
                }
            }
            try (FileOutputStream fOut = new FileOutputStream(file)) {
                fOut.write(bytes);
                fOut.flush();
            }
        } catch (IOException e) {
            throw new FileException("Failed to save file bytes: " + s, e);
        }
    }

    public static void saveFile(String name, String content) throws FileException {
        File file = new File(dir, name);
        saveAbsoluteFile(file.getAbsolutePath(), content);
    }

    private static void saveAbsoluteFile(String name, String content) throws FileException {
        File file = new File(name);
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    ClientLogger.error("FileUtils", "failed to create " + name);
                    throw new FileException("Failed to create file: " + name);
                }
            }
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                bw.write(content);
                bw.flush();
            }
        } catch (IOException e) {
            throw new FileException("Failed to save file: " + name, e);
        }
    }

    public static void saveTempValue(String name, String value) throws FileException {
        File dir = new File(fpsmasterCache, name + ".tmp");
        saveAbsoluteFile(dir.getAbsolutePath(), value);
    }

    public static String readTempValue(String name) throws FileException {
        File dir = new File(fpsmasterCache, name + ".tmp");
        if (!dir.exists()) {
            return "";
        }
        try {
            return readAbsoluteFile(dir.getAbsolutePath());
        } catch (Exception e) {
            throw new FileException("Failed to read temp value: " + name, e);
        }
    }

    public static void release(String file) {
        File f = new File(dir, "file.lang").getAbsoluteFile();
        ClientLogger.info("release " + file);
        try {
            f.createNewFile();
            try (InputStream resourceAsStream = FileUtils.class.getResourceAsStream("/assets/minecraft/client/lang/" + file + ".lang")) {
                if (resourceAsStream == null) {
                    ClientLogger.error("An error occurred while loading language file: " + file + ".lang");
                    return;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    try {
                        saveFile(file + ".lang", sb.toString());
                    } catch (FileException e) {
                        ClientLogger.error("Failed to save language file: " + file + ".lang");
                        throw new IOException("Failed to save language file", e);
                    }
                }
            }
        } catch (IOException e) {
            ClientLogger.error("An error occurred while releasing language file: " + file + ".lang");
            e.printStackTrace();
        }
    }

    public static int getDirSize(File folder) {
        long size = 0;
        File[] fileList = folder.listFiles();
        if (fileList == null) return 0;
        for (File file : fileList) {
            size += file.isDirectory() ? getDirSize(file) : file.length();
        }
        return (int) (size / 1024 / 1024);
    }

    public static String readFile(String name) throws FileException {
        File file = new File(dir, name);
        return readAbsoluteFile(file.getAbsolutePath());
    }

    public static String readAbsoluteFile(String name) throws FileException {
        File file = new File(name);
        StringBuilder result = new StringBuilder();
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new FileException("Failed to create file: " + name);
                }
            }
            try (FileInputStream fIn = new FileInputStream(file);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fIn, StandardCharsets.UTF_8))) {
                    String str;
                    while ((str = bufferedReader.readLine()) != null) {
                        result.append(str).append(System.lineSeparator());
                    }
                }
        } catch (IOException e) {
            throw new FileException("Failed to read file: " + name, e);
        }
        return result.toString();
    }

    public static String fixName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}



