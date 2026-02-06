package top.fpsmaster.exception;

import top.fpsmaster.modules.logger.ClientLogger;

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

    /**
     * Handles a specific type of exception related to account operations.
     * 
     * @param e The exception to handle
     * @param message Additional context message
     */
    public static void handleAccountException(Exception e, String message) {
        ClientLogger.error("Account error: " + message + ": " + e.getMessage());
        logExceptionDetails(e, "Account");
    }

    /**
     * Handles a specific type of exception related to file operations.
     * 
     * @param e The exception to handle
     * @param message Additional context message
     */
    public static void handleFileException(Exception e, String message) {
        ClientLogger.error("File operation error: " + message + ": " + e.getMessage());
        logExceptionDetails(e, "File");
    }

    /**
     * Handles a specific type of exception related to network operations.
     * 
     * @param e The exception to handle
     * @param message Additional context message
     */
    public static void handleNetworkException(Exception e, String message) {
        ClientLogger.error("Network error: " + message + ": " + e.getMessage());
        logExceptionDetails(e, "Network");
    }

    /**
     * Handles a specific type of exception related to module operations.
     * 
     * @param e The exception to handle
     * @param message Additional context message
     */
    public static void handleModuleException(Exception e, String message) {
        ClientLogger.error("Module error: " + message + ": " + e.getMessage());
        logExceptionDetails(e, "Module");
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
        e.printStackTrace();

//         Log stack trace in a structured way
//        StringWriter sw = new StringWriter();
//        PrintWriter pw = new PrintWriter(sw);
//        e.printStackTrace(pw);
//
//        // Split the stack trace into lines and log each line
//        String[] stackTraceLines = sw.toString().split("\\r?\\n");
//        for (String line : stackTraceLines) {
//            ClientLogger.debug(line);
//        }
    }
}



