package com.fitoherb.fitoherb_backend_v2.exceptions;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("You do not have permission to execute this action");
    }
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
