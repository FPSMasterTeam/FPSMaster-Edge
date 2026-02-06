package top.fpsmaster.exception;

/**
 * Exception thrown when there is an error related to account operations.
 */
public class AccountException extends Exception {
    
    /**
     * Constructs a new AccountException with the specified detail message.
     * 
     * @param message the detail message
     */
    public AccountException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new AccountException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}