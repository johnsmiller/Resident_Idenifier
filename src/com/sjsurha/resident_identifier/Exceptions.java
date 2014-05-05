/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sjsurha.resident_identifier;

/**
 *
 * @author John
 */
public class Exceptions {
    public static class CEAuthenticationFailedException extends Exception {

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

    public static class CEDuplicateAttendeeException extends Exception{

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

    public static class CEEncryptionErrorException extends Exception {

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

    public static class CEMaximumAttendeesException extends Exception {

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

    public static class CENonResidentException extends Exception {

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

    
}
