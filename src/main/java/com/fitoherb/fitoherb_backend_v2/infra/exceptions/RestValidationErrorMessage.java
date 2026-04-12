package com.fitoherb.fitoherb_backend_v2.infra.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Standard object for field-level validation error responses")
public class RestValidationErrorMessage {

    @Schema(description = "HTTP Status Code", example = "BAD_REQUEST")
    private HttpStatus status;

    @Schema(description = "General error category message", example = "Validation failed for one or more fields")
    private String message;

    @Schema(description = "Map containing field names and validation failure reasons",
            example = "{\"field\": \"error message\"}")
    private Map<String, String> errors;
}