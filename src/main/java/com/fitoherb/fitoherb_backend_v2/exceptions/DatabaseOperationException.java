package com.fitoherb.fitoherb_backend_v2.exceptions;

public class DatabaseOperationException extends RuntimeException {
    public DatabaseOperationException(String message) {
        super(message);
    }
}
