package top.fpsmaster.utils;

import top.fpsmaster.modules.logger.ClientLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GitInfo {

    private static final Properties properties = new Properties();

    static {
        try (InputStream inputStream = GitInfo.class.getResourceAsStream("/git.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                ClientLogger.error("Failed to load git.properties file");
            }
        } catch (IOException e) {
            ClientLogger.error("Failed to load git.properties file");
        }
        }

    public static String getCommitId() {
        return properties.getProperty("git.commit.id");
    }

    public static String getCommitIdAbbrev() {
        return properties.getProperty("git.commit.id.abbrev");
    }

    public static String getCommitTime() {
        return properties.getProperty("git.commit.time");
    }

    public static String getBranch() {
        return properties.getProperty("git.branch");
    }
}
