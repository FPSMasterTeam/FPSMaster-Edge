package top.fpsmaster.exception;

/**
 * Exception thrown when there is an error related to module operations.
 */
public class ModuleException extends Exception {
    
    /**
     * Constructs a new ModuleException with the specified detail message.
     * 
     * @param message the detail message
     */
    public ModuleException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new ModuleException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}