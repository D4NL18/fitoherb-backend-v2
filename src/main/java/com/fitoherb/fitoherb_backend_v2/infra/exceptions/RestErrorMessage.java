package com.fitoherb.fitoherb_backend_v2.infra.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Standard object for API error responses")
public class RestErrorMessage {

    @Schema(description = "HTTP Status Code", example = "BAD_REQUEST")
    private HttpStatus status;

    @Schema(description = "Detailed error message", example = "Description of the error that occurred.")
    private String message;
}