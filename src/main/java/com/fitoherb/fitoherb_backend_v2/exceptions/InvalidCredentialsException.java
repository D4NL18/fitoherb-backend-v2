package com.fitoherb.fitoherb_backend_v2.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("E-mail or password incorrect.");
    }
    public InvalidCredentialsException(String message) {
        super(message);
    }}
