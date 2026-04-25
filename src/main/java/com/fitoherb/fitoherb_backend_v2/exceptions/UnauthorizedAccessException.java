package com.fitoherb.fitoherb_backend_v2.exceptions;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("Você não tem permissão para executar esta ação");
    }
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
