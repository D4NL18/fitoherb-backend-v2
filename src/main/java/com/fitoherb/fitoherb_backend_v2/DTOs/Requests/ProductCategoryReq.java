package com.fitoherb.fitoherb_backend_v2.DTOs.Requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
@Schema(description = "Request object for creating or updating a product category")
public class ProductCategoryReq {

    @Schema(description = "Unique name of the category", example = "Chás e Infusões")
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;
}