package com.fitoherb.fitoherb_backend_v2.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.fitoherb.fitoherb_backend_v2.utils.validations.ValidationConstants.*;

@Data
@Schema(description = "Request object for creating or updating a banner")
public class BannerReq {
    @Schema(description = "Title of the banner", example = "Promoção de Inverno")
    @Size(min = MIN_STRING_LENGTH, max = MAX_STRING_LENGTH, message = MSG_STRING_SIZE)
    @NotBlank(message = MSG_REQUIRED_FIELD)
    private String title;

    @Schema(description = "Indicates whether the banner is currently active", example = "true")
    private boolean isActive = true;

    @Schema(description = "Ordering position for the banner on the homepage", example = "0")
    private int position = 0;
}
