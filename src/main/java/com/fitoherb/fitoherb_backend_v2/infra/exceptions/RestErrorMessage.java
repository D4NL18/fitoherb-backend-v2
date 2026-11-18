package com.fitoherb.fitoherb_backend_v2.infra.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard object for API error responses")
public class RestErrorMessage {

    @Schema(description = "HTTP Status Code", example = "BAD_REQUEST")
    private HttpStatus status;

    @Schema(description = "Detailed error message", example = "Description of the error that occurred.")
    private String message;

    @Schema(description = "Optional machine-readable domain error code", example = "ERP_SKU_INACTIVE_CONFIRMATION_REQUIRED")
    private String code;

    @Schema(description = "Optional structured details associated with the error")
    private Map<String, Object> details;

    public RestErrorMessage(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
