package com.fitoherb.fitoherb_backend_v2.exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public class ResourceAlreadyExistsException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public ResourceAlreadyExistsException(String message) {
        super(message);
        this.code = null;
        this.details = null;
    }

    public ResourceAlreadyExistsException(String resourceName, String fieldName, String value) {
        super(String.format("%s com %s '%s' já existe", resourceName, fieldName, value));
        this.code = null;
        this.details = null;
    }

    public ResourceAlreadyExistsException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
