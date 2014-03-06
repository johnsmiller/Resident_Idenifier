package com.sjsurha.resident_identifier;

/**
 *
 * @author John
 */
public class CEEncryptionErrorException extends Exception {

    /**
     * Creates a new instance of
     * <code>CEEncryptionErrorException</code> without detail message.
     */
    public CEEncryptionErrorException() {
    }

    /**
     * Constructs an instance of
     * <code>CEEncryptionErrorException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public CEEncryptionErrorException(String msg) {
        super(msg);
    }
    
    /**
     *
     * @param message
     * @param throwable
     */
    public CEEncryptionErrorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
