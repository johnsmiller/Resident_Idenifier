package com.sjsurha.resident_identifier;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author John
 */
public class CEMaximumAttendeesException extends Exception {
    
    /**
     * Creates a new instance of
     * <code>CEMaximumAttendeesException</code> without detail message.
     */
    public CEMaximumAttendeesException() {
        super();
    }

    /**
     * Constructs an instance of
     * <code>CEMaximumAttendeesException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public CEMaximumAttendeesException(String msg) {
        super(msg);
    }
    
    /**
     *
     * @param message
     * @param throwable
     */
    public CEMaximumAttendeesException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
