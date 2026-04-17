package com.fitoherb.fitoherb_backend_v2.exceptions;

public class MailSendingException extends RuntimeException {
    public MailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}