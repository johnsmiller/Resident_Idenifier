package com.sjsurha.resident_identifier;

/**
 *
 * @author John
 */
public class CEDuplicateAttendeeException extends Exception{
    
    /**
     *
     */
    public CEDuplicateAttendeeException() {
        super();
    }
    
    /**
     *
     * @param message
     */
    public CEDuplicateAttendeeException(String message) {
        super(message);
    }

    /**
     *
     * @param message
     * @param throwable
     */
    public CEDuplicateAttendeeException(String message, Throwable throwable) {
        super(message, throwable);
    }
       
}
