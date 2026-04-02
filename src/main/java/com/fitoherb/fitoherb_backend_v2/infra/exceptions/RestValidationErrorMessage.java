package com.fitoherb.fitoherb_backend_v2.infra.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class RestValidationErrorMessage {
    private HttpStatus status;
    private String message;
    private Map<String, String> errors;
}
