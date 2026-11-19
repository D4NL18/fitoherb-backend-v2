package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.MSG_REQUIRED_FIELD;

@Data
@Schema(description = "Request object for creating an admin API key")
public class ApiKeyReq {
    @Schema(description = "Human-readable label identifying who/what uses the key", example = "claude-projeto-x")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres")
    private String name;
}
