package com.sjsurha.resident_identifier;

/**
 *
 * @author John
 */
public class CENonResidentException extends Exception {
    
    /**
     *
     */
    public CENonResidentException() {
        super();
    }

    /**
     *
     * @param message
     */
    public CENonResidentException(String message) {
        super(message);
    }

    /**
     *
     * @param message
     * @param throwable
     */
    public CENonResidentException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
