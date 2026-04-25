package com.fitoherb.fitoherb_backend_v2.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("E-mail ou senha incorretos.");
    }
    public InvalidCredentialsException(String message) {
        super(message);
    }}
