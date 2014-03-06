package com.sjsurha.resident_identifier;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author John
 */
public class CEMaxiumAttendeesException extends Exception {
    
    /**
     * Creates a new instance of
     * <code>CEMaxiumAttendeesException</code> without detail message.
     */
    public CEMaxiumAttendeesException() {
        super();
    }

    /**
     * Constructs an instance of
     * <code>CEMaxiumAttendeesException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public CEMaxiumAttendeesException(String msg) {
        super(msg);
    }
    
    /**
     *
     * @param message
     * @param throwable
     */
    public CEMaxiumAttendeesException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
