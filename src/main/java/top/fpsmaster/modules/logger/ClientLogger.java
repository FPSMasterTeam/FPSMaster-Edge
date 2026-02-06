package top.fpsmaster.modules.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientLogger {
    private static final Logger logger = LogManager.getLogger("FPSMaster");

    public static void info(String s) {
        logger.info(s);
    }

    public static void error(String s) {
        logger.error(s);
    }

    public static void warn(String s) {
        logger.warn(s);
    }

    public static void debug(String s) {
        logger.debug(s);
    }

    public static void fatal(String s) {
        logger.fatal(s);
    }

    public static void trace(String s) {
        logger.trace(s);
    }

    public static void info(String from, String s) {
        logger.info("{} -> {}", from, s);
    }

    public static void error(String from, String s) {
        logger.error("{} -> {}", from, s);
    }
}



