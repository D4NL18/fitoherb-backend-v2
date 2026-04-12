package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
@Schema(description = "Request object for creating or updating a supplier")
public class SupplierReq {

    @Schema(description = "Official business name of the supplier", example = "FitoHerb Matrizes Naturais Ltda")
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;

    @Schema(description = "Indicates if the supplier should be featured or highlighted in the storefront", example = "true")
    @NotNull(message = MSG_REQUIRED_FIELD)
    private Boolean isHighlighted;
}