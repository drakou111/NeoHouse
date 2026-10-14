package com.drakou111.neohouse.exceptions;

public class InterruptActionException extends RuntimeException {
    public InterruptActionException(String message) {
        super(message);
    }

    public InterruptActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
