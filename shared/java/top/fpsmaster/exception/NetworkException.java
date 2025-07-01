package top.fpsmaster.exception;

/**
 * Exception thrown when there is an error related to network operations.
 */
public class NetworkException extends Exception {
    
    /**
     * Constructs a new NetworkException with the specified detail message.
     * 
     * @param message the detail message
     */
    public NetworkException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new NetworkException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}