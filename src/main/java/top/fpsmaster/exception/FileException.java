package top.fpsmaster.exception;

/**
 * Exception thrown when there is an error related to file operations.
 */
public class FileException extends Exception {
    
    /**
     * Constructs a new FileException with the specified detail message.
     * 
     * @param message the detail message
     */
    public FileException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new FileException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
}