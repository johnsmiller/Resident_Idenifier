package com.sjsurha.resident_identifier;

/**
 *
 * @author John
 */
public class CEAuthenticationFailedException extends Exception {

    /**
     * Creates a new instance of
     * <code>CEAuthenticationFailedException</code> without detail message.
     */
    public CEAuthenticationFailedException() {
    }

    /**
     * Constructs an instance of
     * <code>CEAuthenticationFailedException</code> with the specified detail
     * message.
     *
     * @param msg the detail message.
     */
    public CEAuthenticationFailedException(String msg) {
        super(msg);
    }
    
    /**
     *
     * @param message
     * @param throwable
     */
    public CEAuthenticationFailedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
