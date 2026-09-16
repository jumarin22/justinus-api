package com.justinus.api.exception;

/**
 * For cross-field validation that Bean Validation annotations can't
 * express on their own (e.g. dateFinished before dateStarted).
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
