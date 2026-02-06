package top.fpsmaster.exception;

/**
 * Exception thrown when there is an error related to network operations.
 */
public class NetworkException extends Exception {
    int code;
    /**
     * Constructs a new NetworkException with the specified detail message.
     * 
     * @param message the detail message
     */
    public NetworkException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * Constructs a new NetworkException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public NetworkException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}