package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Getter
@Setter
@Schema(description = "Request object for creating or updating a product")
public class ProductReq {

    @Schema(description = "Full name of the product", example = "Chá de Camomila Orgânico")
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String name;

    @Schema(description = "Detailed description of the product and its benefits", example = "Flores de camomila desidratadas, ideais para infusões relaxantes antes de dormir.")
    @Size(min = MIN_STRING_LENGTH, max = MAX_TEXT_LENGTH, message = MSG_TEXT_SIZE)
    private String description;

    @Schema(description = "The unique SEO slug of the associated category", example = "chas-e-infusoes")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String categorySlug;

    @Schema(description = "The unique SEO slug of the associated supplier", example = "fitoherb-natural-ltda")
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String supplierSlug;

    @Schema(description = "Flavours available for that product", example = "[\"Chocolate\", \"Morango\", \"Baunilha\"]")
    @Size(max = MAX_ARRAY_SIZE, message = MSG_ARRAY_SIZE)
    private List<String> flavours;

    @Schema(description = "Presentations available for that product", example = "[\"100g\", \"250g\", \"500g\"]")
    @Size(max = MAX_ARRAY_SIZE, message = MSG_ARRAY_SIZE)
    private List<String> presentation;
}