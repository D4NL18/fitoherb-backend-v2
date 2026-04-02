package com.fitoherb.fitoherb_backend_v2.exceptions;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super("E-mail already in use");
    }

    public UserAlreadyExistsException(String customMessage) {
        super(customMessage);
    }
}
