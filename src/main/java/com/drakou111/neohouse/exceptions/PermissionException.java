package com.drakou111.neohouse.exceptions;

public class PermissionException extends RuntimeException {

    public PermissionException(String message) {
        super("[ERROR] " + message);
    }
}
