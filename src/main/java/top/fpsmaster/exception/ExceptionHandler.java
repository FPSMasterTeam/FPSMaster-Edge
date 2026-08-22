package top.fpsmaster.exception;

import top.fpsmaster.modules.logger.ClientLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Centralized exception handler for the FPSMaster application.
 * This class provides methods to handle different types of exceptions in a consistent way.
 */
public class ExceptionHandler {

    /**
     * Handles any exception by logging it and optionally taking additional actions.
     * 
     * @param e The exception to handle
     * @param message Additional context message
     */
    public static void handle(Exception e, String message) {
        ClientLogger.error(message + ": " + e.getMessage());
        logExceptionDetails(e, "General");
    }

    /**
     * Handles any exception by logging it.
     * 
     * @param e The exception to handle
     */
    public static void handle(Exception e) {
        handle(e, "An error occurred");
    }

    public static void handleAccountException(Exception e, String message) {
        handle(e, "Account error: " + message);
    }

    public static void handleFileException(Exception e, String message) {
        handle(e, "File operation error: " + message);
    }

    public static void handleNetworkException(Exception e, String message) {
        handle(e, "Network error: " + message);
    }

    public static void handleModuleException(Exception e, String message) {
        handle(e, "Module error: " + message);
    }

    /**
     * Logs detailed information about an exception in a structured way.
     * 
     * @param e The exception to log
     * @param category The category of the exception for better organization
     */
    private static void logExceptionDetails(Exception e, String category) {
        ClientLogger.error(category + " Exception", "Type: " + e.getClass().getName());

        // Get the cause if available
        Throwable cause = e.getCause();
        if (cause != null) {
            ClientLogger.error(category + " Exception", "Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        for (String line : sw.toString().split("\\r?\\n")) {
            ClientLogger.error(category + " Exception", line);
        }
    }
}



