package com.fitoherb.fitoherb_backend_v2.exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public class UnprocessableEntityException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public UnprocessableEntityException(String message) {
        super(message);
        this.code = null;
        this.details = null;
    }

    public UnprocessableEntityException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public UnprocessableEntityException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
